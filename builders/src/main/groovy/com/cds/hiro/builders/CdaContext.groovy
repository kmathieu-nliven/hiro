package com.cds.hiro.builders

import com.cds.hiro.builders.contexts.*
import com.github.rahulsom.cda.CD
import com.github.rahulsom.cda.CE
import com.github.rahulsom.cda.II
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.builder.Builder
import groovy.transform.builder.SimpleStrategy

import java.time.Instant
import java.time.LocalDateTime

/**
 * Represents a CDA Document's data as used by the {@link Cda} class.
 *
 * @author Rahul Somasunderam
 */
@CompileStatic
@AutoClone
@Builder(builderStrategy = SimpleStrategy, prefix = '')
class CdaContext extends BaseContext {

  String realmCode = 'US'

  II CDA_R2 = new II().withRoot('2.16.840.1.113883.3.27.1776').withAssigningAuthorityName('CDA/R2'),
     HL7_CDT_HEADER = new II().withRoot('2.16.840.1.113883.10.20.3').withAssigningAuthorityName('HL7/CDT Header'),
     IHE_PCC = new II().withRoot('1.3.6.1.4.1.19376.1.5.3.1.1.1').withAssigningAuthorityName('IHE/PCC'),
     HITSP_C32 = new II().withRoot('2.16.840.1.113883.3.88.11.32.1').withAssigningAuthorityName('HITSP/C32')

  /** Template Ids */
  List<II> templates = [CDA_R2, HL7_CDT_HEADER, IHE_PCC, HITSP_C32]

  /** Template Ids */
  void templates(II... templates) { this.templates = templates.toList() }

  /** Document Code */
  CE code

  /** Document title, mostly for human consumption */
  String title = 'Continuity of Care Document'

  /** What confidentiality code is to be used http://www.hl7.org/documentcenter/public_temp_E7D3F1D2-1C23-BA17-0C1AB922E27A8E55/standards/vocabulary/vocabulary_tables/infrastructure/vocabulary/Confidentiality.html */
  CE confidentiality

  /** Language to be used for the document */
  Locale language = Locale.US

  @Builder(builderStrategy = SimpleStrategy, prefix = '')
  static class FamilyHistory {
    CE code
    String condition
  }
  List<FamilyHistory> familyHistoryList = []

  FamilyHistory familyMember(CE code) {
    def member = new FamilyHistory().code(code)
    familyHistoryList << member
    member
  }

  @Builder(builderStrategy = SimpleStrategy, prefix = '')
  static class SocialHistory {
    CD code
    String status
  }
  List<SocialHistory> socialHistoryList = []

  SocialHistory social(CD code) {
    def social = new SocialHistory().code(code)
    socialHistoryList << social
    social
  }

  @Builder(builderStrategy = SimpleStrategy, prefix = '')
  static class Medication {
    CE code
    String from, to, withStatus
  }
  List<Medication> medications = []

  Medication prescribed(CE code) {
    def medication = new Medication().code(code)
    medications << medication
    medication
  }

  @Builder(builderStrategy = SimpleStrategy, prefix = '')
  static class Immunization {
    CE code
    CE by
    String on, withStatus
  }
  List<Immunization> immunizations = []

  Immunization immunized(CE code) {
    def immunization = new Immunization().code(code)
    immunizations << immunization
    immunization
  }

  @Builder(builderStrategy = SimpleStrategy, prefix = '')
  static class VitalsGroup {
    String on

    @Builder(builderStrategy = SimpleStrategy, prefix = '')
    static class VitalSign {
      CE code
      String at, of
    }
    List<VitalSign> vitalSigns = []

    VitalSign measured(CE code) {
      def vital = new VitalSign().code(code)
      vitalSigns << vital
      vital
    }
  }
  List<VitalsGroup> vitalsGroups = []

  void vitals(@DelegatesTo(VitalsGroup) Closure closure) {
    closure.resolveStrategy = Closure.DELEGATE_FIRST
    def vitals = closure.delegate = new VitalsGroup()
    vitalsGroups << vitals
    closure.call()
  }

  @Builder(builderStrategy = SimpleStrategy, prefix = '')
  static class ResultsGroup {
    String on

    @Builder(builderStrategy = SimpleStrategy, prefix = '')
    static class Result {
      CE code
      String at, of, withRange, was
    }
    List<Result> results = []

    Result measured(CE code) {
      def vital = new Result().code(code)
      results << vital
      vital
    }
  }
  List<ResultsGroup> resultsGroups = []

  void results(@DelegatesTo(ResultsGroup) Closure closure) {
    closure.resolveStrategy = Closure.DELEGATE_FIRST
    def results = closure.delegate = new ResultsGroup()
    resultsGroups << results
    closure.call()
  }

  @Builder(builderStrategy = SimpleStrategy, prefix = '')
  static class Allergy {
    CE code
    CE causes
  }
  List<Allergy> allergies = []

  Allergy allergen(CE code) {
    def allergy = new Allergy().code(code)
    allergies << allergy
    allergy
  }

  @Builder(builderStrategy = SimpleStrategy, prefix = '')
  static class Informant {
    String orgName, identifiedAs
  }
  Informant informant

  Informant informant(String orgName) {
    informant = new Informant().orgName(orgName)
  }

  @Builder(builderStrategy = SimpleStrategy, prefix = '')
  static class Custodian {
    String orgName, identifiedAs
  }
  Custodian custodian

  Custodian custodian(String orgName) {
    custodian = new Custodian().orgName(orgName)
  }

  @Builder(builderStrategy = SimpleStrategy, prefix = '')
  static class Diagnosis {
    CE code
    String performerGiven
    String performerFamily

    Diagnosis by(String family, String given) {
      performerFamily = family
      performerGiven = given
      this
    }
    String at
    String facilityRoot
    String performerId

    Diagnosis identifiedAs(String root, String extension) {
      facilityRoot = root
      performerId = extension
      this
    }
    String on
  }
  List<Diagnosis> diagnoses = []

  void diagnosis(@DelegatesTo(Diagnosis) Closure closure) {
    def diagnosis = closure.delegate = new Diagnosis()
    diagnoses << diagnosis
    closure.resolveStrategy = Closure.DELEGATE_FIRST
    closure.call()
  }

  @Builder(builderStrategy = SimpleStrategy, prefix = '')
  static class Assessment {
    CD code
    String on
    CE toBe
  }
  List<Assessment> assessments = []
  Assessment assessed(CD code) {
    def assessment = new Assessment().code(code)
    assessments << assessment
    assessment
  }
}
