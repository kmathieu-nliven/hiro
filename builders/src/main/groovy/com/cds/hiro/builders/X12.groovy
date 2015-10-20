package com.cds.hiro.builders

import com.cds.hiro.x12.EdiParser
import com.cds.hiro.x12_837p.composites.HealthCareCodeInformation
import com.cds.hiro.x12_837p.enums.*
import com.cds.hiro.x12_837p.loops.*
import com.cds.hiro.x12_837p.segments.*
import com.cds.hiro.x12_837p.transactionsets.M837Q1

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Builder for X12.837 Documents
 *
 * @author Rahul Somasunderam
 */
class X12 {
  static M837Q1 create(@DelegatesTo(CdaContext) Closure closure) {
    create(new CdaContext(), closure)
  }

  static M837Q1 create(CdaContext defaults, @DelegatesTo(CdaContext) Closure closure) {
    def delegate = defaults.clone() as CdaContext
    closure.delegate = delegate
    closure.call()
    createX12(delegate)
  }

  static M837Q1 createX12(CdaContext context) {
    def x12 = new M837Q1()
    configureHeaders(x12, context)
    configureAuthor(x12, context.author)
    configureInsured(x12, context.patient)
    configurePayer(x12, context.payers?.first())
    configureServiceEventAndEncounter(x12, context)

    configureProblems(x12, context.problems)

    x12
  }

  private static void configureProblems(x12, ArrayList<CdaContext.Problem> problems) {
    problems.each {
      def startProblem = it.between ?
          dtp(DateTimeQualifier.OnsetofCurrentSymptomsorIllness_431, it.between) :
          it.since ?
              dtp(DateTimeQualifier.OnsetofCurrentSymptomsorIllness_431, it.since) :
              null
      def endProblem = it.and ? dtp(DateTimeQualifier.OccurrenceSpanTo_450, it.and) : null

      x12.withL2000c(new L2000C().
          withL2300_8(new L2300().
              withDtp_2(startProblem).
              withDtp_3(endProblem).
              withHi_79(new HI().
                  withHealthCareCodeInformation_01(new HealthCareCodeInformation().
                      withCodeListQualifierCode_01(
                          CodeListQualifierCode.InternationalClassificationofDiseasesClinicalModificationICD9CMAdmittingDiagnosis_BJ
                          /* TODO This assumes ICD9CM. May be wrong */
                      ).
                      withIndustryCode_02(it.code.code)
                  )
              )/*.
              withL2400_94(new L2400().
                  withSv1_2(new SV1().with01)
              )*/
          )
      )
    }
  }

  private static void configureServiceEventAndEncounter(M837Q1 x12, CdaContext context) {
    x12.withL2000c(new L2000C().
        withPat_2(createPatient(context)).
        withL2300_8(new L2300().
            withL2310b_86(createServiceEvent(context.serviceEvent)).
            withDtp_2(dtp(DateTimeQualifier.InitialTreatment_454, context.encounter.between)).
            withDtp_3(dtp(DateTimeQualifier.LastVisit_691, context.encounter.and))
        )
    )
  }

  private static CdaContext.VitalsGroup.VitalSign computeWeight(CdaContext context) {
    context.vitalsGroups.
        sort { it.on }.
        collectMany { it.vitalSigns }.
        find { CdaContext.VitalsGroup.VitalSign sign ->
          sign.code.displayName.equalsIgnoreCase('Weight')
        } as CdaContext.VitalsGroup.VitalSign
  }

  private static PAT createPatient(CdaContext context) {
    CdaContext.VitalsGroup.VitalSign weight = computeWeight(context)
    new PAT().
        withIndividualRelationshipCode_01(IndividualRelationshipCode.Self_18).
        withPatientLocationCode_02(PatientLocationCode.OutpatientFacility_O).
        withEmploymentStatusCode_03(EmploymentStatusCode.EmployedbyOutsideOrganization_EO).
        withDateTimePeriodFormatQualifier_05(DateTimePeriodFormatQualifier.DateandTimeExpressedinFormatCCYYMMDDHHMM_DT).
        withDateTimePeriod_06(LocalDateTime.ofInstant(context.created, ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern('yyyyMMddHHmm'))).
        withWeight_08(weight ? Double.valueOf(weight.at) : null).
        withYesNoConditionorResponseCode_09(YesNoConditionorResponseCode.No_N)
  }

  private static DTP dtp(DateTimeQualifier dateTimeQualifier, String and) {
    new DTP().
        withDateTimeQualifier_01(dateTimeQualifier).
        withDateTimePeriodFormatQualifier_02(DateTimePeriodFormatQualifier.DateExpressedinFormatCCYYMMDD_D8).
        withDateTimePeriod_03(and)
  }

  private static L2310B createServiceEvent(CdaContext.ServiceEvent serviceEvent) {
    new L2310B().
        withNm1_1(new NM1().
            withEntityIdentifierCode_01(EntityIdentifierCode.Provider_1P).
            withEntityTypeQualifier_02(EntityTypeQualifier.Person_1).
            withNameLastorOrganizationName_03(serviceEvent.family).
            withNameFirst_04(serviceEvent.given).
            withIdentificationCodeQualifier_08(IdentificationCodeQualifier.EmployeeIdentificationNumber_EI).
            withIdentificationCode_09(serviceEvent.extension)
        )
  }

  private static void configurePayer(M837Q1 x12, CdaContext.Payer payer) {
    x12.withL1000b(new L1000B().
        withNm1_1(new NM1().
            withEntityIdentifierCode_01(EntityIdentifierCode.Receiver_40).
            withEntityTypeQualifier_02(EntityTypeQualifier.NonPersonEntity_2).
            withNameLastorOrganizationName_03(payer.name).
            withIdentificationCodeQualifier_08(IdentificationCodeQualifier.ElectronicTransmitterIdentificationNumberETIN_46).
            withIdentificationCode_09(payer.identifiedAs)
        )
    )
  }

  private static void configureAuthor(M837Q1 x12, CdaContext.Author author) {
    x12.withL1000a(new L1000A().
        withNm1_1(new NM1().
            withEntityIdentifierCode_01(EntityIdentifierCode.Submitter_41).
            withEntityTypeQualifier_02(EntityTypeQualifier.NonPersonEntity_2).
            withNameLastorOrganizationName_03(author.at).
            withIdentificationCodeQualifier_08(IdentificationCodeQualifier.ElectronicTransmitterIdentificationNumberETIN_46).
            withIdentificationCode_09(author.identifiedAs)
        ).
        withPer_2(new PER().
            withContactFunctionCode_01(ContactFunctionCode.InformationContact_IC).
            withName_02("${author.given} ${author.family}").
            withCommunicationNumberQualifier_03(CommunicationNumberQualifier.Telephone_TE).
            withCommunicationNumber_04(author.phone)
        )
    )
  }

  private static void configureInsured(M837Q1 x12, CdaContext.Patient patient) {
    if (patient) {
      x12.withL2000a(new L2000A().
          withHl_1(new HL()).
          withL2010aa_5(new L2010AA().
              withNm1_1(new NM1().
                  withEntityIdentifierCode_01(EntityIdentifierCode.InsuredorSubscriber_IL).
                  withEntityTypeQualifier_02(EntityTypeQualifier.Person_1).
                  withNameLastorOrganizationName_03(patient.family).
                  withNameFirst_04(patient.given).
                  withIdentificationCodeQualifier_08(IdentificationCodeQualifier.MemberIdentificationNumber_MI).
                  withIdentificationCode_09(patient.extension)
              ).
              withN3_3(new N3().
                  withAddressInformation_01(patient.addr.street)
              ).
              withN4_4(new N4().
                  withCityName_01(patient.addr.city).
                  withStateorProvinceCode_02(patient.addr.state).
                  withPostalCode_03(patient.addr.postalCode).
                  withCountryCode_04(patient.addr.country).
                  withLocationQualifier_05(LocationQualifier.HomeAddress_H)
              )
          )
      )
    }
  }

  private static void configureHeaders(M837Q1 x12, CdaContext context) {
    def localDateTime = LocalDateTime.ofInstant(context.created, ZoneId.systemDefault())
    x12.
        withSt(new ST().
            withTransactionSetIdentifierCode_01(TransactionSetIdentifierCode.HealthCareClaim_837).
            withTransactionSetControlNumber_02(context.id).
            withImplementationConventionReference_03('005010X222A1')
        ).
        withBht(new BHT().
            withHierarchicalStructureCode_01(HierarchicalStructureCode.InformationSourceSubscriberDependent_0019).
            withTransactionSetPurposeCode_02(TransactionSetPurposeCode.Original_00).
            withReferenceIdentification_03(context.id).
            withDate_04(localDateTime.toLocalDate()).
            withTime_05(localDateTime.toLocalTime()).
            withTransactionTypeCode_06(TransactionTypeCode.Chargeable_CH)
        ).
        withRef(new REF().
            withReferenceIdentificationQualifier_01(ReferenceIdentificationQualifier.ClaimNumber_D9).
            withReferenceIdentification_02(context.id).
            withDescription_03('Claim')
        )
  }

  static String serialize(M837Q1 document, boolean prettyPrint = false) {
    def tokens = document.toTokens(prettyPrint ? 0 : -1)
    new EdiParser().toEdi(tokens)
  }
}
