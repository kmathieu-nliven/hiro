package com.cds.hiro.builders

import com.github.rahulsom.cda.CD
import com.github.rahulsom.cda.CE
import com.github.rahulsom.cda.II
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.builder.Builder
import groovy.transform.builder.SimpleStrategy

/**
 * Represents a CDA Document's data as used by the {@link Cda} class.
 *
 * @author Rahul Somasunderam
 */
@CompileStatic
@AutoClone
@Builder(builderStrategy = SimpleStrategy, prefix = '')
class CdaContext {

  String realmCode = 'US'

  II CDA_R2 = new II().withRoot('2.16.840.1.113883.3.27.1776').withAssigningAuthorityName('CDA/R2'),
     HL7_CDT_HEADER = new II().withRoot('2.16.840.1.113883.10.20.3').withAssigningAuthorityName('HL7/CDT Header'),
     IHE_PCC = new II().withRoot('1.3.6.1.4.1.19376.1.5.3.1.1.1').withAssigningAuthorityName('IHE/PCC'),
     HITSP_C32 = new II().withRoot('2.16.840.1.113883.3.88.11.32.1').withAssigningAuthorityName('HITSP/C32')

  /** Template Ids */
  List<II> templates = [CDA_R2, HL7_CDT_HEADER, IHE_PCC, HITSP_C32]

  /** Template Ids */
  void templates(II... templates) { this.templates = templates.toList() }

  /** Document Identifier */
  String id = UUID.randomUUID().toString()

  /** Document Code */
  CE code

  /** Document title, mostly for human consumption */
  String title = 'Continuity of Care Document'

  /** What confidentiality code is to be used http://www.hl7.org/documentcenter/public_temp_E7D3F1D2-1C23-BA17-0C1AB922E27A8E55/standards/vocabulary/vocabulary_tables/infrastructure/vocabulary/Confidentiality.html */
  CE confidentiality

  /** Language to be used for the document */
  Locale language = Locale.US

  /**
   * The patient to whom this document is tied
   */
  @Builder(builderStrategy = SimpleStrategy, prefix = '')
  static class Patient {
    /** given name of patient */
    String given
    /** family name of patient*/
    String family

    /** Shortcut to set family and given name */
    void name(String family, String given) {
      this.family = family
      this.given = given
    }

    /** patient gender */
    String gender
    /** patient birth time */
    String birthTime

    /** patient marital status */
    String maritalStatus

    /** patient identifer */
    String root, extension

    /** shortcut to set patient identifier */
    void id(String root, String extension) {
      this.root = root
      this.extension = extension
    }

    /**
     * Represents patient address
     */
    @Builder(builderStrategy = SimpleStrategy, prefix = '')
    static class Address {
      String street, city, state, country, postalCode
    }
    Address addr

    void addr(@DelegatesTo(Address) Closure closure) {
      closure.resolveStrategy = Closure.DELEGATE_FIRST
      closure.delegate = addr = new Address()
      closure.call()
    }

    void addr(Address address) {
      addr = address
    }
  }
  Patient patient

  void patient(@DelegatesTo(Patient) Closure closure) {
    closure.resolveStrategy = Closure.DELEGATE_FIRST
    closure.delegate = patient = new Patient()
    closure.call()
  }

  /** Generate a random patient */
  Patient patient() {
    this.patient {
      name 'Doe', 'John'
      gender 'M'
      birthTime '19520413'
      maritalStatus 'M'

      id '1.2.3.4', '42'

      addr {
        street '123 Main St'
        city 'San Jose'
        state 'CA'
        postalCode '95129'
        country 'USA'
      }
    }
    this.patient
  }

  void patient(Patient patient) {
    this.patient = patient
  }

  @Builder(builderStrategy = SimpleStrategy, prefix = '')
  static class Encounter {
    void met(String family, String given) {
      this.given = given
      this.family = family
    }
    String given, family, at
    String between, and
  }
  Encounter encounter

  void encounter(@DelegatesTo(Encounter) Closure closure) {
    closure.resolveStrategy = Closure.DELEGATE_FIRST
    closure.delegate = encounter = new Encounter()
    closure.call()
  }

  @Builder(builderStrategy = SimpleStrategy, prefix = '')
  static class Problem {
    CE code
    String between, and, since, withStatus
    boolean active = false
  }
  List<Problem> problems = []

  Problem suffered(CE code) {
    def problem = new Problem().code(code).active(false)
    problems << problem
    return problem
  }

  Problem suffers(CE code) {
    def problem = new Problem().code(code).active(true)
    problems << problem
    return problem
  }

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
  static class Payer {
    String name
    String identifiedAs
    String identifierIs
  }
  List<Payer> payers = []

  Payer payer(String name) {
    def payer = new Payer().name(name)
    payers << payer
    payer
  }

  @Builder(builderStrategy = SimpleStrategy, prefix = '')
  static class Author {
    String of, identifiedAs, at, given, family
  }
  Author author

  Author authoredBy(String family, String given) {
    author = new Author().family(family).given(given)
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
  static class ServiceEvent {
    String given, family, root, extension, at, identifiedAs

    void id(String root, String extension) {
      this.root = root
      this.extension = extension
    }

    void initiatedBy(String family, String given) {
      this.family = family
      this.given = given
    }
  }
  ServiceEvent serviceEvent

  void serviceEvent(@DelegatesTo(ServiceEvent) Closure closure) {
    closure.resolveStrategy = Closure.DELEGATE_FIRST
    serviceEvent = closure.delegate = new ServiceEvent()
    closure.call()
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
  static class Procedure {
    CD code
    String on, from, to, withStatus
  }
  List<Procedure> procedures = []
  Procedure performed(CD code) {
    def procedure = new Procedure().code(code)
    procedures << procedure
    procedure
  }
}
