package com.cds.hiro.ccd

import groovy.util.logging.Log4j

/**
 * Prepare HTML version of CCD component
 * Author: SGaddam
 */
@Log4j
class CcdComponentToHtmlUtil {
  String custodian
  String patient

  /**
   * Map of closures for each component.
   */
  final Map componentMap = ["${CcdConstants.ProblemsCode}": ['Condition': {it*.value*.@displayName*.get(0)},
      'Effective Dates': {it*.effectiveTime.collect {it.low[0]?.@value}},
      'Condition Status': {it*.value*.@displayName*.get(1)}],

      "${CcdConstants.ProceduresCode}": ['Procedure': {it*.code*.@displayName},
          'Date': {it*.effectiveTime.collect {it.@value ?: "${it.low[0]?.@value}-${it.high[0]?.@value}"}}],

      "${CcdConstants.MedicationsCode}": ['Medication': {it*.manufacturedMaterial*.code*.@displayName},
          'Instructions': {it*.administrationUnitCode*.@displayName},
          'Start Date': {it*.effectiveTime.collect {"${it.low[0]?.@value}-${it.high[0]?.@value}"}},
          'Status': {it*.statusCode*.@code}],

      "${CcdConstants.PayersCode}": ['Payer Name': {it*.representedOrganization.collect {it.name[0].text()}},
          'Policy Type': {it*.participantRole*.code*.@displayName}, 'Policy ID': {it*.participantRole*.id*.@extension},
          'Policy Holder': {nodes -> patient}],

      "${CcdConstants.AdvanceDirectivesCode}": ['Directive': {it*.code*.@displayName.get(0)},
          'Description': {it*.value*.@displayName}, 'Custodian': {nodes -> custodian},
          'Supporting Document(s)': {
            it*.externalDocument.collect {
              "<linkHtml href=${it.text.reference.@value}>${it.code.@displayName}</linkHtml>"
            }
          }],

      "${CcdConstants.FunctionalStatusCode}": ['Functional Condition': {it*.value*.@displayName.get(0)},
          'Effective Dates': {it*.effectiveTime.collect {"${it.low[0]?.@value}-${it.high[0]?.@value}"}},
          'Condition Status': {it*.value*.@displayName.get(1)}],

      "${CcdConstants.FamilyHistoryCode}": ['Family Member': {it*.relatedSubject*.code*.@displayName},
          'Medical History': {it*.value*.text()}],

      "${CcdConstants.SocialHistoryCode}": ['Social History Element': {
        it*.observation*.code.collect {
          it[0].@displayName ?: it.originalText.text()
        }
      }, 'Description': {it*.observation*.value*.text()}],

      "${CcdConstants.AllergyCode}": ['Substance': {it*.playingEntity*.code*.@displayName},
          'Reaction': {it*.findAll {it.@typeCode == 'MFST'}*.observation.value.collect {it[0].@displayName[0]}},
          'Status': {it*.findAll {it.@typeCode == 'REFR'}*.observation.value.collect {it[0].@displayName[0]}}],

      "${CcdConstants.MedicalEquipmentCode}": ['Supply/Device': {it*.playingDevice*.code*.@displayName},
          'Date Supplied': {it*.effectiveTime.collect {it.center.@value ?: "${it.low[0]?.@value}-${it.high[0]?.@value}"}}],

      "${CcdConstants.ImmunizationsCode}": ['Vaccine': {it*.manufacturedMaterial*.code*.@displayName},
          'Date': {it*.effectiveTime.collect {it.center.@value ?: "${it.low[0]?.@value}-${it.high[0]?.@value}"}},
          'Status': {it*.statusCode*.@code}],

      "${CcdConstants.ResultsCode}": ['Date': {
        it.findAll {it.component}*.observation[0].effectiveTime.collect {
          it.@value ?: "${it.low[0]?.@value}-${it.high[0]?.@value}"
        }
      }, 'Description': {it.findAll {it.component}*.observation[0].code.@displayName},
          'Test Name': {it.findAll {it.component}*.observation[0].code.@displayName},
          'Value': {it.findAll {it.component}*.observation[0].value.collect {"${it.@value}${it.@unit}"}},
          'Reference': {it.findAll {it.component}*.observation[0].referenceRange.observationRange.text*.text()},
          'Interpretation': {it.findAll {it.component}*.observation[0].interpretationCode*.@code},
          'Status': {it.findAll {it.component}*.observation[0].statusCode*.@code}],

      "${CcdConstants.EncountersCode}": ['Encounter': {it*.code.originalText.reference[0].@value},
          'Performer': {it*.assignedPerson.name[0]*.depthFirst().collect {it.text()}},
          'Location': {it*.ParticipantRole*.playingEntity*.name*.text()},
          'Date': { it*.effectiveTime.collect {it[0].@value ?: "${it.low[0]?.@value}-${it.high[0]?.@value}"}}],

      "${CcdConstants.PlanOfCareCode}": ['Planned Activity': {it*.code[0]*.@displayName},
          'Planned Date': {it*.effectiveTime.collect {it.center[0].@value ?: "${it.low[0]?.@value}-${it.high[0]?.@value}"}}],

      "${CcdConstants.VitalSignsCode}": ['Date/Time': {it*.observation*.code.collect {it.@displayName}.flatten().unique()}]]

  /**
   * Add html version of component to ccd component
   * @param sectionCode component section code
   * @param componentSection individual ccd component
   */
  void addHtmlTextToComponent(def sectionCode, def componentSection) {
    if (sectionCode && componentSection) {
      Map tableMap = componentMap.get("${sectionCode}")
      if (tableMap) {
        def whiteList = componentSection.entry*.depthFirst()
        Map tableBodyMap = tableMap.collectEntries { k, v ->
          ["${k}": v?.call(whiteList)]
        }
        if (sectionCode == CcdConstants.VitalSignsCode) {
          whiteList*.effectiveTime*.@value.flatten().unique().each {observedTime ->
            tableBodyMap."${observedTime}" = []
            tableBodyMap.get("${'Date/Time'}").each {observation ->
              def obs = whiteList*.observation*.find {
                it.effectiveTime[0].@value == observedTime && it.code[0].@displayName == observation
              }
              tableBodyMap."${observedTime}" << "${obs?.value[0]?.@value?.getAt(0)}${obs?.value[0]?.@unit?.getAt(0)}"
            }
          }
        }
        addHtmlTableToText(tableBodyMap, sectionCode, componentSection)
      } else {
        log.warn("Unsupported CDA/CCD component is encountered.${sectionCode}")
      }
    }
  }

  /**
   * Add html equivalent of component to aggregated ccd
   * @param tableBodyMap map of entry header and their values
   * @param sectionCode component section code
   * @param componentSection ccd component
   */
  void addHtmlTableToText(Map tableBodyMap, String sectionCode, def componentSection) {
    if (tableBodyMap && componentSection) {
      List headers = tableBodyMap.keySet().toList()
      int rowCount = tableBodyMap.get(headers[0]).size()
      Node textNode = new Node(componentSection, 'text')
      Node tableNode = new Node(textNode, 'table', ['border': '1', 'width': '100%'])
      Node tHead = new Node(tableNode, 'thead')
      Node tBody = new Node(tableNode, 'tbody')
      Node tr = new Node(tHead, 'tr')
      headers.each {
        new Node(tr, 'th', it)
      }
      List rows = tableBodyMap.values().toList()
      rowCount.times {idx ->
        Node tableRow = new Node(tBody, 'tr')
        rows.each {
          new Node(tableRow, 'td', it ? it[idx] : null)
        }
      }
    } else {
      log.warn("Unable to add HTML translation of component:${sectionCode}")
    }
  }

}
