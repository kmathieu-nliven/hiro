package com.cds.hiro.builders

import com.cds.hiro.x12.EdiParser
import com.cds.hiro.x12_837p.enums.CommunicationNumberQualifier
import com.cds.hiro.x12_837p.enums.ContactFunctionCode
import com.cds.hiro.x12_837p.enums.EntityIdentifierCode
import com.cds.hiro.x12_837p.enums.EntityTypeQualifier
import com.cds.hiro.x12_837p.enums.HierarchicalStructureCode
import com.cds.hiro.x12_837p.enums.IdentificationCodeQualifier
import com.cds.hiro.x12_837p.enums.LocationQualifier
import com.cds.hiro.x12_837p.enums.ReferenceIdentificationQualifier
import com.cds.hiro.x12_837p.enums.TransactionSetIdentifierCode
import com.cds.hiro.x12_837p.enums.TransactionSetPurposeCode
import com.cds.hiro.x12_837p.enums.TransactionTypeCode
import com.cds.hiro.x12_837p.loops.L1000A
import com.cds.hiro.x12_837p.loops.L1000B
import com.cds.hiro.x12_837p.loops.L2000A
import com.cds.hiro.x12_837p.loops.L2010AA
import com.cds.hiro.x12_837p.segments.BHT
import com.cds.hiro.x12_837p.segments.HL
import com.cds.hiro.x12_837p.segments.N3
import com.cds.hiro.x12_837p.segments.N4
import com.cds.hiro.x12_837p.segments.NM1
import com.cds.hiro.x12_837p.segments.PER
import com.cds.hiro.x12_837p.segments.REF
import com.cds.hiro.x12_837p.segments.ST
import com.cds.hiro.x12_837p.transactionsets.M837Q1

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

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
    configurePatient(x12, context.patient)
    conigurePayer(x12, context.payers?.first())
    x12
  }

  private static void conigurePayer(M837Q1 x12, CdaContext.Payer payer) {
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

  private static void configurePatient(M837Q1 x12, CdaContext.Patient patient) {
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
    x12.withSt(new ST().
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
