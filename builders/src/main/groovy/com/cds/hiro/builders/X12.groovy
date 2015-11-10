package com.cds.hiro.builders

import com.cds.hiro.builders.contexts.*
import com.cds.hiro.x12.EdiParser
import com.cds.hiro.x12.batch.Interchange
import com.cds.hiro.x12.structures.Message
import com.cds.hiro.x12_837p.composites.*
import com.cds.hiro.x12_837p.enums.*
import com.cds.hiro.x12_837p.loops.*
import com.cds.hiro.x12_837p.segments.*
import com.cds.hiro.x12_837p.transactionsets.*

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Builder for X12.837 Documents
 *
 * @author Rahul Somasunderam
 */
class X12 {
  static M837Q1 create(@DelegatesTo(X12Context) Closure closure) {
    create(new X12Context(), closure)
  }

  static M837Q1 create(X12Context defaults, @DelegatesTo(X12Context) Closure closure) {
    def delegate = defaults.clone() as X12Context
    closure.delegate = delegate
    closure.call()
    createX12(delegate)
  }

  static M837Q1 createX12(X12Context context) {
    def x12 = new M837Q1()
    configureHeaders(x12, context)
    configureAuthor(x12, context.author)
    configureInsured(x12, context.patient)
    if (context.payers) {
      configurePayer(x12, context.payers?.first())
    }
    if (context.serviceEvent) {
      configureServiceEventAndEncounter(x12, context, context.facility)
    }
    configureProblems(x12, context.problems, context.facility)
    configureDiagnoses(x12, context.diagnoses, context.facility)
    configureProcedures(x12, context.procedures, context.facility)
    configureFooter(x12, context)

    x12
  }

  private static void configureFooter(M837Q1 x12, X12Context context) {
    def segCount = x12.toTokens(-1).size() + 1

    x12.withSe(new SE().
        withNumberofIncludedSegments_01(segCount).
        withTransactionSetControlNumber_02(context.id)
    )
  }

  private static Map<String, ProductServiceIDQualifier> productServiceIDQualifierMap = [
      '2.16.840.1.113883.5.25': ProductServiceIDQualifier.HealthCareFinancingAdministrationCommonProceduralCodingSystemHCPCSCodes_HC,
      '2.16.840.1.113883.6.96': ProductServiceIDQualifier.SNOMEDSystematizedNomenclatureofMedicine_LD,
      '2.16.840.1.113883.6.1' : ProductServiceIDQualifier.LogicalObservationIdentifierNamesandCodesLOINCCodes_LB,
  ]

  private static void configureProcedures(x12, ArrayList<Procedure> procedures, Facility facility) {
    int idx = 1
    procedures.each {

      def startProcedure = it.from ?
          dtp(DateTimeQualifier.ServicePeriodStart_150, it.from) : it.on ?
          dtp(DateTimeQualifier.ServicePeriodStart_150, it.on) : null

      def endProcedure = it.to ? dtp(DateTimeQualifier.ServicePeriodEnd_151, it.to) : null

      def codeSystemQualifier = productServiceIDQualifierMap[it.code.codeSystem]
      if (!codeSystemQualifier) {
        throw new Exception("CodeSystem ${it.code.codeSystem} is not mapped in com.cds.hiro.builders.X12.productServiceIDQualifierMap")
      }

      x12.withL2000c(new L2000C().
          //withHl_1(hl('1', HierarchicalLevelCode.Dependent_23)).
          withL2300_8(new L2300().
              withL2400_94(new L2400().
                  withLx_1(new LX().withAssignedNumber_01(idx++)).
                  withSv2_3(new SV2().
                      withCompositeMedicalProcedureIdentifier_02(new CompositeMedicalProcedureIdentifier().
                          withProductServiceIDQualifier_01(codeSystemQualifier).
                          withProductServiceID_02(it.code.code).
                          withDescription_07(it.code.displayName)
                      )
                  ).
                  withDtp_23(startProcedure).
                  withDtp_24(endProcedure)
              ))
      )
    }
  }

  private static Map<String, CodeListQualifierCode> codeListQualifierCodeMap = [
      '2.16.840.1.113883.6.103': CodeListQualifierCode.InternationalClassificationofDiseasesClinicalModificationICD9CMAdmittingDiagnosis_BJ,
      '2.16.840.1.113883.6.96' : CodeListQualifierCode.SNOMEDSystematizedNomenclatureofMedicine_AAA,
  ]

  private static void configureProblems(x12, ArrayList<Problem> problems, Facility facility) {
    problems.each {
      def startProblem = it.between ?
          dtp(DateTimeQualifier.ServicePeriodStart_150, it.between) :
          it.since ?
              dtp(DateTimeQualifier.ServicePeriodStart_150, it.since) :
              null
      def endProblem = it.and ? dtp(DateTimeQualifier.ServicePeriodEnd_151, it.and) : null

      def codeListQualifierCode = codeListQualifierCodeMap[it.code.codeSystem]
      if (!codeListQualifierCode) {
        throw new Exception("CodeSystem ${it.code.codeSystem} is not mapped in com.cds.hiro.builders.X12.codeListQualifierCodeMap")
      }

      x12.withL2000c(new L2000C().
          withL2300_8(new L2300().
              withHi_79(new HI().
                  withHealthCareCodeInformation_01(new HealthCareCodeInformation().
                      withCodeListQualifierCode_01(codeListQualifierCode).
                      withIndustryCode_02(it.code.code)
                  )
              )
          )
      )
    }
  }

  private static void configureDiagnoses(x12, ArrayList<Diagnosis> diagnoses, Facility facility) {
    diagnoses.each {
      def diagnosisDate = it.on ?
          dtp(DateTimeQualifier.ServicePeriodStart_150, it.on) :
          null

      def codeListQualifierCode = codeListQualifierCodeMap[it.code.codeSystem]
      if (!codeListQualifierCode) {
        throw new Exception("CodeSystem ${it.code.codeSystem} is not mapped in com.cds.hiro.builders.X12.codeListQualifierCodeMap")
      }

      x12.withL2000c(new L2000C().
          withL2300_8(new L2300().
              withHi_79(new HI().
                  withHealthCareCodeInformation_01(new HealthCareCodeInformation().
                      withCodeListQualifierCode_01(codeListQualifierCode).
                      withIndustryCode_02(it.code.code)
                  )
              )
          )
      )
    }
  }

  private static void configureServiceEventAndEncounter(M837Q1 x12, X12Context context, Facility facility) {
    x12.withL2000c(new L2000C().
        withHl_1(hl('1', HierarchicalLevelCode.Dependent_23)).
        withPat_2(createPatient(context)).
        withL2300_8(new L2300().
            withClm_1(clm(facility)).
            withL2310b_86(createServiceEvent(context.serviceEvent)).
            withDtp_2(dtp(DateTimeQualifier.Discharge_096, context.encounter.and)).
            withDtp_3(dtp(DateTimeQualifier.Statement_434, context.encounter.between, context.encounter.and)).
            withDtp_4(dtp(DateTimeQualifier.Admission_435, context.encounter.between))
        )
    )
  }

  private static CLM clm(Facility facility) {
    new CLM().
        withClaimSubmittersIdentifier_01(facility ? facility.idx : '99999999').
        withMonetaryAmount_02(20.00).
        withHealthCareServiceLocationInformation_05(new HealthCareServiceLocationInformation().
            withFacilityCodeValue_01(facility ? facility.identifier : '12').
            withFacilityCodeQualifier_02(FacilityCodeQualifier.UniformBillingClaimFormBillType_A).
            withClaimFrequencyTypeCode_03('1')
        ).
        withProviderAcceptAssignmentCode_07(ProviderAcceptAssignmentCode.Assigned_A).
        withYesNoConditionorResponseCode_08(YesNoConditionorResponseCode.Yes_Y).
        withReleaseofInformationCode_09(ReleaseofInformationCode.YesProviderhasaSignedStatementPermittingReleaseofMedicalBillingDataRelatedtoaClaim_Y)
  }

  private static PAT createPatient(X12Context context) {
    new PAT().
        withIndividualRelationshipCode_01(IndividualRelationshipCode.Self_18).
        withPatientLocationCode_02(PatientLocationCode.OutpatientFacility_O).
        withEmploymentStatusCode_03(EmploymentStatusCode.EmployedbyOutsideOrganization_EO).
        withDateTimePeriodFormatQualifier_05(DateTimePeriodFormatQualifier.DateandTimeExpressedinFormatCCYYMMDDHHMM_DT).
        withDateTimePeriod_06(LocalDateTime.ofInstant(context.created, ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern('yyyyMMddHHmm'))).
        withWeight_08(context.patientWeight).
        withYesNoConditionorResponseCode_09(YesNoConditionorResponseCode.No_N)
  }

  private static LocalDateTime parseDateTime(String input) {
    def l = 14 - input.length()
    LocalDateTime.from(DateTimeFormatter.ofPattern('yyyyMMddHHmmss').parse(input + ('0' * l)))
  }

  private static DTP dtp(DateTimeQualifier dateTimeQualifier, String date) {
    new DTP().
        withDateTimeQualifier_01(dateTimeQualifier).
        withDateTimePeriodFormatQualifier_02(DateTimePeriodFormatQualifier.DateandTimeExpressedinFormatCCYYMMDDHHMM_DT).
        withDateTimePeriod_03(parseDateTime(date).format(EdiParser.DateTimeFormat))
  }

  private static DTP dtp(DateTimeQualifier dateTimeQualifier, String startDate, String endDate) {
    new DTP().
        withDateTimeQualifier_01(dateTimeQualifier).
        withDateTimePeriodFormatQualifier_02(DateTimePeriodFormatQualifier.RangeofDatesExpressedinFormatCCYYMMDDCCYYMMDD_RD8).
        withDateTimePeriod_03(parseDateTime(startDate).format(EdiParser.DateFormat) + '-' +
            parseDateTime(endDate).format(EdiParser.DateFormat))
  }

  private static L2310B createServiceEvent(ServiceEvent serviceEvent) {
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

  private static void configurePayer(M837Q1 x12, Payer payer) {
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

  private static void configureAuthor(M837Q1 x12, Author author) {
    x12.withL1000a(new L1000A().
        withNm1_1(new NM1().
            withEntityIdentifierCode_01(EntityIdentifierCode.Submitter_41).
            withEntityTypeQualifier_02(EntityTypeQualifier.NonPersonEntity_2).
            withNameLastorOrganizationName_03(author.of).
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

  private static void configureInsured(M837Q1 x12, Patient patient) {
    if (patient) {
      def nm1Segment = new NM1().
          withEntityIdentifierCode_01(EntityIdentifierCode.InsuredorSubscriber_IL).
          withEntityTypeQualifier_02(EntityTypeQualifier.Person_1).
          withNameLastorOrganizationName_03(patient.family).
          withNameFirst_04(patient.given).
          withIdentificationCodeQualifier_08(IdentificationCodeQualifier.MemberIdentificationNumber_MI).
          withIdentificationCode_09(patient.extension)

      x12.withL2000a(new L2000A().
          withHl_1(hl('1', HierarchicalLevelCode.InformationSource_20)).
          withL2010aa_5(new L2010AA().
              withNm1_1(nm1Segment).
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
      x12.withL2000b(new L2000B().
          withHl_1(hl('2', HierarchicalLevelCode.Subscriber_22)).
          withL2010ba_6(new L2010BA().
              withNm1_1(nm1Segment)
          )
      )
    }
  }

  private static HL hl(String id, HierarchicalLevelCode hierarchicalLevelCode) {
    new HL().
        withHierarchicalIDNumber_01(id).
        withHierarchicalLevelCode_03(hierarchicalLevelCode)
  }

  private static void configureHeaders(M837Q1 x12, X12Context context) {
    def localDateTime = LocalDateTime.ofInstant(context.created, ZoneId.systemDefault())
    x12.
        withSt(new ST().
            withTransactionSetIdentifierCode_01(TransactionSetIdentifierCode.HealthCareClaim_837).
            withTransactionSetControlNumber_02(context.id).
            withImplementationConventionReference_03(context.conventionReference)
        ).
        withBht(new BHT().
            withHierarchicalStructureCode_01(HierarchicalStructureCode.InformationSourceSubscriberDependent_0019).
            withTransactionSetPurposeCode_02(TransactionSetPurposeCode.Original_00).
            withReferenceIdentification_03(context.id).
            withDate_04(localDateTime.toLocalDate()).
            withTime_05(localDateTime.toLocalTime()).
            withTransactionTypeCode_06(TransactionTypeCode.Chargeable_CH)
        )
        //TODO - Temporarily taken out since Dory is not expecting this.
//        .
//        withRef(new REF().
//            withReferenceIdentificationQualifier_01(ReferenceIdentificationQualifier.ClaimNumber_D9).
//            withReferenceIdentification_02(context.id).
//            withDescription_03('Claim')
//        )
  }

  static String serialize(Message document, boolean prettyPrint = false) {
    def tokens = document.toTokens(prettyPrint ? 0 : -1)
    new EdiParser().toEdi(tokens)
  }

  static String serialize(Interchange document, boolean prettyPrint = false) {
    def tokens = document.toTokens(prettyPrint ? 0 : -1)
    new EdiParser().toEdi(tokens)
  }
}
