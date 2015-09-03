package com.cds.hiro.ccd

/**
 * TODO: Explain This
 *
 * Created by seth.darr on 8/20/15.
 */
class CcdConstants {
  public static final String PurposeCode = '48764-5'
  public static final String PayersCode = '48768-6'
  public static final String AdvanceDirectivesCode = '42348-3'
  public static final String FunctionalStatusCode = '47420-5'
  public static final String ProblemsCode = '11450-4'
  public static final String PastMedicalCode = '11348-0'
  public static final String FamilyHistoryCode = '10157-6'
  public static final String SocialHistoryCode = '29762-2'
  public static final String AllergyCode = '48765-2'
  public static final String MedicationsCode = '10160-0'
  public static final String MedicalEquipmentCode = '46264-8'
  public static final String ImmunizationsCode = '11369-6'
  public static final String VitalSignsCode = '8716-3'
  public static final String ResultsCode = '30954-2'
  public static final String ProceduresCode = '47519-4'
  public static final String EncountersCode = '46240-8'
  public static final String PlanOfCareCode = '18776-5'
  public static final String hl7Namespace = "urn:hl7-org:v3"

  // CCD Constants
  public static final String CcdSourceAttr = 'CcdSource'
  public static final String Unknown = 'UNK'
  public static final String CcdDateTimePat = 'yyyyMMddHHmmss'
  public static final String CcdDatePat = 'yyyyMMdd'

  // SNOMED CT
  public static final String SnoMedCodeSystemName = 'SNOMED CT'
  public static final String SnoMedCodeSystem = '2.16.840.1.113883.6.96'

  //RxNorm
  public static final String RxCodeSystemName = 'RxNORM'
  public static final String RxCodeSystem = '2.16.840.1.113883.6.88'

  //Lab Tests
  public static final String LabTestsCodeSystemName = 'LOINC'
  public static final String LabTestsCodeSystem = '2.16.840.1.113883.6.1'

  // Procedures
  public static final String ProcedureCodeSystemName = 'SNOMED CT'
  public static final String ProcedureCodeSystem = '2.16.840.1.113883.6.96'
}
