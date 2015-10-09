package com.cds.hiro.ccd

import groovy.json.JsonSlurper
import groovy.util.logging.Log4j

/**
 * An utility class to aid streaming ccd component entries
 * Author: Sivanarayana Gaddam
 */
@Log4j
class CcdStreamingUtil {
  List<Map> events = []
  def brokerMessagingTemplate
  String originalRequestId

  /**
   * Stream ccd entries
   * @param aggregator Instance of CcdAggregator
   * @param ccdDocuments List of strings, each representing a ccd document
   */
  void streamCcdEntries(CcdAggregator aggregator, List<String> ccdDocuments) {
    if (aggregator && ccdDocuments) {
      try {
        ccdDocuments.each { String ccdString ->
          if (ccdString) {
            def newCcd = new XmlParser().parseText(ccdString)
            aggregator.closuresMap = [onNewEntry: onNewEntry]
            log.debug "Calling update"
            aggregator.updateCcd(newCcd)
            log.debug "Update called"
          } else {
            log.warn "Received empty ccd."
          }
        }
      } catch (Exception e) {
        log.warn("Unable to stream ccd data", e)
      }
    } else {
      log.warn("Unable to stream ccd entries. Aggregator: ${aggregator}. CCDs: ${ccdDocuments.size()}")
    }
  }

  /**
   * Closure to be called on new entry
   */
  def onNewEntry = { aggregated, sourceData = null ->
    onNewEntrySynchronized(aggregated, sourceData)
  }

  private synchronized void onNewEntrySynchronized(aggregated, sourceData) {
    def vHelper = new CcdVisualizationHelper()
    def helpers = [
        '10160-0': [data: {vHelper.getMedication(it.entry, it.section)}, section: 'medications'],
        '30954-2': [data: {vHelper.getObservation(it.observation)}, section: 'labResults'],
        '8716-3' : [data: {vHelper.getObservation(it.observation)}, section: 'vitalSigns'],
        '48764-5': [data: {vHelper.getSummaryPurpose(it.entry)}, section: 'summaryPurpose'],
        '46240-8': [data: {vHelper.getVisitSummary(it.entry, it.section)}, section: 'visitSummary'],
        '11348-0': [data: {vHelper.getPastMedical(it.entry, it.section)}, section: 'history.pastMedical'],
        '11450-4': [data: {vHelper.getProblem(it.entry, it.section)}, section: 'problems'],
        '10157-6': [data: {vHelper.getFamilyHistory(it.entry)}, section: 'history.family'],
        '29762-2': [data: {vHelper.getSocialHistory(it.entry, it.section)}, section: 'history.social'],
        '11369-6': [data: {vHelper.getImmunizationHistory(it.entry)}, section: 'history.immunization'],
        '48765-2': [data: {vHelper.getAlert(it.entry, it.section)}, section: 'self.allergies'],
        '18776-5': [data: {vHelper.getPlanOfCare(it.entry, it.section)}, section: 'planOfCare'],
        '11535-2': [data: {vHelper.getDischargeDiagnosis(it.entry, it.section)}, section: 'hospitalDischargeDiagnosis'],
        '10183-2': [data: {vHelper.getDischargeMedications(it.entry, it.section)}, section: 'hospitalDischargeMedications'],
        '29545-1': [data: {vHelper.getPhysicalExamination(it.entry, it.section)}, section: 'physicalExamination'],
        '47519-4': [data: {vHelper.getProcedures(it.entry, it.section)}, section: 'procedures'],
        '51848-0': [data: {vHelper.getAssessment(it.entry.observation)}, section: 'assessments'],
    ]
    def helper = helpers[aggregated.sectionCode]
    if (helper) {
      def dataJsonString = helper.data.call(aggregated)
      log.debug("Streaming entry from section:${aggregated.sectionCode} target uuid: ${originalRequestId} and json:${dataJsonString}")
      def map = new JsonSlurper().parseText(dataJsonString)
      def jsonMap = [data: map, section: helper.section]
      events << jsonMap
      log.debug "Broadcast complete. ${jsonMap}"
    } else {
      log.warn "Could not stream ${aggregated.sectionCode}"
    }
  }
}
