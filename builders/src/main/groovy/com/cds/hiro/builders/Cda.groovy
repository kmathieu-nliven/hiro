package com.cds.hiro.builders

import com.github.rahulsom.ItiHelper
import com.github.rahulsom.cda.*

import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBElement
import javax.xml.bind.Marshaller
import javax.xml.namespace.QName

/**
 * Builder for creating CDA documents.
 *
 * @author Rahul Somasunderam
 */
class Cda {
  /**
   * Create CDA from scratch
   *
   * @param closure builder for configuring how the CDA is built.
   * @return a CDA JAXB object.
   */
  static POCDMT000040ClinicalDocument create(@DelegatesTo(CdaContext) Closure closure) {
    create(new CdaContext(), closure)
  }

  /**
   * Create CDA starting from a defaults configuration. This helps you define some standard configuration in one
   * place and then reuse that configuration to build more customizations out of it.
   *
   * @param closure builder for configuring how the CDA is built.
   * @return a CDA JAXB object.
   */
  static POCDMT000040ClinicalDocument create(CdaContext defaults, @DelegatesTo(CdaContext) Closure closure) {
    def delegate = defaults.clone() as CdaContext
    closure.delegate = delegate
    closure.call()
    createCcd(delegate)
  }

  /**
   * This exists for when you want to create the context entirely on your own and generate a CDA out of it.
   * Use of this method is highly discouraged, but supported nonetheless.
   *
   * @param context a configured context
   * @return a CDA JAXB object.
   */
  static POCDMT000040ClinicalDocument createCcd(CdaContext context) {

    def document = new POCDMT000040ClinicalDocument().
        withTypeId(new POCDMT000040InfrastructureRootTypeId().
            withRoot('2.16.840.1.113883.1.3').
            withExtension('POCD_HD000040'))
    if (context.realmCode) {
      document.withRealmCode(new CS().withCode(context.realmCode))
    }
    document.withTemplateId(context.templates)
    if (context.id) {
      document.id = new II().withRoot(context.id)
    }
    document.code = context.code
    document.title = new ST().withContent(context.title)
    document.confidentialityCode = context.confidentiality
    document.languageCode = new CS().withCode(context.language.toLanguageTag())

    configurePatient(context.patient, document)
    configureAuthor(document, context.author)
    configureInformant(document, context.informant)
    configureCustodian(document, context.custodian)
    configureServiceEvent(document, context.serviceEvent)

    def structuredBody = new POCDMT000040StructuredBody()
    document.withComponent(new POCDMT000040Component2().withStructuredBody(structuredBody))
    addEncounter(structuredBody, context.encounter)
    addProblems(structuredBody, context.problems)
    addFamilyHistory(structuredBody, context.familyHistoryList)
    addSocialHistory(structuredBody, context.socialHistoryList)
    addImmunizations(structuredBody, context.immunizations)
    addMedications(structuredBody, context.medications)
    addVitalSigns(structuredBody, context.vitalsGroups)
    addDiagnoses(structuredBody, context.diagnoses)
    addProcedures(structuredBody, context.procedures)

    document
  }

  private static void addProcedures(POCDMT000040StructuredBody structuredBody, List<CdaContext.Procedure> procedures) {
    if (procedures)
      addSection(structuredBody, generateSectionCode('47519-4')) {
        procedures.each { procedure ->

          def ivlts = procedure.on ?
              new IVLTS().withValue(procedure.on) :
              new IVLTS().withRest([
                  procedure.from ? ItiHelper.jaxb('low', IVLTS, new IVLTS().withValue(procedure.from)) : null,
                  procedure.to ? ItiHelper.jaxb('high', IVLTS, new IVLTS().withValue(procedure.to)) : null,
              ].findAll {it})

          withEntry(new POCDMT000040Entry().withTypeCode(XActRelationshipEntry.DRIV).
              withProcedure(new POCDMT000040Procedure().
                  withClassCode('OBS').withMoodCode(XDocumentProcedureMood.EVN).
                  withCode(procedure.code).withEffectiveTime(ivlts)
              )
          )
        }
      }
  }

  private static void addDiagnoses(
      POCDMT000040StructuredBody structuredBody, List<CdaContext.Diagnosis> diagnoses) {
    if (diagnoses)
      addSection(structuredBody, generateSectionCode('11348-0')) {
        diagnoses.each { diagnosis ->
          withEntry(new POCDMT000040Entry().
              withObservation(new POCDMT000040Observation().
                  withClassCode('OBS').withMoodCode(XActMoodDocumentObservation.EVN).
                  withCode(new CD().withCode('55607006').withCodeSystem('2.16.840.1.113883.6.96')).
                  withValue(diagnosis.code).
                  withPerformer(new POCDMT000040Performer2().
                      withTypeCode(ParticipationPhysicalPerformer.PRF).
                      withAssignedEntity(new POCDMT000040AssignedEntity().
                          withId(new II().withExtension(diagnosis.performerId).withRoot(diagnosis.facilityRoot)).
                          withAddr(new AD().withContent(
                              ItiHelper.jaxb('streetAddressLine', AdxpStreetAddressLine, new AdxpStreetAddressLine().withContent(diagnosis.at))
                          )).
                          withAssignedPerson(new POCDMT000040Person().withName(new PN().withContent(
                              ItiHelper.jaxb('given', EnGiven, new EnGiven().withContent(diagnosis.performerGiven)),
                              ItiHelper.jaxb('family', EnFamily, new EnFamily().withContent(diagnosis.performerFamily)),
                          )))
                      )
                  )
              )
          )
        }
      }

  }

  private static ANY createValue(String type, String value) {
    def (numericValue, unit) = value.split(' ', 2)
    if (type == 'PQ') {
      new PQ().withValue(numericValue).withUnit(unit)
    } else if (type == 'RTO_PQ_PQ') {
      def (numUnit, denomUnit) = unit.split('/', 2)
      def (numValue, denomValue) = (numericValue + '/1').split('/', 2)
      new RTOPQPQ().
          withNumerator(new PQ().withValue(numValue).withUnit(numUnit)).
          withDenominator(new PQ().withValue(denomValue ?: '1').withUnit(denomUnit))
    } else {
      new ST().withContent(value)
    }
  }

  private static void addVitalSigns(
      POCDMT000040StructuredBody structuredBody, List<CdaContext.VitalsGroup> vitalsGroups) {
    if (vitalsGroups)
      addSection(structuredBody, generateSectionCode('8716-3')) {
        vitalsGroups.each { vitalsGroup ->
          withEntry(new POCDMT000040Entry().
              withTypeCode(XActRelationshipEntry.DRIV).
              withOrganizer(new POCDMT000040Organizer().
                  withClassCode(XActClassDocumentEntryOrganizer.CLUSTER).withMoodCode('EVN').
                  withEffectiveTime(new IVLTS().withValue(vitalsGroup.on)).
                  withComponent(vitalsGroup.vitalSigns.collect { vital ->
                    new POCDMT000040Component4().
                        withObservation(new POCDMT000040Observation().
                            withClassCode('OBS').
                            withMoodCode(XActMoodDocumentObservation.EVN).
                            withCode(vital.code).
                            withValue(createValue(vital.of, vital.at))
                        )
                  })
              )
          )
        }
      }
  }

  private static void addMedications(
      POCDMT000040StructuredBody structuredBody, List<CdaContext.Medication> medications) {
    if (medications)
      addSection(structuredBody, generateSectionCode('10160-0')) {
        medications.each { medication ->
          withEntry(new POCDMT000040Entry().
              withSubstanceAdministration(new POCDMT000040SubstanceAdministration().
                  withClassCode('SBADM').withMoodCode(XDocumentSubstanceMood.EVN).
                  withEffectiveTime(/* TODO */).
                  withConsumable(new POCDMT000040Consumable().
                      withManufacturedProduct(new POCDMT000040ManufacturedProduct().
                          withClassCode(RoleClassManufacturedProduct.MANU).
                          withManufacturedMaterial(new POCDMT000040Material().withCode(medication.code))
                      )
                  )
              )
          )
        }

      }
  }

  private static void addImmunizations(
      POCDMT000040StructuredBody structuredBody, List<CdaContext.Immunization> immunizations) {
    if (immunizations)
      addSection(structuredBody, generateSectionCode('11369-6')) {
        immunizations.each { immunization ->
          withEntry(new POCDMT000040Entry().
              withSubstanceAdministration(new POCDMT000040SubstanceAdministration().
                  withEffectiveTime(new SXCMTS().withValue(immunization.on)).
                  withRouteCode(immunization.by).
                  withConsumable(new POCDMT000040Consumable().
                      withManufacturedProduct(new POCDMT000040ManufacturedProduct().
                          withClassCode(RoleClassManufacturedProduct.MANU).
                          withManufacturedMaterial(new POCDMT000040Material().withCode(immunization.code))
                      )
                  )
              )
          )
        }
      }
  }

  private static void addSocialHistory(
      POCDMT000040StructuredBody structuredBody, List<CdaContext.SocialHistory> socialHistories) {
    if (socialHistories)
      addSection(structuredBody, generateSectionCode('29762-2')) {
        socialHistories.each { social ->
          withEntry(new POCDMT000040Entry().
              withTypeCode(XActRelationshipEntry.DRIV).
              withObservation(new POCDMT000040Observation().
                  withClassCode('OBS').withMoodCode(XActMoodDocumentObservation.EVN).
                  withCode(social.code as CD).withValue(new ST().withContent(social.status))
              )

          )
        }
      }

  }

  private static void addFamilyHistory(
      POCDMT000040StructuredBody structuredBody, List<CdaContext.FamilyHistory> familyHistory) {
    if (familyHistory)
      addSection(structuredBody, generateSectionCode('10157-6')) {
        familyHistory.each { family ->
          withEntry(new POCDMT000040Entry().
              withObservation(new POCDMT000040Observation().
                  withClassCode('OBS').
                  withMoodCode(XActMoodDocumentObservation.EVN).
                  withCode(new CD().withCode('ASSERTION').withCodeSystem('2.16.840.1.113883.5.4')).
                  withSubject(new POCDMT000040Subject().
                      withRelatedSubject(new POCDMT000040RelatedSubject().
                          withClassCode(XDocumentSubject.PRS).withCode(family.code)
                      )
                  ).withValue(new ST().withContent(family.condition))
              ))
        }
      }
  }

  private static void addProblems(POCDMT000040StructuredBody structuredBody, List<CdaContext.Problem> problems) {
    if (problems)
      addSection(structuredBody, generateSectionCode('11450-4')) {
        problems.each { problem ->
          withEntry(new POCDMT000040Entry().
              withTypeCode(XActRelationshipEntry.DRIV).
              withAct(new POCDMT000040Act().
                  withClassCode(XActClassDocumentEntryAct.ACT).withMoodCode(XDocumentActMood.EVN).
                  withEntryRelationship(new POCDMT000040EntryRelationship().
                      withTypeCode(XActRelationshipEntryRelationship.SUBJ).
                      withObservation(new POCDMT000040Observation().
                          withClassCode('OBS').withMoodCode(XActMoodDocumentObservation.EVN).
                          withCode(new CD().
                              withCode('64572001').withCodeSystem('2.16.840.1.113883.6.96')
                          ).
                          withEffectiveTime(new IVLTS().withRest(/*TODO*/)).
                          withValue(problem.code)
                      )
                  )
              )
          )
        }
      }
  }

  private static void addEncounter(POCDMT000040StructuredBody structuredBody, CdaContext.Encounter encounter) {
    if (encounter)
      addSection(structuredBody, generateSectionCode('46240-8')) {
        withEntry(new POCDMT000040Entry().
            withTypeCode(XActRelationshipEntry.DRIV).
            withEncounter(new POCDMT000040Encounter().
                withCode(null /*TODO*/).
                withEffectiveTime(new IVLTS().withRest(/*TODO*/)).
                withText(new ED().withContent('TODO')).
                withPerformer(new POCDMT000040Performer2().
                    withAssignedEntity(new POCDMT000040AssignedEntity().
                        withAssignedPerson(new POCDMT000040Person().
                            withName(new PN().withContent(/*TODO*/))
                        )
                    )
                ).
                withParticipant(new POCDMT000040Participant2().
                    withParticipantRole(new POCDMT000040ParticipantRole().withClassCode('SDLOC').
                        withPlayingEntity(new POCDMT000040PlayingEntity().withClassCode('PLC').
                            withName(new PN().withContent(encounter.at)))
                    )
                )
            )
        )

      }
  }

  private static void addSection(
      POCDMT000040StructuredBody structuredBody, CE code, @DelegatesTo(POCDMT000040Section) Closure closure) {

    def section = new POCDMT000040Section().
        withTitle(new ST().
            withContent(code.displayName)).
        withCode(code)

    closure.resolveStrategy = Closure.DELEGATE_FIRST
    closure.delegate = section
    closure.call()

    structuredBody.withComponent(new POCDMT000040Component3().withSection(section))
  }

  private static void configureServiceEvent(
      POCDMT000040ClinicalDocument document, CdaContext.ServiceEvent serviceEvent) {
    if (serviceEvent)
      document.withDocumentationOf(new POCDMT000040DocumentationOf().
          withServiceEvent(new POCDMT000040ServiceEvent().
              withClassCode('PCPR').
              withPerformer(new POCDMT000040Performer1().
                  withFunctionCode(new CE().withCode('PP').withCodeSystem('2.16.840.1.113883.12.443')).
                  withTime(new IVLTS().withRest(/*TODO*/)).
                  withAssignedEntity(new POCDMT000040AssignedEntity().
                      withId(new II().withRoot(serviceEvent.root).withExtension(serviceEvent.extension)).
                      withAssignedPerson(new POCDMT000040Person().
                          withName(new PN().withContent(/*TODO*/))
                      ).
                      withRepresentedOrganization(new POCDMT000040Organization().
                          withId(new II().withRoot(serviceEvent.identifiedAs)).
                          withName(new ON().withContent(serviceEvent.at))
                      )
                  )
              )
          )
      )
  }

  private static void configureInformant(POCDMT000040ClinicalDocument document, CdaContext.Informant informant) {
    if (informant)
      document.withInformant(new POCDMT000040Informant12().
          withAssignedEntity(new POCDMT000040AssignedEntity().
              withRepresentedOrganization(new POCDMT000040Organization().
                  withId(new II().withRoot(informant.identifiedAs)).
                  withName(new ON().withContent(informant.orgName))
              )
          )
      )
  }

  private static void configureCustodian(POCDMT000040ClinicalDocument document, CdaContext.Custodian custodian) {
    if (custodian)
      document.withCustodian(new POCDMT000040Custodian().
          withAssignedCustodian(new POCDMT000040AssignedCustodian().
              withRepresentedCustodianOrganization(new POCDMT000040CustodianOrganization().
                  withId(new II().withRoot(custodian.identifiedAs)).
                  withName(new ON().withContent(custodian.orgName))
              )
          )
      )
  }

  private static void configureAuthor(POCDMT000040ClinicalDocument document, CdaContext.Author author) {
    if (author)
      document.withAuthor(new POCDMT000040Author().
          withTypeCode('AUT').
          withContextControlCode('OP').
          withTime(new TS().withValue(new Date().format('yyyyMMddHHmmssZ'))).
          withAssignedAuthor(new POCDMT000040AssignedAuthor().
              withRepresentedOrganization(new POCDMT000040Organization().
                  withId(new II().withRoot(author.identifiedAs)).
                  withName(new ON().withContent(author.of))
              ).
              withAssignedPerson(new POCDMT000040Person().
                  withName(new PN().withContent([
                  ItiHelper.jaxb('given', EnGiven, new EnGiven().withContent(author.given)),
                  ItiHelper.jaxb('family', EnFamily, new EnFamily().withContent(author.family)),
              ].findAll {it})))
          )
      )
  }

  private static void configurePatient(CdaContext.Patient patient, POCDMT000040ClinicalDocument document) {
    if (patient)
      document.withRecordTarget(new POCDMT000040RecordTarget().
          withPatientRole(new POCDMT000040PatientRole().
              withId(new II().withRoot(patient.root).withExtension(patient.extension)).
              withAddr(new AD().withContent([
                  ItiHelper.jaxb('streetAddressLine', AdxpStreetAddressLine, new AdxpStreetAddressLine().withContent(patient.addr.street)),
                  ItiHelper.jaxb('city', AdxpCity, new AdxpCity().withContent(patient.addr.city)),
                  ItiHelper.jaxb('state', AdxpState, new AdxpState().withContent(patient.addr.state)),
                  ItiHelper.jaxb('postalCode', AdxpPostalCode, new AdxpPostalCode().withContent(patient.addr.postalCode)),
                  ItiHelper.jaxb('country', AdxpCountry, new AdxpCountry().withContent(patient.addr.country)),
              ].findAll { it })).
              withPatient(new POCDMT000040Patient().
                  withName(new PN().withContent([
                      ItiHelper.jaxb('given', EnGiven, new EnGiven().withContent(patient.given)),
                      ItiHelper.jaxb('family', EnFamily, new EnFamily().withContent(patient.family)),
                  ].findAll {it})).
                  withAdministrativeGenderCode(new CE().
                      withCode(patient.gender).
                      withCodeSystem('2.16.840.1.113883.5.1').
                      withCodeSystemName('HL7 AdministrativeGender')
                  ).
                  withBirthTime(new TS().withValue(patient.birthTime)).
                  withMaritalStatusCode(new CE().
                      withCode(patient.maritalStatus).
                      withCodeSystem('2.16.840.1.113883.5.2')
                  )

              )
          )
      )
  }

  /**
   * Serializes a CDA using JAXB
   *
   * @param document The Document to serialize
   * @param prettyPrint whether to prettify the document with wrapping and indentation
   * @return String representation of document
   */
  static String serialize(
      POCDMT000040ClinicalDocument document, boolean prettyPrint = false, String stylesheet = null) {
    def context = JAXBContext.newInstance(POCDMT000040ClinicalDocument)
    def sw = new StringWriter()

    def marshaller = context.createMarshaller()

    def repoHome = "https://raw.githubusercontent.com/rahulsom/ihe-iti"
    def schemaLocation = "urn:hl7-org:v3 $repoHome/master/src/main/resources/cda/infrastructure/cda/CDA.xsd".toString()

    marshaller.setProperty Marshaller.JAXB_SCHEMA_LOCATION, schemaLocation
    marshaller.setProperty Marshaller.JAXB_FORMATTED_OUTPUT, prettyPrint

    if (stylesheet) {
      marshaller.setProperty Marshaller.JAXB_FRAGMENT, true
      sw.println($/<?xml version="1.0"?>/$)
      sw.println($/<?xml-stylesheet type="text/xsl" href="${stylesheet}" ?>/$)
    }

    def qName = generateQName('')
    marshaller.marshal(new JAXBElement(qName, POCDMT000040ClinicalDocument, document), sw)
    sw.toString()
  }

  private static QName generateQName(String prefix) {
    new QName('urn:hl7-org:v3', 'ClinicalDocument', prefix)
  }

  private static CE generateSectionCode(String code) {
    def lookup = [
        '11348-0': 'History of Past Illnesses',
        '46240-8': 'Encounters',
        '48764-5': 'Summary Purpose',
        '11450-4': 'Problems',
        '29762-2': 'Social History',
        '10157-6': 'Family History',
        '11369-6': 'Immunizations',
        '10160-0': 'Medications',
        '8716-3' : 'Vital Signs',
        '30954-2': 'Results',
        '48765-2': 'Allergies, Adverse Reactions, Alerts',
        '48768-6': 'Payers',
        '18776-5': 'Plan of Care',
        '11535-2': 'Hospital Discharge Diagnosis',
        '10183-2': 'Hospital Discharge Medications',
        '29545-1': 'Physical Examination',
        '47519-4': 'Procedures',
    ]

    new CE().
        withCode(code).
        withDisplayName(lookup[code]).
        withCodeSystem('2.16.840.1.113883.6.1').
        withCodeSystemName('LOINC')
  }
}
