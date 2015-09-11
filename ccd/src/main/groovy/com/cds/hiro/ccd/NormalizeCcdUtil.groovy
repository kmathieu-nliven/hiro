package com.cds.hiro.ccd

//import com.cds.healthdock.vocabulary.CodingSystem
import groovy.json.JsonSlurper
import groovy.util.logging.Log4j
import groovy.xml.Namespace
import groovy.xml.QName
import groovy.xml.XmlUtil

import java.text.SimpleDateFormat

/**
 * An utility class to aggregate and normalize ccds
 * Author: Sivanarayana Gaddam
 */
@Log4j
class NormalizeCcdUtil {
  def terminologyClientService
  def executorService
//  Facility currentFacility
  String localApelonServer
  def eventHandlerService
  def ns = new Namespace('urn:hl7-org:v3')

  def parseDocument = { String ccd ->
    try {
      new XmlParser().parseText(ccd)
    } catch (Exception e) {
      log.error "Unable to parse as ccd:\n$ccd"
      null
    }
  }

  def validDocuments = {it}
  /**
   * Aggregates ccd documents into one big ccd
   * @param ccds list ccd documents in string format
   * @param documentMap Map of unique Ids and the corresponding documents
   * @param ccdExists true if ccd present already;false otherwise
   * @return aggregated ccd on success; null otherwise
   */
  def aggregate(List ccds, Map documentMap, ccdExists = true, Map closureMap = [:]) {
    log.debug("Ccd Aggregation is initiated")
    if (ccds) {
      def aggregatedCcd = null
      def clinicalDocuments = ccds.
          collect (parseDocument).
          findAll (validDocuments).
          collect (dereferencedCcd)

      if (!clinicalDocuments) {
        log.error("No valid ccds found to aggregate.")
        return null
      }

      def ccdInternalId = documentMap?.keySet()?.find {it} ?: ''
      if (clinicalDocuments.size() < 2) {
        log.info("Received only one ccd. Returning the same as aggregated CCD")
        aggregatedCcd = clinicalDocuments[0]
        addDocumentSourceElementToCcd(aggregatedCcd, ccdInternalId, documentMap)
        def componentArray = aggregatedCcd[ns.component][ns.structuredBody][ns.component]
        def oldEffectiveTime = aggregatedCcd[ns.effectiveTime]?.@value?.getAt(0)
        componentArray?.each {component ->
          streamComponent(component, oldEffectiveTime, closureMap)
        }
        String aggregatedCcdStr = XmlUtil.serialize(aggregatedCcd)
        log.debug("Storing the same ccd as aggregated ccd")
        return aggregatedCcdStr
      }

      log.debug("Adding CDA Header to aggregated CCD from the latest CCD")
      if (clinicalDocuments) {
        aggregatedCcd = clinicalDocuments[0]
        if (!ccdExists) {
          addDocumentSourceElementToCcd(aggregatedCcd, ccdInternalId, documentMap)
        }
      }
      log.debug("Adding aggregated CDA Body to aggregated CCD from the latest CCD")
      aggregatedCcd = getAggregatedCcd(aggregatedCcd, clinicalDocuments[1..-1], documentMap, true, true, true, closureMap)

      String aggregatedCcdStr = XmlUtil.serialize(aggregatedCcd)
      log.debug("Ccd Aggregation is finished! aggregated ${clinicalDocuments.size()} ccds into one big ccd!")
      return aggregatedCcdStr
    }
    log.info("No ccds received to aggregate into one ccd. returning null")
    return null
  }

  /**
   * Invoke new entry closures so that they gets streamed
   * @param sectionCode
   * @param entries
   * @param closureMap
   */
  void streamEntries(String sectionCode, List entries, Map closureMap) {
    if (closureMap) {
      entries.each {
        Map entryMap = [sectionCode: sectionCode, entry: it]
        if (sectionCode in [CcdConstants.VitalSignsCode, CcdConstants.ResultsCode]) {
          getObservationComponents(it).each {observationComponent ->
            entryMap.observation = observationComponent[ns.observation]?.getAt(0)
            callClosuresOnNewEntry(closureMap, entryMap)
          }
        } else {
          callClosuresOnNewEntry(closureMap, entryMap)
        }
      }
    } else {
      log.debug("Unable to stream section:${sectionCode} entries since empty closures are passed")
    }
  }

  /**
   * Return list of observations from Result/Vital signs component
   * @param entry ccd entry from Result/Vital Signs section
   * @return observations on success; empty otherwise
   */
  List getObservationComponents(def entry) {
    if (entry) {
      List observationComponents1 = entry[ns.organizer][ns.component].findAll {it[ns.observation]}
      List observationComponents2 = entry[ns.procedure][ns.entryRelationship][ns.organizer][ns.component].findAll {it[ns.observation]}
      observationComponents1 + observationComponents2
    } else {
      []
    }
  }

  /**
   * Stream the whole component
   * @param component CCD Component
   * @param oldEffectiveTime component effective time
   * @param closureMap Map of closures to be called on new/known entries
   */
  void streamComponent(def component, def oldEffectiveTime, Map closureMap) {
    def sectionCode = component[ns.section][ns.code]?.@code?.getAt(0)
    List latestEntries = []
    if (sectionCode == CcdConstants.PhysicalExamination) {
      latestEntries = component?.getAt(ns.section)?.getAt(ns.component)
    } else {
      latestEntries = component?.getAt(ns.section)?.getAt(ns.entry)
    }
    if (oldEffectiveTime) {
      updateEffectiveTime(latestEntries, oldEffectiveTime, sectionCode)
    }
    streamEntries(sectionCode, latestEntries, closureMap)
  }

  /**
   * call closure asynchronously
   * @param closure
   * @param entryMap Map of section code and entry. [sectionCode:sectionCode, entry:entry]
   */
  def callClosureAsync(Closure closure, def entryMap) {
    closure?.call(entryMap)
  }

  /**
   * Update header of aggregated document
   * @param aggregatedDocument
   */
  def updateHeader(def aggregatedDocument) {
    //TODO: Implement when certify header is prepared
    // Till then use the latest ccd header
  }

  /**
   * Generates aggregated ccd
   * @param oldCcd existing ccd
   * @param newCcds new ccd list
   * @param documentMap map of unique ids to document source
   * @param deRef dereference text on true;
   * @param addSource add source element on true
   * @param genHtml generate aggregated html on true
   * @param closuresMap map of call back closures
   * @return aggregated ccd
   */
  def getAggregatedCcd(oldCcd, newCcds, Map documentMap, boolean deRef, boolean addSource, boolean genHtml, Map closuresMap = [:]) {
    def interimOldCcd = oldCcd
    List interimNewCcds = newCcds
    def oldEffectiveTime = interimOldCcd[ns.effectiveTime]?.@value?.getAt(0)

    def documentationOf = interimNewCcds[-1]?.getAt(ns.documentationOf)
    String custodian = documentationOf?.getAt(0)?.depthFirst()?.getAt(ns.assignedPerson)?.getAt(ns.name)?.getAt(0)?.depthFirst()*.text()?.join(' ')

    def recordTarget = interimNewCcds[-1]?.getAt(ns.recordTarget)
    String patient = recordTarget?.getAt(0)?.depthFirst()?.getAt(ns.patientRole)?.getAt(ns.patient)?.getAt(ns.name)?.getAt(0)?.depthFirst()*.text()?.join(' ')
    CcdComponentToHtmlUtil componentToHtmlUtil = new CcdComponentToHtmlUtil(custodian: custodian, patient: patient)
    log.debug "Aggregating new ccd documents: ${newCcds.size()} with the existing CCD"
    interimOldCcd = addNewComponentsToAggregatedCcd(interimOldCcd, newCcds, closuresMap)
    def componentArray = interimOldCcd[ns.component][ns.structuredBody][ns.component]
    componentArray?.each {component ->
      def sectionCode = component[ns.section][ns.code]?.@code?.getAt(0)
      try {
        List latestEntries = component[ns.section][ns.entry]
        // De-reference text items and update effective time for the old ccd
        if (deRef) {
          updateEffectiveTime(latestEntries, oldEffectiveTime, sectionCode)
        }
        aggregateSectionComponents(component, interimNewCcds, documentMap, addSource, deRef, closuresMap)
        def componentSection = component[ns.section]?.getAt(0)
        if (genHtml) {
          log.debug "Generated html version of the aggregated component :${sectionCode}"
          // componentSection.remove(componentSection.text)
          // componentToHtmlUtil.addHtmlTextToComponent(sectionCode, componentSection)
        }
      } catch (Exception e) {
        log.error "Unable to aggregate ccd section ${sectionCode}, errors:", e
      }
    }
    log.info "Aggregation of new ccd documents: ${newCcds.size()} with the existing CCD is successful"
    return interimOldCcd
  }

  /**
   * Add any new components from incoming ccds to facilitate aggregation
   * @param aggregatedCcd
   * @param newCcds
   * @return aggregated ccd with new entries if found; else returns the same
   */
  def addNewComponentsToAggregatedCcd(def aggregatedCcd, List newCcds, Map closureMap) {
    if (aggregatedCcd && newCcds) {
      def componentArray = aggregatedCcd[ns.component][ns.structuredBody][ns.component]
      newCcds.each {newCcd ->
        def newCcdComponents = newCcd[ns.component][ns.structuredBody][ns.component]
        newCcdComponents.each {newComponent ->
          def newSection = newComponent[ns.section][ns.code]?.@code?.getAt(0)
          def knownComponent = componentArray.find {it[ns.section][ns.code]?.@code?.getAt(0) == newSection}
          if (!knownComponent) {
            log.debug("Detected new component and adding to aggregated:${newSection}")
            componentArray[0]?.parent()?.children()?.add(newComponent)
            if (closureMap) {
              log.debug("Streaming new component:${newSection}")
              streamComponent(newComponent, null, closureMap)
            }
          }
        }
      }
      return aggregatedCcd
    }
  }

  /**
   * Aggregates ccd components into one
   * @param component ccd component; e.g: Problems, medications etc..
   * @param newCcds incoming ccds that need to be merged
   * @param documentMap Map of documents and their corresponding unique Ids
   * @return aggregated component on success;
   */
  def aggregateSectionComponents(component, List newCcds, Map documentMap, boolean addSource = true, boolean deRef = true, Map closuresMap = [:]) {
    def sectionCode = component[ns.section][ns.code]?.@code?.getAt(0)
    def sectionCodeSystem = component[ns.section][ns.code]?.@codeSystem?.getAt(0)
    List ccdInternalIds = documentMap.keySet().toList()
    log.debug "Aggregating component section: ${sectionCode}"
    newCcds?.eachWithIndex {newCcd, idx ->
      def desiredComponents = newCcd[ns.component][ns.structuredBody][ns.component].findAll {
        it[ns.section][ns.code]?.@code?.getAt(0) == sectionCode && it[ns.section][ns.code]?.@codeSystem?.getAt(0) == sectionCodeSystem
      }
      log.debug "Found ${desiredComponents?.size()} components in new ccd"
      String ccdInternalId = desiredComponents && ccdInternalIds ? ccdInternalIds[idx] : null
      if (addSource) {
        addDocumentSourceElementToComponent(desiredComponents[0], ccdInternalId, documentMap)
      }
      List oldEntries = component[ns.section][ns.entry]
      List newEntries = []
      desiredComponents?.each { desiredComponent ->
        log.debug "Appending ${desiredComponents?.size()} components from new ccd to the existing ccd"
        def desiredEntries = desiredComponent[ns.section][ns.entry]
        updateEffectiveTime(desiredEntries, newCcd[ns.effectiveTime]?.@value?.getAt(0), sectionCode)
        newEntries?.addAll(desiredEntries)
      }
      closuresMap.'updateEntry' = null
      closuresMap.'onSpecialKey' = null
      closuresMap.'onFallBackKey' = null
      List mergedEntries = mergeGenericSection(sectionCode, oldEntries, newEntries, closuresMap)
      def discard = component[ns.section][ns.entry]
      def componentSection = component[ns.section][ns.entry]?.getAt(0)?.parent()
      componentSection = componentSection ?: component[ns.section]?.getAt(0)
      discard.each {componentSection?.remove(it) }
      mergedEntries.each {componentSection?.children()?.add(it)}
    }
  }

  /**
   * Update effective time for components
   * @param entries component entries; for e.g each medication or problem
   * @param component ccd component; e.g Problems or Medications
   * @param sectionCode code to identify component
   */
  def updateEffectiveTime(def entries, def dateTime, def sectionCode) {
    // Update only for purpose section if not present for now.
    // TODO: Investigate the need for other sections
    if (sectionCode == CcdConstants.PurposeCode) {
      entries.each { entry ->
        def effectiveTime = entry[ns.act][ns.effectiveTime]
        if (!effectiveTime) {
          def formattedDateTime = new SimpleDateFormat(CcdConstants.CcdDateTimePat).parse(dateTime).format(CcdConstants.CcdDateTimePat)
          new Node(entry.act[0], 'effectiveTime', formattedDateTime)
        }
      }
    }
  }

  /**
   * Dereferences data in reference elements. While references is what IHE recommends, it's a pain to manage.
   *
   * @param ccdRoot a Node representing the root of the CCD.
   */
  def dereferencedCcd = { Node ccdRoot ->
    def allElements = ccdRoot.depthFirst()
    def refElements = allElements.findAll { elem ->
      elem instanceof Node && (
      (elem.name() instanceof QName && elem.name().localPart == 'reference') ||
          (elem.name() instanceof String && elem.name() == 'reference')
      )
    }
    refElements.
        findAll { elem -> elem.@value?.startsWith('#') }.
        each { Node elem ->
          try {
            def theValueElem = allElements.find { it instanceof Node && it.@ID == elem.@value[1..-1] }
            def theValue = theValueElem?.value()?.getAt(0)
            // leave following commented code in. It's helpful in debugging sometimes
            // log.debug "${getXPath(elem)} :: ${elem.@value} -> ${theValue}"
            def theParent = elem.parent()
            theParent.remove(elem)
            theParent.setValue(theValue)
          } catch (Exception e) {
            log.debug "Could not deref ${elem.@value} because of $e"
          }
        }
    ccdRoot
  }

  def getXPath(Node node) {
    def thisPath = node.name() instanceof String ? node.name() : (node.name() instanceof QName ? node.name().localPart : 'UNKNOWN')
    if (node.parent()) {
      getXPath(node.parent()) + "/" + thisPath
    } else {
      thisPath
    }
  }

  /**
   * Add source of the ccd to aggregated document
   * @param ccd ccd document
   * @param ccdInternalId internal document id
   * @param documentMap Map of document unique ids to ccd documents
   */
  def addDocumentSourceElementToCcd(def ccd, def ccdInternalId, Map documentMap) {
    def componentArray = ccd[ns.component][ns.structuredBody][ns.component]
    if (componentArray && ccdInternalId) {
      componentArray.each { component ->
        addDocumentSourceElementToComponent(component, ccdInternalId, documentMap)
      }
    } else {
      log.error("Unable to add source elements for the ccd:${ccdInternalId}")
    }
  }

  /**
   * Add document source element for each entry in teh component
   * @param component ccd component for e.g Problems
   * @param ccdInternalId document unique id
   * @param documentMap map of unique id to document source;e.g['1.2.3.4':'San Jose General Hospital']
   * document source can be obtained from classification whose scheme is 'urn:uuid:93606bcf-9494-43ec-9b4e-a7748d1a838d'
   * and slot is authorInstitution
   */
  def addDocumentSourceElementToComponent(def component, def ccdInternalId, Map documentMap) {
    if (component && ccdInternalId && documentMap) {
      component[ns.section][ns.entry].each {
        def ccdSource = it.children().find {it.@id == CcdConstants.CcdSourceAttr}
        // Add another ccd id
        if (ccdSource) {
          new Node(ccdSource, 'ccd', ccdInternalId)
        } else {
          // Create new text for CcdSource ids
          def ccdSourceNode = new Node(it, 'text')
          ccdSourceNode.@id = CcdConstants.CcdSourceAttr
          def sourceMap = documentMap."${ccdInternalId}"
          ccdSourceNode.@source = sourceMap.source
          new Node(ccdSourceNode, 'ccd', ccdInternalId)
        }
      }
    }
  }

  /**
   * Aggregate CDA Body components
   * @param sectionCode - code to identify component
   * @param targetEntries - component entries to get merged
   * @return merged component
   */
  def mergeGenericSection(String sectionCode, def oldEntries, def newEntries, Map closuresMap = [:]) {
    List targetEntries = oldEntries + newEntries
    switch (sectionCode) {
      case CcdConstants.ProblemsCode:
       def gPathExpr = {entry -> entry[ns.act][ns.entryRelationship].getAt(0)[ns.observation]?.getAt(0)?.getAt(ns.value)?.getAt(0)}
        def freeTextClosure = {entryCodeMap ->
          entryCodeMap?.getAt(ns.translation)?.getAt(0)?.@displayName
        }
        closuresMap.'updateEntry' = updateProblemDuration
        return mergeComponentEntries(sectionCode, oldEntries, newEntries, 'SNOMED', closuresMap, gPathExpr, freeTextClosure, true)
      case CcdConstants.MedicationsCode:
        return mergeDrugInformation(sectionCode, oldEntries, newEntries, true, closuresMap)
      case CcdConstants.VitalSignsCode:
        Map gPathClosures = [:]
        gPathClosures.componentsClosure = {entry -> getObservationComponents(entry)}
        gPathClosures.codeClosure = {vitalComp -> vitalComp[ns.observation][ns.code]?.getAt(0)}
        gPathClosures.timeClosure = {vitalComp -> vitalComp[ns.observation][ns.effectiveTime]?.getAt(0)?.@value}
        gPathClosures.keyClosure = {vitalComp ->
          "${vitalComp[ns.observation][ns.code]?.@code?.getAt(0)}**${vitalComp[ns.observation][ns.effectiveTime]?.getAt(0)?.@value}"
        }
        gPathClosures.eventClosure = {vitalComp, id, time -> "${id}${time}"}
        return mergeComponentEntriesByInPlace(sectionCode, oldEntries, newEntries, 'SNOMED', gPathClosures, closuresMap, null)
      case CcdConstants.ProceduresCode:
        def gPathExpr = {entry -> entry[ns.procedure][ns.code]?.getAt(0)}
        def freeTextClosure = {procedure -> procedure?.@displayName}
        closuresMap.'updateEntry' = updateProcedureDuration
        return mergeComponentEntries(sectionCode, oldEntries, newEntries, 'SNOMED', closuresMap, gPathExpr, freeTextClosure, true)
      case CcdConstants.ResultsCode:
        Map gPathClosures = [:]
        gPathClosures.componentsClosure = {entry -> getObservationComponents(entry)}
        gPathClosures.codeClosure = {result -> result[ns.observation][ns.code]?.getAt(0)}
        gPathClosures.timeClosure = {result -> result[ns.observation][ns.effectiveTime]?.getAt(0)?.@value}
        gPathClosures.keyClosure = {result ->
          "${result[ns.observation][ns.code]?.@code?.getAt(0)}**${result[ns.observation][ns.effectiveTime]?.getAt(0)?.@value}"
        }
        gPathClosures.eventClosure = {result, id, time ->
          "${result[ns.observation][ns.id]?.@extension?.getAt(0) ?: result[ns.observation][ns.id]?.@root?.getAt(0)}${id}${time}"
        }
        return mergeComponentEntriesByInPlace(sectionCode, oldEntries, newEntries, 'SNOMED', gPathClosures, closuresMap, updateResultsSemanticInformation)
      case CcdConstants.ImmunizationsCode:
        return mergeDrugInformation(sectionCode, oldEntries, newEntries, true, closuresMap)
      case CcdConstants.AllergyCode:
        def gPathExpr = {entry ->
          def allergy = entry[ns.act][ns.entryRelationship].find {it[ns.observation]}
          return allergy ? allergy[ns.observation][ns.participant][ns.participantRole][ns.playingEntity][ns.code]?.getAt(0) : null
        }
        def freeTextClosure = {allergyObservation -> allergyObservation?.@displayName}
        closuresMap.'updateEntry' = updateAllergyStatusAndEffectiveTime
        return mergeComponentEntries(sectionCode, oldEntries, newEntries, 'SNOMED', closuresMap, gPathExpr, freeTextClosure, false)
      case CcdConstants.SocialHistoryCode:
        def gPathExpr = {entry ->
          def socialHabit = entry[ns.observation][ns.entryRelationship].find {it[ns.observation]}
          socialHabit = socialHabit ? socialHabit[ns.observation][ns.value]?.getAt(0) : null
          def socialHabitCode = socialHabit?.@code ? socialHabit : socialHabit?.getAt(ns.translation)?.getAt(0)
          socialHabitCode ?: entry[ns.observation][ns.code]?.getAt(0)
        }
        def freeTextClosure = { socialHabit -> socialHabit?.@displayName ?: socialHabit[ns.translation]?.getAt(0)?.@displayName }
        closuresMap.'updateEntry' = updateSocialHistoryStatusAndTime
        return mergeComponentEntries(sectionCode, oldEntries, newEntries, 'LOINC', closuresMap, gPathExpr, freeTextClosure, false)
      case CcdConstants.FamilyHistoryCode:
        def gPathExpr = {entry -> entry[ns.observation][ns.subject][ns.relatedSubject][ns.code]?.getAt(0) }
        def freeTextClosure = { entryCode -> entryCode?.@displayName}
        closuresMap.'onSpecialKey' = {entry, codeMap -> codeMap.code + codeMap.displayName}
        return mergeComponentEntries(sectionCode, oldEntries, newEntries, 'LOINC', closuresMap, gPathExpr, freeTextClosure, false)
      case CcdConstants.EncountersCode:
        return mergeEncountersSection(sectionCode, oldEntries, newEntries, closuresMap)
      case CcdConstants.AdvanceDirectivesCode:
        def gPathExpr = {entry ->
          def directive = entry[ns.observation][ns.entryRelationship].find {it[ns.observation]}
          return directive ? directive[ns.observation][ns.value]?.getAt(0) : null
        }
        def freeTextClosure = { directiveCode -> directiveCode?.@displayName}
        closuresMap.'updateEntry' = updateObservationTimeFromLatestObservation
        return mergeComponentEntries(sectionCode, oldEntries, newEntries, 'LOINC', closuresMap, gPathExpr, freeTextClosure, false)
      case CcdConstants.PurposeCode:
        def gPathExpr = {entry ->
          entry[ns.act][ns.entryRelationship]*.depthFirst().getAt(0)[ns.code]?.getAt(0)
        }
        closuresMap.'onSpecialKey' = {entry, codeMap ->
          "${codeMap.code}-${entry[ns.act][ns.effectiveTime]?.getAt(0)?.@value ?: entry[ns.act][ns.effectiveTime]?.getAt(0)?.text()}-${codeMap.displayName}"
        }
        def freeTextClosure = { purposeCode -> purposeCode?.@displayName}
        return mergeComponentEntries(sectionCode, oldEntries, newEntries, 'LOINC', closuresMap, gPathExpr, freeTextClosure, false)
      case CcdConstants.PayersCode:
        closuresMap.'onFallBackKey' = {entry, codeMap ->
          entry[ns.act]?.getAt(0)?.depthFirst()?.getAt(ns.performer)?.getAt(ns.assignedEntity)?.getAt(ns.representedOrganization)?.getAt(ns.id)?.@root?.getAt(0)
        }
        return mergeComponentEntries(sectionCode, oldEntries, newEntries, 'LOINC', closuresMap, null, null, false)
      default:
        break
    }
    return targetEntries
  }

  /**
   * Update known observation from the latest observation
   * @param knownDirective
   * @param directiveObservation
   * @return
   */
  def updateObservationTimeFromLatestObservation = {knownDirective, newDirective ->
    // Just update the latest state and latest time stamp
    def knownObservation = knownDirective[ns.observation][ns.entryRelationship]?.find {it.getAt(ns.observation)}?.getAt(ns.observation)
    def directiveObservation = newDirective[ns.observation][ns.entryRelationship]?.find {it.getAt(ns.observation)}?.getAt(ns.observation)
    knownObservation[ns.statusCode]?.getAt(0)?.@code = directiveObservation[ns.statusCode]?.getAt(0)?.@code ?: knownObservation[ns.statusCode]?.getAt(0)?.@code
    knownObservation[ns.effectiveTime][ns.low]?.getAt(0)?.@value = directiveObservation[ns.effectiveTime][ns.low]?.@value?.getAt(0) ?: knownObservation[ns.effectiveTime][ns.low]?.@value?.getAt(0)
    knownObservation[ns.effectiveTime][ns.high]?.getAt(0)?.@value = directiveObservation[ns.effectiveTime][ns.high]?.@value?.getAt(0) ?: knownObservation[ns.effectiveTime][ns.high]?.@value?.getAt(0)
    return knownDirective
  }

  /**
   * Update social history status and time
   */
  def updateSocialHistoryStatusAndTime = {knownEntry, newEntry ->
    if (knownEntry && newEntry) {
      def knownObservation = knownEntry[ns.observation][ns.entryRelationship].find {it[ns.observation]}
      knownObservation = knownObservation ? knownObservation[ns.observation] : null
      def newObservation = newEntry[ns.observation][ns.entryRelationship].find {[ns.observation]}
      newObservation = newObservation ? newObservation[ns.observation] : null
      String newStatus = newObservation?.getAt(ns.statusCode)?.getAt(0)?.@code
      if (newStatus) {
        knownObservation[ns.statusCode]?.getAt(0)?.@code = newStatus
      }
      updateObservationEffectiveTime(knownObservation, newObservation)
    }
  }

  /**
   * Update the time range of observation
   * @param knownObservation
   * @param newObservation
   * @return updated observation
   */
  def updateObservationEffectiveTime(def knownObservation, def newObservation) {
    if (knownObservation && newObservation) {
      //Update duration of allergy
      def lowTime = newObservation[ns.effectiveTime][ns.low]?.@value?.getAt(0)
      def highTime = newObservation[ns.effectiveTime][ns.high]?.@value?.getAt(0)
      Date lowDateTime = lowTime ? new SimpleDateFormat(CcdConstants.CcdDateTimePat).parse(formatDateTime(lowTime)) : null
      Date highDateTime = highTime ? new SimpleDateFormat(CcdConstants.CcdDateTimePat).parse(formatDateTime(highTime)) : null
      if (lowDateTime) {
        def knownLowTime = knownObservation[ns.effectiveTime][ns.low]?.@value?.getAt(0)
        knownLowTime = knownLowTime ?: lowTime
        Date knownLowDateTime = new SimpleDateFormat(CcdConstants.CcdDateTimePat).parse(formatDateTime(knownLowTime))
        knownObservation[ns.effectiveTime][ns.low]?.getAt(0)?.@value = lowDateTime?.before(knownLowDateTime) ? lowDateTime.format(CcdConstants.CcdDateTimePat) : knownLowTime
      }
      if (highDateTime) {
        def knownHighTime = knownObservation[ns.effectiveTime][ns.high]?.@value?.getAt(0)
        knownHighTime = knownHighTime ?: highTime
        Date knownHighDateTime = new SimpleDateFormat(CcdConstants.CcdDateTimePat).parse(formatDateTime(knownHighTime))
        knownObservation[ns.effectiveTime][ns.high]?.getAt(0)?.@value = highDateTime?.after(knownHighDateTime) ? highDateTime.format(CcdConstants.CcdDateTimePat) : knownHighTime
      }

    }
    return knownObservation
  }

  /**
   * Update allergy status and effective time
   */
  def updateAllergyStatusAndEffectiveTime = { knownEntry, newEntry ->
    if (knownEntry && newEntry) {
      def knownObservation = knownEntry[ns.act][ns.entryRelationship].find {it[ns.observation]}?.getAt(ns.observation)
      def newObservation = newEntry[ns.act][ns.entryRelationship].find {it[ns.observation]}?.getAt(ns.observation)
      if (newObservation[ns.statusCode]?.@code?.getAt(0)) {
        knownObservation[ns.statusCode]?.getAt(0)?.@code = newObservation[ns.statusCode]?.@code?.getAt(0)
      }
      updateObservationEffectiveTime(knownObservation, newObservation)
    }
    return knownEntry
  }

  /**
   * aggregates drug information. for e.g medications/immunizations
   * @param sectionCode component section code
   * @param targetEntries all entries A + B
   * @param normalize normalize if true;none on false
   * @param closuresMap Map of closures, onNew/onKnown entries and onUpdateEntry
   * @return aggregated entries
   */
  def mergeDrugInformation(String sectionCode, def oldEntries, def newEntries, boolean normalize, Map closuresMap = [:]) {
    if (newEntries) {
      def gPathExpr = {knownDrug ->
        knownDrug[ns.substanceAdministration][ns.consumable][ns.manufacturedProduct][ns.manufacturedMaterial][ns.code]?.getAt(0)
      }
      def freeTextClosure = {  manufacturedMaterial ->
        String freeText = manufacturedMaterial?.@displayName
        freeText ?: manufacturedMaterial[ns.originalText][ns.reference]?.@value?.getAt(0)
      }
      closuresMap.'onSpecialKey' = {entry, codeMap ->
        def low = entry[ns.substanceAdministration][ns.effectiveTime]?.getAt(0)[ns.low]?.getAt(0)?.@value
        def high = entry[ns.substanceAdministration][ns.effectiveTime]?.getAt(0)[ns.high]?.getAt(0)?.@value
        "${codeMap.code}${low}-${high}"
      }
      return mergeComponentEntries(sectionCode, oldEntries, newEntries, 'RxNORM', closuresMap, gPathExpr, freeTextClosure, normalize)
    }
    return oldEntries
  }

  /**
   * format datetime to ccd date time format if not comply
   * @param dateTimeStr
   * @return formatted date time
   */
  def formatDateTime(String dateTimeStr) {
    if (dateTimeStr) {
      (CcdConstants.CcdDateTimePat.length() - dateTimeStr.length()).times {dateTimeStr += '0'}
    }
    return dateTimeStr
  }

  /**
   * Update the duration of the procedure
   * @param targetProcedure known procedure entry
   * @param newProcedure new procedure entry
   */
  def updateProcedureDuration = {targetProcedure, newProcedure ->
    if (targetProcedure && newProcedure) {
      // Update the latest status of the procedure
      if (newProcedure[ns.procedure][ns.statusCode]?.@code?.getAt(0)) {
        targetProcedure[ns.procedure][ns.statusCode]?.getAt(0)?.@code = newProcedure[ns.procedure][ns.statusCode]?.getAt(0)?.@code
      }
      def targetLow = targetProcedure[ns.procedure][ns.effectiveTime]?.getAt(0)[ns.low]?.@value?.getAt(0)
      def targetHigh = targetProcedure[ns.procedure][ns.effectiveTime]?.getAt(0)[ns.high]?.@value?.getAt(0)
      def newLow = newProcedure[ns.procedure][ns.effectiveTime]?.getAt(0)[ns.low]?.@value?.getAt(0)
      def newHigh = newProcedure[ns.procedure][ns.effectiveTime]?.getAt(0)[ns.high]?.@value?.getAt(0)
      def targetLowDate = targetLow ? new SimpleDateFormat(CcdConstants.CcdDateTimePat).parse(formatDateTime(targetLow)) : null
      def targetHighDate = targetHigh ? new SimpleDateFormat(CcdConstants.CcdDateTimePat).parse(formatDateTime(targetHigh)) : null
      def newLowDate = newLow ? new SimpleDateFormat(CcdConstants.CcdDateTimePat).parse(formatDateTime(newLow)) : null
      def newHighDate = newHigh ? new SimpleDateFormat(CcdConstants.CcdDateTimePat).parse(formatDateTime(newHigh)) : null
      if (newLowDate) {
        targetLow = newLowDate?.before(targetLowDate) ? newLowDate.format(CcdConstants.CcdDateTimePat) : targetLow
        targetProcedure[ns.procedure][ns.effectiveTime]?.getAt(ns.low)?.getAt(0)?.@value = targetLow
      }
      if (newHighDate) {
        targetHigh = newHighDate?.after(targetHighDate) ? newHighDate.format(CcdConstants.CcdDateTimePat) : targetHigh
        targetProcedure[ns.procedure][ns.effectiveTime]?.getAt(ns.high)?.getAt(0)?.@value = targetHigh
      }
    }
    return targetProcedure
  }

  /**
   * Normalize results section
   */
  def updateResultsSemanticInformation = {semanticCodes, vocabularyMap, results, eventToId ->
    if (semanticCodes && vocabularyMap && results) {
      List vocabulary = vocabularyMap.values().toList()
      if (semanticCodes.size() == vocabulary.size()) {
        vocabulary.eachWithIndex {result, idx ->
          if (semanticCodes[idx]) {
            def knownResult = results.get("${eventToId.get("${result.code}**${result.time}")}")
            def knownResults = knownResult[ns.organizer][ns.component]?.findAll {it[ns.observation]}
            knownResults.each {
              def knownResultCode = it.observation?.code[0]
              knownResultCode?.@code = semanticCodes[idx].code ?: knownResultCode?.@code
              knownResultCode?.@displayName = semanticCodes[idx].name ?: knownResultCode?.@displayName
              knownResultCode?.@codeSystem = CcdConstants.LabTestsCodeSystem
              knownResultCode?.@codeSystemName = CcdConstants.LabTestsCodeSystemName
            }
          }
        }
      }
    }
  }

  /**
   * Verify if the two observations happened at the same time
   * @param knownObservation
   * @param newObservation
   * true if happened at same time; false otherwise
   */
  def isObservedAtSameTime(def knownObservation, def newObservation) {
    if (knownObservation && newObservation) {
      def knownStatus = knownObservation?.statusCode[0]?.@code
      def knownLowTime = knownObservation?.effectiveTime?.low[0]?.@value
      Date knownLowDate = knownLowTime ? new SimpleDateFormat(CcdConstants.CcdDateTimePat).parse(formatDateTime(knownLowTime)) : null
      def knownHighTime = knownObservation?.effectiveTime?.high[0]?.@value
      Date knownHighDate = knownHighTime ? new SimpleDateFormat(CcdConstants.CcdDateTimePat).parse(formatDateTime(knownHighTime)) : null
      def newStatus = newObservation?.statusCode[0]?.@code
      def newLowTime = newObservation?.effectiveTime?.low[0]?.@value
      Date newLowDate = newLowTime ? new SimpleDateFormat(CcdConstants.CcdDateTimePat).parse(formatDateTime(newLowTime)) : null
      def newHighTime = newObservation?.effectiveTime?.high[0]?.@value
      Date newHighDate = newHighTime ? new SimpleDateFormat(CcdConstants.CcdDateTimePat).parse(formatDateTime(newHighTime)) : null
      if (knownStatus == newStatus && knownLowDate.equals(newLowDate) && knownHighDate.equals(newHighDate)) {
        return true
      }
    }
    return false
  }

  /**
   * Aggregates encounter section
   * @param sectionCode component section code
   * @param targetEntries A + B, entries from both old and new
   * @param closuresMap closures to be called.e.g: onNewEntry, onKnownEntry, onUpdateEntry
   * @return aggregated entries
   */
  def mergeEncountersSection(String sectionCode, def oldEntries, def newEntries, Map closuresMap) {
    if (newEntries) {
      def gPathExpr = {entry -> entry[ns.encounter][ns.code]?.getAt(0) }
      def freeTextClosure = { entryCode -> entryCode?.@displayName}
      closuresMap.'onSpecialKey' = {entry, codeMap ->
        def nodeList = entry[ns.encounter]*.depthFirst()
        def testName = nodeList?.getAt(0)[ns.text][ns.reference]?.@value
        def conductedTime = nodeList?.getAt(0)[ns.effectiveTime]?.@value?.getAt(0)
        conductedTime = conductedTime ?: "${nodeList?.getAt(0)[ns.low]?.getAt(0)?.@value}-${nodeList?.getAt(0)[ns.high]?.getAt(0)?.@value}"
        "${testName}${conductedTime}"
      }
      return mergeComponentEntries(sectionCode, oldEntries, newEntries, 'LOINC', closuresMap, gPathExpr, freeTextClosure, false)
    }
    return oldEntries
  }

  /**
   * Get time range for medication administration
   * @param targetMedState known medication entry
   */
  def getMedicationTimeDuration(def targetMedState) {
    Map timeRange = [:]
    Date low = null
    Date high = null
    def ts = targetMedState.substanceAdministration.effectiveTime
    ts.each {
      String lowTime = it.low[0]?.@value
      String highTime = it.high[0]?.@value
      if (!low && lowTime) {
        low = new SimpleDateFormat(CcdConstants.CcdDateTimePat).parse(formatDateTime(lowTime))
      } else if (low) {
        Date newLowTime = new SimpleDateFormat(CcdConstants.CcdDateTimePat).parse(formatDateTime(lowTime))
        low = low.before(newLowTime) ? low : newLowTime
      }
      if (!high && highTime) {
        high = new SimpleDateFormat(CcdConstants.CcdDateTimePat).parse(formatDateTime(highTime))
      } else if (high) {
        Date newHighTime = new SimpleDateFormat(CcdConstants.CcdDateTimePat).parse(formatDateTime(highTime))
        high = high.after(newHighTime) ? high : newHighTime
      }
    }
    timeRange = [low: low, high: high]
    return timeRange
  }

  /**
   * Update medication quantity and duration of the course
   * @param targetMedState known medication
   * @param newMedState new medication
   */
  def updateTimeAndMedicationQty = {targetMedState, newMedState ->
    if (targetMedState && newMedState) {
      Map targetTimeRange = getMedicationTimeDuration(targetMedState)
      Map newTimeRange = getMedicationTimeDuration(newMedState)
      if (newTimeRange.low) {
        targetTimeRange.low = targetTimeRange.low?.before(newTimeRange.low) ? targetTimeRange.low : newTimeRange.low
      }
      if (newTimeRange.high) {
        targetTimeRange.high = targetTimeRange.high?.after(newTimeRange.high) ? targetTimeRange.high : newTimeRange.high
      }
      String targetDose = targetMedState.substanceAdministration.doseQuantity[0]?.@value
      String targetUnit = targetMedState.substanceAdministration.doseQuantity[0]?.@unit
      String newDose = newMedState.substanceAdministration.doseQuantity[0]?.@value
      String newUnit = newMedState.substanceAdministration.doseQuantity[0]?.@unit
      if (newDose && newDose ==~ /\d*/) {
        targetDose = targetDose && targetDose ==~ /\d*/ ? (targetDose.toInteger() + newDose.toInteger()).toString() : newDose
      }
      if (newUnit) {
        targetUnit = targetUnit ? targetUnit : newUnit
        if (targetUnit ==~ /\d*/ && newUnit ==~ /\d*/) {
          targetUnit = (targetUnit.toInteger() + newUnit.toInteger()).toString()
        }
      }
      if (targetDose) {
        targetMedState.substanceAdministration.doseQuantity[0]?.@value = targetDose
      }
      if (targetUnit) {
        targetMedState.substanceAdministration.doseQuantity[0]?.@unit = targetUnit
      }

    }
    return targetMedState
  }

  def getCodeSystemNameByOid(String oid) {
    return oid ? CodingSystem.getByOid(oid)?.name() : oid
  }

  def getAbbreviatedSystemName(String codeSystemName) {
    return codeSystemName ? CodingSystem.getBySystemName(codeSystemName)?.name() : null
  }

  /**
   * Apply all closures for known entries
   * @param closureMap
   * @param knownEntryMap [entry:entry, sectionCode:sectionCode]
   * @param newEntry
   */
  def callClosuresOnKnownEntry(Map closureMap, Map knownEntryMap, def newEntry) {
    Closure knownEntryClosure = closureMap.'onKnownEntry'
    callClosureAsync(knownEntryClosure, knownEntryMap.entry)
    Closure updateEntry = closureMap.'updateEntry'
    updateEntry?.call(knownEntryMap.entry, newEntry)
    if (updateEntry) {
      callClosureAsync(closureMap.'onUpdateEntry', knownEntryMap)
    }
  }

  /**
   * apply all closures for new entries
   * @param closureMap
   * @param newEntryMap [entry:entry, sectionCode:sectionCode]
   */
  def callClosuresOnNewEntry(Map closureMap, Map newEntryMap) {
    Closure newEntryClosure = closureMap.'onNewEntry'
    if(newEntryMap.entry){
      newEntryMap.section = newEntryMap.entry.parent()
    }
    try {
      newEntryClosure.call(newEntryMap)
    } catch (Exception e) {
      log.error("Failed to process section[${newEntryMap.sectionCode}] on new entry", e)
    }
  }

  /**
   * Aggregate entries by their code
   * @param sectionCode component section code
   * @param codeMap code to identify entry
   * @param currentEntry entry
   * @param entries entry map
   * @param vocabulary vocabulary properties for normalization
   * @param closureMap Map of closures onNewEntry, onKnownEntry, onUpdateEntry
   * @return entry map
   */
  def aggregateByGenericCode(String sectionCode, Map codeMap, def currentEntry, Map entries, Map vocabulary, Map closureMap) {
    def entryCode = codeMap?.code
    Map entryMap = [sectionCode: sectionCode]
    Closure specialKeyClosure = closureMap.'onSpecialKey'
    entryCode = specialKeyClosure ? specialKeyClosure(currentEntry, codeMap) : entryCode
    if (entries."${entryCode}") {
      def known = entries."${entryCode}"
      entryMap.entry = known
      callClosuresOnKnownEntry(closureMap, entryMap, currentEntry)
    } else {
      entries."${entryCode}" = currentEntry
      entryMap.entry = currentEntry
      callClosuresOnNewEntry(closureMap, entryMap)
      def source = getAbbreviatedSystemName(codeMap?.name)
      source = source ?: getCodeSystemNameByOid(codeMap?.oid)
      vocabulary."${entryCode}" = [code: codeMap?.code, source: source, target: codeMap?.target]
    }
  }

  /**
   * Prepares hash map from the component entries
   * @param entries component entries
   * @param componentProperties ; Map of component properties
   * [sectionCode:sectionCode, gPathExpr:gPathExpr, target:target, freeText:freeTextClosure
   * @return entryMap
   */
  Map prepareComponentEntryMap(sectionCode, entries, target, Map closuresMap, gPathExpr, freeTextClosure, Map entryMap, Map vocabulary) {
    entries.each { entry ->
      def entryCodeMap = gPathExpr?.call(entry)
      def entryCode = entryCodeMap?.@code
      if (entryCode && entryCode != CcdConstants.Unknown) {
        Map codeMap = [code: entryCodeMap?.@code, name: entryCodeMap?.@codeSystemName, oid: entryCodeMap?.@codeSystem,
            target: target, displayName: entryCodeMap?.displayName]
        aggregateByGenericCode(sectionCode, codeMap, entry, entryMap, vocabulary, closuresMap)
      } else {
        String freeText = freeTextClosure?.call(entryCodeMap)
        entryMap = aggregateByFreeText(sectionCode, freeText, entry, entryMap, closuresMap)
      }
    }
    return entryMap
  }

  /**
   * merge component entries. Basically perform A U B on sets
   * sectionCode component section code
   * entries all component entries A + B
   * targetCodeSystem target code system of entry for normalization
   * closuresMap closures for onNewEntry, onKnownEntry, onUpdateEntry
   * gPathExpr closure to get desired information. for instance code
   * normalize true/false
   */
  def mergeComponentEntries = {sectionCode, oldEntries, newEntries, target, Map closuresMap, gPathExpr, freeTextClosure, normalize = false, Map entryMap = [:] ->
    Map vocabulary = [:]
    prepareComponentEntryMap(sectionCode, oldEntries, target, ['onFallBackKey': closuresMap.'onFallBackKey', 'onSpecialKey': closuresMap.'onSpecialKey'],
        gPathExpr, freeTextClosure, entryMap, vocabulary)
    prepareComponentEntryMap(sectionCode, newEntries, target, closuresMap, gPathExpr, freeTextClosure, entryMap, vocabulary)
    if (normalize && currentFacility?.normalizationEnabled) {
      List semanticCodes = fetchSemanticCodes(sectionCode, vocabulary.values().toList())
      normalizeComponent(semanticCodes, vocabulary, entryMap, gPathExpr)
    }
    closuresMap.'onSpecialKey' = null
    return entryMap.values().toList()
  }

//  List fetchSemanticCodes(String sectionCode, List originalCodes) {
//    List semanticCodes = []
//    if (originalCodes) {
//      String remoteFacilityUuid = currentFacility?.remoteVocabularyServer ?: null
//      if (remoteFacilityUuid) {
////        RemoteFacility remoteFacility = RemoteFacility.findByFacilityAndUuidAndStatusAndDeleted(currentFacility, remoteFacilityUuid, RemoteFacility.Status.Online, false)
//        semanticCodes = fetchSemanticCodesFromPeer(sectionCode, remoteFacility, originalCodes)
//      } else {
//        log.info("Normalizing ccd section:${sectionCode} using ${currentFacility?.nickName} apelon dts server:${localApelonServer}")
//        semanticCodes = terminologyClientService?.getSemanticCodes(originalCodes, localApelonServer)
//      }
//    }
//    return semanticCodes
//  }


//  List fetchSemanticCodesFromPeer(String sectionCode, RemoteFacility remoteFacility, List actualCodes) {
//    if (remoteFacility) {
//      log.info("Normalizing ccd section:${sectionCode} using ${remoteFacility.name} apelon dts server")
//      final f = [pending: [], semanticCodes: []]
//      String uuid = StringUtils.randomUuid()
//      String queryJson = new JsonBuilder(actualCodes).toString()
//      def msg = Message.generate(facility: currentFacility, toFacility: remoteFacility, state: State.Outboxed,
//          type: Message.Type.Normalize, subType: 'get', uuid: uuid, queryJson.bytes).save(flush: true)
//      def outboxContext = [id: msg.id]
//      f.pending << msg.id
//      def closure = new ResponseHandler(f: f, eventHandlerService: eventHandlerService, msg: outboxContext).closure
//      eventHandlerService.subscribe("message.acked.${msg.id}", closure)
//      log.debug "Request prepared"
//      eventHandlerService.fireSync("message.outboxed", outboxContext)
//      log.debug "Request sent"
//
//      log.debug "Started wait"
//      //TODO: Identify better way of doing blocking call. The below method lead us nowhere
//      for (int i = 0; i < 50; i++) {
//        Thread.sleep(500)
//        log.debug f.pending
//        if (!f.pending || f.semanticCodes) {
//          break
//        }
//      }
//      if (f.pending && !f.semanticCodes) {
//        log.warn "Remotes could not find it either"
//      }
//      log.debug("Section ${sectionCode}, before normalization :${queryJson}")
//      log.debug("Section ${sectionCode}, after normalization :${f.semanticCodes}")
//      return f.semanticCodes
//    } else {
//      log.error("Unable fetch semantic codes from peer:${remoteFacility}")
//    }
//
//    []
//  }

  private class ResponseHandler {
    Map f
    Map msg
    def eventHandlerService

    def closure = { context ->
      log.debug "Got an update for ${msg.id}"
      def foo = new JsonSlurper().parseText(context.messageContent)
      if (foo.status == 'Success') {
        log.debug "Someone got it"
        f.semanticCodes = foo.semanticCodes
      } else {
        log.debug "Someone did not get it"
      }
      f.pending.remove(msg.id)
      eventHandlerService.removeSubscribers("message.acked.${msg.id}")
    }
  }

  def prepareInPlaceComponentEntryMap(sectionCode, entries, targetCodeSystem, Map gPathClosures, Map closuresMap, Map cachedData) {
    Map entryMap = cachedData.entryMap
    Map vocabulary = cachedData.vocabulary
    Map eventToId = cachedData.eventToId
    Map entryProperties = [sectionCode: sectionCode]
    entries.each { entry ->
      def observationComponents = gPathClosures.componentsClosure?.call(entry)
      observationComponents.each {observationComponent ->
        def observation = observationComponent[ns.observation]?.getAt(0)
        def observationCode = gPathClosures.codeClosure?.call(observationComponent)
        def observationId = observationCode?.@code
        def observationTime = gPathClosures.timeClosure?.call(observationComponent)
        def formattedDateTime = formatDateTime(observationTime)
        def observationTimeStr = formattedDateTime ? new SimpleDateFormat(CcdConstants.CcdDateTimePat)?.parse(formattedDateTime)?.format(CcdConstants.CcdDateTimePat) : null
        def observationEvent = gPathClosures.eventClosure?.call(observationComponent, observationId, observationTimeStr)
        if (observationEvent) {
          if (entryMap."${observationEvent}") {
            observation?.parent()?.remove(observationComponent)
            if (!observation) {
              entryMap."${observationEvent}" = null
            }
            entryProperties.entry = entryMap."${observationEvent}"
            entryProperties.observation = observation
            callClosuresOnKnownEntry(closuresMap, entryProperties, entry)
          } else {
            def source = getAbbreviatedSystemName(observationCode?.@codeSystemName)
            source = source ?: getCodeSystemNameByOid(observationCode?.@codeSystem)
            entryMap."${observationEvent}" = entry
            entryProperties.entry = entry
            entryProperties.observation = observation
            String vocabularyKey = gPathClosures.keyClosure?.call(observationComponent)
            vocabulary."${vocabularyKey}" = [code: observationCode?.@code, codeSystem: observationCode?.@codeSystem,
                source: source, target: targetCodeSystem]
            eventToId."${vocabularyKey}" = observationId
            callClosuresOnNewEntry(closuresMap, entryProperties)
          }
        } else {
          entryMap."${UUID.randomUUID()}" = entry
          entryProperties.entrySet = entry
          entryProperties.observation = observation
          callClosuresOnNewEntry(closuresMap, entryProperties)
        }
      }
    }
  }

  /**
   * merge component entries in place. Basically perform A U B on sets
   * sectionCode component section code
   * entries all component entries A + B
   * targetCodeSystem target code system of entry for normalization
   * closuresMap closures for onNewEntry, onKnownEntry, onUpdateEntry
   * normalizeClosure closure to normalize
   */
  def mergeComponentEntriesByInPlace = {sectionCode, oldEntries, newEntries, target, Map gPathClosures, Map closuresMap, normalizeClosure ->
    Map entryMap = [:]
    Map vocabulary = [:]
    Map eventToId = [:]
    Map cachedEntries = [entryMap: entryMap, vocabulary: vocabulary, eventToId: eventToId]
    prepareInPlaceComponentEntryMap(sectionCode, oldEntries, target, gPathClosures, [:], cachedEntries)
    prepareInPlaceComponentEntryMap(sectionCode, newEntries, target, gPathClosures, closuresMap, cachedEntries)
    if (normalizeClosure && currentFacility?.normalizationEnabled) {
      List semanticCodes = fetchSemanticCodes(sectionCode, vocabulary.values().toList())
      normalizeClosure(semanticCodes, vocabulary, entryMap, eventToId)
    }
    return entryMap.values().toList().collectAll {it}.unique()
  }


  /**
   * Normalize component by replacing entry code information from terminology server
   * semanticCodes target entry codes
   * vocabularyMap map of entries to be normalized
   * entries map of entries
   * gPathExpr closure to get desired code for normalization
   */
  def normalizeComponent = { semanticCodes, vocabularyMap, entries, gPathExpr ->
    if (semanticCodes && vocabularyMap && entries) {
      List vocabulary = vocabularyMap.values().toList()
      if (semanticCodes.size() == vocabulary.size()) {
        vocabulary.eachWithIndex {entry, idx ->
          if (semanticCodes[idx]) {
            def knownEntry = entries."${entry.code}"
            def knownEntryCode = gPathExpr(knownEntry)
            knownEntryCode.@code = semanticCodes[idx].code ?: knownEntryCode.@code
            knownEntryCode.@displayName = semanticCodes[idx].name ?: knownEntryCode.@displayName
            knownEntryCode.@codeSystem = CcdConstants.SnoMedCodeSystem
            knownEntryCode.@codeSystemName = CcdConstants.SnoMedCodeSystem
          }
        }
      }
    }
  }

  /**
   * Aggregate entries by display name
   * @param sectionCode component section code
   * @param freeText display name
   * @param currentEntry component entry
   * @param sectionEntries known entries
   * @param closureMap map of closures, onNewEntry, onKnownEntry, onUpdateEntry
   * @return entry map
   */
  def aggregateByFreeText(String sectionCode, def freeText, def currentEntry, Map sectionEntries, Map closureMap) {
    Closure newEntryClosure = closureMap.'onNewEntry'
    Map entryMap = [sectionCode: sectionCode]
    freeText = freeText ?: closureMap.'onFallBackKey'?.call(currentEntry, [:])
    if (freeText) {
      if (sectionEntries."${freeText}") {
        entryMap.entry = sectionEntries."${freeText}"
        callClosuresOnKnownEntry(closureMap, entryMap, currentEntry)
      } else {
        sectionEntries."${freeText}" = currentEntry
        entryMap.entry = currentEntry
        callClosuresOnNewEntry(closureMap, entryMap)
      }
    } else {
      sectionEntries."${UUID.randomUUID()}" = currentEntry
      entryMap.entry = currentEntry
      callClosuresOnNewEntry(closureMap, entryMap)
    }
    return sectionEntries
  }

  /**
   * Update the high/low time of a disease
   * @param targetProblemState known problem entry
   * @param newProblemState new problem entry
   */
  def updateProblemDuration = {  targetProblemState, newProblemState ->
    if (targetProblemState && newProblemState) {
      String targetLowTime = targetProblemState[ns.act][ns.entryRelationship]?.getAt(0)[ns.observation][ns.effectiveTime][ns.low]?.@value?.getAt(0)
      String targetHighTime = targetProblemState[ns.act][ns.entryRelationship]?.getAt(0)[ns.observation][ns.effectiveTime][ns.high]?.@value?.getAt(0)
      String newLowTime = newProblemState[ns.act][ns.entryRelationship]?.getAt(0)[ns.observation][ns.effectiveTime][ns.low]?.@value?.getAt(0)
      String newHighTime = newProblemState[ns.act][ns.entryRelationship]?.getAt(0)[ns.observation][ns.effectiveTime][ns.high]?.@value?.getAt(0)
      int datePatLength = CcdConstants.CcdDatePat.length()
      if (targetLowTime && newLowTime) {
        if (targetLowTime.length() < datePatLength) {
          (datePatLength - targetLowTime.length()).times {targetLowTime += '0'}
        }
        if (newLowTime.length() < datePatLength) {
          (datePatLength - newLowTime.length()).times {newLowTime += '0'}
        }
        Date targetLowDate = new SimpleDateFormat(CcdConstants.CcdDatePat).parse(targetLowTime)
        Date newLowDate = new SimpleDateFormat(CcdConstants.CcdDatePat).parse(newLowTime)
        if (newLowDate.before(targetLowDate)) {
          targetProblemState[ns.act][ns.entryRelationship]?.getAt(0)[ns.observation][ns.effectiveTime][ns.low]?.getAt(0)?.@value = newLowDate.format(CcdConstants.CcdDatePat)
        }
      }
      if (targetHighTime && newHighTime) {
        if (targetHighTime.length() < datePatLength) {
          (datePatLength - targetHighTime.length()).times {targetHighTime += '0'}
        }
        if (newHighTime.length() < datePatLength) {
          (datePatLength - newHighTime.length()).times {newHighTime += '0'}
        }
        Date targetHighDate = new SimpleDateFormat(CcdConstants.CcdDatePat).parse(targetHighTime)
        Date newHighDate = new SimpleDateFormat(CcdConstants.CcdDatePat).parse(newHighTime)
        if (newHighDate.after(targetHighDate)) {
          targetProblemState[ns.act][ns.entryRelationship]?.getAt(0)[ns.observation][ns.effectiveTime][ns.high]?.getAt(0)?.@value = newHighDate.format(CcdConstants.CcdDatePat)
        }
      }
    }
    return targetProblemState
  }

  /**
   * Merges plan of care sections
   * @param targetEntries
   * @return
   */
  def mergePlanOfCare(def targetEntries) {
    return targetEntries
  }

  /**
   * Merge function status sections
   * @param targetEntries
   */
  def mergeFunctionalStatusCode(def targetEntries) {
    return targetEntries
  }
}
