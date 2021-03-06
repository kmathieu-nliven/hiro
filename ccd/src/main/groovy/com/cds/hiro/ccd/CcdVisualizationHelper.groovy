package com.cds.hiro.ccd

import groovy.json.JsonBuilder
import groovy.util.logging.Log4j
import groovy.xml.Namespace
import groovy.xml.QName

import java.text.SimpleDateFormat

/**
 * Helper methods for CCD Visualization.
 * @author rahulsomasunderam
 * @since 11/8/11 7:57 AM
 */
@Log4j
class CcdVisualizationHelper {

  private static final def xsiType = new groovy.xml.QName('http://www.w3.org/2001/XMLSchema-instance', 'type')
  private static final def ns = new Namespace('urn:hl7-org:v3')

  void renderText(Node node, StringWriter sb) {
    node?.children()?.each {
      if (it instanceof String) {
        sb.append it.replaceAll('\\\\n', '<br/>\n')
      } else {
        renderText(it, sb)
        sb.append(' ')
      }
    }
  }

  String getMedicationFrequency(section, entry) {
    def administration = entry?.getAt(ns.substanceAdministration)
    section = section ?: entry?.parent()
    if (administration?.getAt(ns.doseQuantity)) {
      def dq = administration?.getAt(ns.doseQuantity)?.@value?.getAt(0)
      def unit = administration?.getAt(ns.administrationUnitCode)?.@displayName?.getAt(0)

      def freq = administration?.getAt(ns.effectiveTime)?.find {et ->
        et.attributes().find { it.key == xsiType }?.value =~ 'PIVL_TS'
      }?.period?.with { p ->
        p.@value[0] + ' ' + p.@unit[0]
      }

      if (dq && unit && freq) {
        return "$dq $unit every $freq"
      }
    }

    if (administration?.getAt(ns.text)?.getAt(ns.reference)?.@value?.getAt(0)) {
      return findTextById(section, administration?.getAt(ns.text)?.getAt(ns.reference)?.@value?.getAt(0))
    }

    return ''
  }

  def String findTextById(section, String id) {
    if (!id) {return null}

    StringWriter sw = new StringWriter()
    def referencedElement = section?.depthFirst()?.find { it instanceof Node && it.@ID == id[1..-1] }
    if (referencedElement) {
      renderText(referencedElement, sw)
      sw.toString()
    } else {
      null
    }
  }

  @Deprecated String plotEncounters(section) {
    StringBuilder sb = new StringBuilder()
    section?.getAt(ns.entry)?.each { entry ->
      sb.append "events.push({"
      def encounter = entry?.getAt(ns.encounter)
      def props = printEffectiveDateForJson(encounter?.getAt(ns.effectiveTime)?.getAt(0))
      props << "classification: 'Encounter'"
      def encounterCode = encounter?.getAt(ns.code)
      def assignedPerson = encounter?.getAt(ns.performer)?.getAt(ns.assignedEntity)?.getAt(ns.assignedPerson)
      def originalText = encounterCode?.getAt(ns.originalText)?.text() ?: findTextById(section,
          encounterCode?.getAt(ns.originalText)?.getAt(ns.reference)?.@value?.getAt(0))
      def personName = assignedPerson?.getAt(ns.name)
      def description = personName?.text() ?: (personName?.getAt(ns.given)?.text() + ' ' + personName?.getAt(ns.family)?.text())
      props << "  text: '${originalText?.replaceAll("'", "\\\\'")}, ${description?.replaceAll("'", "\\\\'")}'"
      props << "  description: '${description?.replaceAll("'", "\\\\'")}'"
      sb.append props.join(", ")
      sb.append "});\n"
    }
    getNonNullString(sb)
  }


  private def printEffectiveDate(Node effectiveDate) {
    if (!effectiveDate)
      return null

    if (effectiveDate.@value) {
      parseDate effectiveDate?.@value
    } else if (effectiveDate?.getAt(ns.center)?.@value?.getAt(0)) {
      parseDate(effectiveDate?.getAt(ns.center)?.@value?.getAt(0))
    } else if (effectiveDate?.getAt(ns.low)?.@value?.getAt(0) && effectiveDate?.getAt(ns.high)?.@value?.getAt(0)) {
      [start: parseDate(effectiveDate?.getAt(ns.low)?.@value?.getAt(0)),
          stop: parseDate(effectiveDate?.getAt(ns.high)?.@value?.getAt(0))]
    } else if (effectiveDate?.getAt(ns.low)?.@value?.getAt(0)) {
      [start: parseDate(effectiveDate?.getAt(ns.low)?.@value?.getAt(0)) ]
    } else if (effectiveDate?.getAt(ns.high)?.@value?.getAt(0)) {
      [stop: parseDate(effectiveDate?.getAt(ns.high)?.@value?.getAt(0))]
    } else {
      null
    }
  }

  /**
   * Generates dates for json
   * @param effectiveDate the effective date
   * @deprecated Use {@link #printEffectiveDate} and decorate it instead
   * @return a map for json usage
   */
  @Deprecated
  def printEffectiveDateForJson(effectiveDate) {
    if (effectiveDate) {
      if (effectiveDate.@value) {
        ["time:" + reprintDateForJson(effectiveDate?.@value?.getAt(0))]
      } else if (effectiveDate?.getAt(ns.center)?.@value?.getAt(0)) {
        ["time:" + reprintDateForJson(effectiveDate?.getAt(ns.center)?.@value?.getAt(0))]
      } else if (effectiveDate?.getAt(ns.low)?.@value?.getAt(0) && effectiveDate?.getAt(ns.high)?.@value?.getAt(0)) {
        ["start:" + reprintDateForJson(effectiveDate?.getAt(ns.low)?.@value?.getAt(0)),
            "end:" + reprintDateForJson(effectiveDate?.getAt(ns.high)?.@value?.getAt(0))]
      } else if (effectiveDate?.getAt(ns.low)?.@value?.getAt(0)) {
        ["start:" + reprintDateForJson(effectiveDate?.getAt(ns.low)?.@value?.getAt(0))]
      } else if (effectiveDate?.getAt(ns.high)?.@value?.getAt(0)) {
        ["end:" + reprintDateForJson(effectiveDate?.getAt(ns.high)?.@value?.getAt(0))]
      } else {
        []
      }
    } else {
      []
    }
  }

  @Deprecated String plotFunctionalStatus(section) {
    StringBuilder sb = new StringBuilder()
    section?.getAt(ns.entry)?.each { entry ->
      sb.append "events.push({"
      def observation = entry?.getAt(ns.act)?.getAt(ns.entryRelationship)?.getAt(ns.observation)
      def props = printEffectiveDateForJson(observation?.getAt(ns.effectiveTime)?.getAt(0))
      props << "classification: '${section?.getAt(ns.title)?.text()}'"
      props << "  text: '${observation?.getAt(ns.value)?.@displayName?.getAt(0) ?: observation?.getAt(ns.value)?.getAt(ns.translation)?.@displayName?.getAt(0)?.replaceAll("'", "\\\\'") ?: ''}'"
      sb.append props.join(", ")
      sb.append "});\n"
    }
    getNonNullString(sb)
  }

  @Deprecated void plotVitalSigns(section) {
    StringBuilder sb = new StringBuilder()

    try {
      def organizer = section?.getAt(ns.entry)?.getAt(ns.organizer)
      def dates = organizer?.getAt(ns.effectiveTime)?.@value?.sort()
      def signs = organizer?.getAt(ns.component)?.getAt(ns.observation)?.getAt(ns.code)?.collect { c ->
        [c.@code, c.@codeSystem, c.@displayName]
      } as Set

      signs?.each { row ->
        if (row) {
          dates.each { dt ->
            if (dt != null && getValue(section, dt, row)) {
              sb.append "events.push({"
              def props = ['time: ' + reprintDateForJson(dt)]
              props << "classification: 'Vital Signs'"
              props << "  text: '${row[2]?.replaceAll("'", "\\\\'")}'"
              props << "  value: '${getValue(section, dt, row)?.replaceAll("'", "\\\\'")}'"
              sb.append props.join(", ")
              sb.append "});\n"
            }
          }
        }
      }
    } catch (Exception e) {
      sb = null
      e.printStackTrace()
    }
    getNonNullString(sb)
  }

  @Deprecated String plotResults(section) {
    StringBuilder sb = new StringBuilder()
    def organizer = section?.getAt(ns.entry)?.getAt(ns.organizer)
    def dates = organizer?.getAt(ns.effectiveTime)?.@value
    def signs = organizer?.getAt(ns.component)?.getAt(ns.observation)?.getAt(ns.code)?.collect { c ->
      [c?.@code, c?.@codeSystem, c?.@displayName]
    } as Set

    signs.each { row ->
      dates.each { dt ->
        if (getValue(section, dt, row)) {
          sb.append "events.push({"
          def props = ['time: ' + reprintDateForJson(dt)]
          props << "classification: 'Results'"
          props << "  text: '${row[2]?.replaceAll("'", "\\\\'")}'"
          props << "  value: '${getValue(section, dt, row)?.replaceAll("'", "\\\\'")}'"
          sb.append props.join(", ")
          sb.append "});\n"
        }
      }
    }
    getNonNullString(sb)
  }

  private String getValue(section, dt, row) {
    if (!section) {
      return null
    }
    def val = section?.getAt(ns.entry)?.getAt(ns.organizer)?.find {o ->
      o?.getAt(ns.effectiveTime)?.@value?.getAt(0) == dt
    }?.getAt(ns.component)?.getAt(ns.observation)?.find {o ->
      o?.getAt(ns.code)?.@code?.getAt(0) == row[0] && o?.getAt(ns.code)?.@codeSystem?.getAt(0) == row[1]
    }?.getAt(ns.value)

    if (!val || !val[0]) {
      return ''
    }
    if (val[0].@value && val[0].@unit) {
      return val[0].@value + ' ' + val[0].@unit
    } else if (val[0].@value) {
      return val[0].@value
    } else if (val[0].text()) {
      return val[0].text()
    } else {
      return ''
    }

  }

  @Deprecated String plotImmunizations(section) {
    StringBuilder sb = new StringBuilder()
    section?.getAt(ns.entry)?.each { entry ->
      sb.append "events.push({"
      def substanceAdministration = entry?.getAt(ns.substanceAdministration)
      def props = printEffectiveDateForJson(substanceAdministration?.getAt(ns.effectiveTime))
      props << "classification: '${section?.getAt(ns.title)?.text()?.replaceAll("'", "\\\\'")}'"
      def product = substanceAdministration?.getAt(ns.consumable)?.getAt(ns.manufacturedProduct)
      props << "  text: '${product?.getAt(ns.manufacturedMaterial)?.getAt(ns.code)?.@displayName?.getAt(0)?.replaceAll("'", "\\\\'")}'"
      sb.append props.join(", ")
      sb.append "});\n"
    }
    getNonNullString(sb)
  }

  @Deprecated String plotSocialHistory(section) {
    StringBuilder sb = new StringBuilder()
    try {
      if (section?.getAt(ns.entry)) {
        section?.getAt(ns.entry)?.each { entry ->
          if (entry) {
            sb.append "events.push({"
            def observation = entry?.getAt(ns.observation)
            def props = printEffectiveDateForJson(observation?.getAt(ns.effectiveTime)?.getAt(0))
            props << "classification: 'Social History'"
            def observation2 = observation?.getAt(ns.entryRelationship)?.getAt(ns.observation)
            props << "  text: '${observation?.getAt(ns.code)?.@displayName?.getAt(0) ?: observation2?.getAt(ns.value)?.getAt(ns.translation)?.@displayName?.getAt(0)?.replaceAll("'", "\\\\'")}'"
            props << "  value: '${observation?.getAt(ns.value)?.text()?.replaceAll("'", "\\\\'")}'"
            sb.append props.join(", ")
            sb.append "});\n"
          }
        }
        getNonNullString(sb)
      }
    } catch (Exception e) {
      e.printStackTrace()
    }
  }

  @Deprecated String plotMedications(section) {
    StringBuilder sb = new StringBuilder()
    section?.getAt(ns.entry)?.each { entry ->
      def material = entry?.getAt(ns.substanceAdministration)?.getAt(ns.consumable)?.getAt(ns.manufacturedProduct)?.getAt(ns.manufacturedMaterial)
      sb.append "events.push({"
      def props = entry?.getAt(ns.substanceAdministration)?.getAt(ns.effectiveTime)?.find {et ->
        et.attributes().find { it.key == xsiType }?.value =~ 'IVL_TS'
      }?.with { et ->
        printEffectiveDateForJson(et)
      } ?: []
      props << "classification: 'Medication'"
      props << "  text: '${material?.getAt(ns.name)?.text() ? "${material?.getAt(ns.code)?.@displayName?.getAt(0)} (${material?.getAt(ns.name)?.text()?.replaceAll("'", "\\\\'")})" : "${material?.getAt(ns.code)?.@displayName?.getAt(0)?.replaceAll("'", "\\\\'")}"}'"
      sb.append props.join(", ")
      sb.append "});\n"
    }
    getNonNullString(sb)
  }

  @Deprecated String plotMedicalEquipment(section) {
    StringBuilder sb = new StringBuilder()
    section?.getAt(ns.entry)?.each { entry ->
      def material = entry?.getAt(ns.substanceAdministration)?.getAt(ns.consumable)?.getAt(ns.manufacturedProduct)?.getAt(ns.manufacturedMaterial)
      sb.append "events.push({"
      def props = printEffectiveDateForJson(entry?.getAt(ns.supply)?.getAt(ns.effectiveTime)?.getAt(0))
      props << "classification: 'Medical Equipment'"
      props << "  text: '${entry?.getAt(ns.supply)?.getAt(ns.participant)?.getAt(ns.participantRole)?.getAt(ns.playingDevice)?.getAt(ns.code)?.@displayName?.getAt(0)?.replaceAll("'", "\\\\'")}'"
      sb.append props.join(", ")
      sb.append "});\n"
    }
    getNonNullString(sb)
  }

  @Deprecated String plotProcedures(section) {
    StringBuilder sb = new StringBuilder()
    section?.getAt(ns.entry)?.each { entry ->
      sb.append "events.push({"
      def props = printEffectiveDateForJson(entry?.getAt(ns.procedure)?.getAt(ns.effectiveTime))
      props << "classification: 'Procedure'"
      props << "  text: '${entry?.getAt(ns.procedure)?.getAt(ns.code)?.@displayName?.getAt(0)?.replaceAll("'", "\\\\'")}'"
      sb.append props.join(", ")
      sb.append "});\n"
    }
    getNonNullString(sb)
  }

  @Deprecated String plotPlans(section) {
    StringBuilder sb = new StringBuilder()
    section?.getAt(ns.entry)?.each { entry ->
      sb.append "events.push({"
      def props = printEffectiveDateForJson(entry?.getAt(ns.observation)?.getAt(ns.effectiveTime)?.getAt(0))
      props << "classification: 'Plan'"
      props << "  text: '${entry?.getAt(ns.observation)?.getAt(ns.code)?.@displayName?.getAt(0)?.replaceAll("'", "\\\\'")}'"
      sb.append props.join(", ")
      sb.append "});\n"
    }
    getNonNullString(sb)
  }

  String reprintDateForJson(inString) {
    if (!inString) {
      return null
    }
    def effectiveDate = parseDate(inString)
    if (effectiveDate) {
      return "new Date(${effectiveDate.format('yyyy')}, ${effectiveDate.month }, ${effectiveDate.date} )"
    }
    return inString
  }

  private String getNonNullString(StringBuilder sb) {
    return sb != null && sb.toString().length() != 4 ? sb.toString() : ''
  }

  def createSectionMap(sections) {
    def map = [:]
    sections.each { section ->
      def code = section?.getAt(ns.code)?.@code?.getAt(0)
      switch (code) {
        case '11348-0':
          map.put('pastMedical', section);
          break
        case '10160-0':
          map.put('medications', section);
          break
        case '30954-2':
          map.put('labResults', section);
          break
        case '8716-3':
          map.put('vitalSigns', section);
          break
        case '48764-5':
          map.put('summaryPurpose', section);
          break
        case '46240-8':
          map.put('visitSummary', section);
          break
        case '11450-4':
          map.put('problems', section);
          break
        case '10157-6':
          map.put('familyHistory', section);
          break
        case '29762-2':
          map.put('socialHistory', section);
          break
        case '11369-6':
          map.put('immunizationHistory', section);
          break
        case '48765-2':
          map.put('alerts', section);
          break
        case '48768-6':
          map.put('payer', section);
          break
        case '18776-5':
          map.put('planOfCare', section);
          break
        case '11535-2':
          map.put('hospitalDischargeDiagnosis', section);
          break
        case '10183-2':
          map.put('hospitalDischargeMedications', section);
          break
        case '29545-1':
          map.put('physicalExamination', section);
          break
        case '47519-4':
          map.put('procedures', section);
          break
        default:
          log.warn "Unhandled section in createSectionMap: ${code}"
      }
    }
    return map
  }

  //Print the stylized alerts list
  String printPCP(xml) {
    StringWriter writer = new StringWriter()

    new groovy.xml.MarkupBuilder(writer).span() {
      def name = xml?.getAt(ns.documentationOf)?.getAt(ns.serviceEvent)?.getAt(ns.performer)?.getAt(ns.assignedEntity)?.getAt(ns.assignedPerson)?.getAt(ns.name)
      if (name) {
        span(name?.getAt(ns.given)?.getAt(0)?.text() + " " + name?.getAt(ns.family)?.text())
      } else {
        span("N/A")
      }
    }

    writer.toString()
  }

  String printNetworkID(xml) {
    StringWriter writer = new StringWriter()

    new groovy.xml.MarkupBuilder(writer).span(xml?.getAt(ns.recordTarget)?.getAt(ns.patientRole)?.getAt(ns.id)?.@extension?.toString()?.replace('[', '')?.replace(']', '')) {
      small(xml?.getAt(ns.recordTarget)?.getAt(ns.patientRole)?.getAt(ns.id)?.@root?.toString()?.replace('[', '(')?.replace(']', ')'))
    }

    writer.toString()
  }

  Set getSource(section) {
    def idSet = [];
    def ccdSource = section?.getAt(ns.entry)?.getAt(ns.text)
    idSet.add(ccdSource[0]?.@source + "/" + ccdSource[0]?.getAt(ns.ccd)?.text())
    return idSet
  }

  //Print the stylized alerts list
  String calcAge(dob) {
    if (dob) {
      double msPerGregorianYear = 365.25 * 86400 * 1000;
      def today = new Date()
      Integer age = (today.time - dob.getTime()) / msPerGregorianYear;

      return age + "yr,"
    } else {
      return 'unknown'
    }
  }

  //TODO: optimize with methods already acquiring vital results data
  String[] getHeight(section, dt, row) {
    def val = section?.getAt(ns.entry)?.getAt(ns.organizer)?.find {o ->
      o?.getAt(ns.effectiveTime)?.@value?.getAt(0) == dt
    }?.getAt(ns.component)?.getAt(ns.observation)?.find {o ->
      o?.getAt(ns.code)?.@code?.getAt(0) == row[0] && o?.getAt(ns.code)?.@codeSystem?.getAt(0) == row[1]
    }?.getAt(ns.value)

    if (!val || !val[0]) {
      return null
    }
    return formatHeight(val[0].@value.toString(), val[0].@unit.toString())
  }

  private String[] formatHeight(String value, String unit) {
    try {
      if ("in".equalsIgnoreCase(unit) || "[in_us]".equalsIgnoreCase(unit)) {
        float totalInches = value.toFloat()
        int feet = totalInches / 12
        float inches = totalInches % 12
        String inchString = (inches == (int) inches) ? (int) inches : sprintf('%.2f', inches)
        return [feet, 'ft', inchString, 'in']
      }
    } catch (Exception e) {
      // catch whatever parse error may occur
      log.error "Unable to parse height: ${value} ${unit} - ${e}"
    }
    // default just a pass through (which GSP will escape)
    return [value, unit]
  }

  String getPayerAsJson(payer) {
    def policyIdOriginal = payer?.getAt(ns.act)?.getAt(ns.id)?.@extension
    def performer = payer?.getAt(ns.act)?.getAt(ns.entryRelationship)?.getAt(ns.act)?.getAt(ns.performer)
    def organization = performer?.getAt(ns.assignedEntity)?.getAt(ns.representedOrganization)
    new JsonBuilder([
        organization: organization?.getAt(ns.name)?.getAt(0)?.text(),
        policyId: policyIdOriginal ? policyIdOriginal?.get(0) : null
    ]).toString()
  }

  /**
   * Fetches a text representation for a given node. The possible values array should be modified very carefully so as
   * to not mess with existing visualizations.
   *
   * @param section The section to scan for referenced nodes
   * @param node The node whose text representation is required
   * @return The text representation
   */
  private String getText(section, node, String defaultValue = 'N/A') {
    if (!node) {
      defaultValue
    } else {
      def possibleValues = [
          findTextById(section, node?.getAt(ns.text)?.getAt(ns.reference)?.getAt(0)?.@value),
          node?.getAt(ns.text)?.getAt(0)?.text(),
          node?.getAt(ns.value)?.getAt(0)?.@displayName,
          node?.getAt(ns.value)?.getAt(ns.translation)?.getAt(0)?.@displayName,
          node?.getAt(ns.code)?.getAt(0)?.@displayName,
          node?.getAt(ns.code)?.getAt(ns.originalText)?.getAt(0)?.text(),
          node?.getAt(ns.name)?.getAt(0)?.text(),
      ]
      def chosenValue = possibleValues.find{!it.is(null)}?:defaultValue
    }
  }

  private String normalizeObservationText(String observation){

    observation.equalsIgnoreCase("null") ? null : observation
  }

  String getAlertAsJson(alert, section) {
    def observation = alert?.getAt(ns.act)?.getAt(ns.entryRelationship)?.getAt(ns.observation)
    def playingEntity = observation?.getAt(ns.participant)?.getAt(ns.participantRole)?.getAt(ns.playingEntity)
    def observation2 = observation?.getAt(ns.entryRelationship)?.find {er -> er.@typeCode == 'MFST'}?.getAt(ns.observation)
    def alerts = new JsonBuilder([
        actParticipant: getText(section, playingEntity),
        actObservation: normalizeObservationText(getText(section, observation2, 'Unknown')),
        code: getCodeDetails(playingEntity?.getAt(ns.code)?.getAt(0)) //TODO: Do we need code for reactions of the allgery too?
    ]).toString()
    return alerts
  }

  String getSummaryPurposeAsJson(purpose) {
    def source = purpose?.getAt(ns.text)?.@source
    def code = purpose?.getAt(ns.act)?.getAt(ns.entryRelationship)?.getAt(ns.act)?.getAt(ns.code)?.getAt(0)
    new JsonBuilder([
        reason: getText(purpose, purpose?.getAt(ns.act)?.getAt(ns.entryRelationship)?.getAt(ns.act)),
        date: parseDate(purpose?.getAt(ns.act)?.getAt(ns.effectiveTime)?.text()),
        source: [
            name: source ? source.get(0) : null,
            id: purpose?.getAt(ns.text)?.getAt(ns.ccd)?.text()
        ],
        code: getCodeDetails(code)
    ])
  }

  String getVisitSummaryAsJson(entry, section) {
    def name = entry?.getAt(ns.encounter)?.getAt(ns.performer)?.getAt(ns.assignedEntity)?.getAt(ns.assignedPerson)?.getAt(ns.name)
    def source = entry?.getAt(ns.text)?.@source
    def reasonForVisit = entry?.getAt(ns.encounter)?.getAt(ns.entryRelationship)?.find {it.@typeCode == 'RSON'}
    def visitDateObj = printEffectiveDate(entry?.getAt(ns.encounter)?.getAt(ns.effectiveTime)?.getAt(0))

    new JsonBuilder([
        visitDate: getVisitDate(visitDateObj),
        type: getText(section, entry?.getAt(ns.encounter)),
        reason: getText(section, reasonForVisit?.getAt(ns.observation)),
        performer: [
            name: (name?.getAt(ns.given)?.text() ?: '') + " " + (name?.getAt(ns.family)?.text() ?: ''),
            date: visitDateObj
        ],
        source: [
            name: source ? source.get(0) : null,
            id: entry?.getAt(ns.text)?.getAt(ns.ccd)?.text()
        ],
        code: getCodeDetails(entry?.getAt(ns.encounter)?.getAt(ns.code)?.getAt(0))
    ]).toString()
  }


  Map getCodeDetails(def codeElement) {
    return [
        code: codeElement?.@code, codeSystem: codeElement?.@codeSystem,
        codeSystemName: codeElement?.@codeSystemName, displayName: codeElement?.@displayName
    ]
  }

  Date getVisitDate(def visitDateObj) {
    if (visitDateObj instanceof Date)
      return visitDateObj
    else if (visitDateObj?.start)
      return visitDateObj.start
    else if (visitDateObj?.stop)
      return visitDateObj.stop
    else
      return null
  }

  String getProblemAsJson(problem, section) {
    def observation = problem?.getAt(ns.act)?.getAt(ns.entryRelationship)?.getAt(ns.observation)
    def ccdSource = problem.getAt(ns.text).find {it.@id == 'CcdSource'}
    def code = observation?.getAt(ns.value)?.getAt(0)
    new JsonBuilder([
        name: getText(section, observation),
        started: parseDate(observation?.getAt(ns.effectiveTime)?.getAt(ns.low)?.@value?.getAt(0)),
        source: [
            name: ccdSource?.@source,
            id: ccdSource?.getAt(ns.ccd)?.text(),
        ],
        code: getCodeDetails(code)
    ]).toString()
  }

  String getPastMedicalAsJson(pastMedical, section) {
    def observation = pastMedical?.getAt(ns.observation)
    new JsonBuilder([
        name: getText(section, observation),
        code: getCodeDetails(observation?.getAt(ns.value)?.getAt(0))
    ]).toString()
  }

  String getFamilyHistoryAsJson(family) {
    def observation = family?.getAt(ns.observation)
    def relatedSubject = observation?.getAt(ns.subject)?.getAt(ns.relatedSubject)
    def code = relatedSubject?.getAt(ns.code)?.get(0)
    new JsonBuilder([
        name: relatedSubject?.getAt(ns.code)?.@displayName?.getAt(0) + ": " + observation?.getAt(ns.value)?.text(),
        code: getCodeDetails(code)
    ]).toString()
  }

  String getSocialHistoryAsJson(social, section) {
    new JsonBuilder([
        name: getText(section, social?.getAt(ns.observation)),
        status: social?.getAt(ns.observation)?.getAt(ns.value)?.getAt(0)?.text(),
        code: getCodeDetails(social?.getAt(ns.observation)?.getAt(ns.code)?.get(0))
    ]).toString()
  }

  String getImmunizationHistoryAsJson(immunization) {
    def substanceAdministration = immunization?.getAt(ns.substanceAdministration)
    def code = substanceAdministration?.getAt(ns.consumable)?.getAt(ns.manufacturedProduct)?.getAt(ns.manufacturedMaterial)?.getAt(ns.code)?.getAt(0)
    Date parsedDate = parseDate(substanceAdministration?.getAt(ns.effectiveTime)?.getAt(ns.center)?.@value?.getAt(0))
    return new JsonBuilder([
        name: code?.@displayName,
        date: parsedDate?.format("yyyy-MM-dd'T'HH:mm:ss'Z'"),
        code: getCodeDetails(code)
    ]).toString()
  }

  Date parseDate(String dateString) {
    def formats = [/\d{8}/: 'yyyyMMdd',
        /\d{14}/: 'yyyyMMddHHmmss',
        /\d{14}-\d{4}/: 'yyyyMMddHHmmssZ',
        /\d{14}\.\d{3}-\d{4}/: 'yyyyMMddHHmmss.SSSZ',
    ]
    if (dateString) {
      def format = formats.find {k, v -> dateString ==~ k}
      if (format) {
        new SimpleDateFormat(format.value).parse(dateString)
      } else {
        null
      }
    } else {
      return null
    }
  }

  String getMedicationAsJson(entry, medications = null) {
    def material = entry?.getAt(ns.substanceAdministration)?.getAt(ns.consumable)?.getAt(ns.manufacturedProduct)?.getAt(ns.manufacturedMaterial)
    def source = entry?.getAt(ns.text)?.@source
    def medication = material?.getAt(ns.name)?.text() ? "${material?.getAt(ns.code)?.@displayName?.getAt(0)} (${material?.getAt(ns.name)?.text()})" : "${material?.getAt(ns.code)?.@displayName?.getAt(0)}"
    def effectiveTime = entry?.getAt(ns.substanceAdministration)?.getAt(ns.effectiveTime)
    def assignedAuthor = entry?.getAt(ns.substanceAdministration)?.getAt(ns.author)?.getAt(ns.assignedAuthor)
    def prescriber = entry?.getAt(ns.substanceAdministration)?.getAt(ns.performer)?.getAt(ns.assignedEntity)?.getAt(ns.assignedPerson)

    String medicationsJson = new JsonBuilder([
        status: entry?.getAt(ns.substanceAdministration)?.getAt(ns.statusCode)?.@code?.getAt(0),
        material: medication,
        frequency: getMedicationFrequency(medications, entry),
        source: [
            name:  source ? source.get(0) : "",
            id: entry?.getAt(ns.text)?.getAt(ns.ccd)?.text() ?: ""
        ],
        effectiveDate: [
            low: parseDate(effectiveTime?.getAt(ns.low)?.@value?.getAt(0)),
            high: parseDate(effectiveTime?.getAt(ns.high)?.@value?.getAt(0))
        ],
        author: [
            given: assignedAuthor?.getAt(ns.assignedPerson)?.getAt(ns.name)?.getAt(ns.given)?.text(),
            family: assignedAuthor?.getAt(ns.assignedPerson)?.getAt(ns.name)?.getAt(ns.family)?.text(),
            organization: assignedAuthor?.getAt(ns.representedOrganization)?.getAt(ns.name)?.text(),
        ],
        prescriber: [
            prefix: (prescriber) ? prescriber?.getAt(ns.name)?.prefix?.text() : "",
            given: prescriber?.getAt(ns.name)?.getAt(ns.given)?.text() ?: "",
            family: prescriber?.getAt(ns.name)?.getAt(ns.family)?.text() ?: ""
        ],
        code: getCodeDetails(material?.getAt(ns.code)?.getAt(0))
    ]).toString()
    return medicationsJson
  }

  String getObservationAsJson(observation) {
    def val = observation?.getAt(ns.value)?.getAt(0)
    def ccdSource = observation?.parent()?.parent()?.parent()?.getAt(ns.text)
    def refRange = observation?.getAt(ns.referenceRange)?.getAt(0)


    String observations = new JsonBuilder([
        test: observation?.getAt(ns.code)?.@displayName?.getAt(0),
        result: [
            value: val?.@value ?: val?.text(),
            unit: val?.@unit ,
            level: getLabLevel(val?.@value, refRange)
        ],
        date: parseDate(observation?.getAt(ns.effectiveTime)?.@value?.getAt(0)),
        source: [
            name: ccdSource?.@source?.getAt(0),
            id: ccdSource?.getAt(ns.ccd)?.text(),
        ],
        code: getCodeDetails(observation?.getAt(ns.code)?.getAt(0))
    ]).toString()
    return observations
  }

  String getHospitalDischargeDiagnosisAsJson(entry, section) {
    def observation = entry?.getAt(ns.act)?.getAt(ns.entryRelationship)?.getAt(ns.observation)
    def code = observation?.getAt(ns.code)?.getAt(0)

    new JsonBuilder([
        name: getText(section, observation),
        started: parseDate(observation?.getAt(ns.effectiveTime)?.getAt(ns.low)?.@value?.getAt(0)),
        code: getCodeDetails(code)
    ]).toString()
  }

  String getHospitalDischargeMedicationsAsJson(entry, section) {
    def substanceAdministration = entry?.getAt(ns.act)?.getAt(ns.entryRelationship)?.getAt(ns.substanceAdministration)
    def material = substanceAdministration?.getAt(ns.consumable)?.getAt(ns.manufacturedProduct)?.getAt(ns.manufacturedMaterial)
    def medication = material?.getAt(ns.name)?.text() ? "${material?.getAt(ns.code)?.@displayName?.getAt(0)} (${material?.getAt(ns.name)?.text()})" : "${material?.getAt(ns.code)?.@displayName?.getAt(0)}"
    def effectiveTime = substanceAdministration?.getAt(ns.effectiveTime)
    def assignedAuthor = substanceAdministration?.getAt(ns.author)?.getAt(ns.assignedAuthor)
    def prescriber = substanceAdministration?.getAt(ns.performer)?.getAt(ns.assignedEntity)?.getAt(ns.assignedPerson)

    String medicationsJson = new JsonBuilder([
        status: substanceAdministration?.getAt(ns.statusCode)?.@code?.getAt(0),
        material: medication,
        frequency: getMedicationFrequency(section, entry),
        effectiveDate: [
            low: parseDate(effectiveTime?.getAt(ns.low)?.@value?.getAt(0)),
            high: parseDate(effectiveTime?.getAt(ns.high)?.@value?.getAt(0))
        ],
        author: [
            given: assignedAuthor?.getAt(ns.assignedPerson)?.getAt(ns.name)?.getAt(ns.given)?.text(),
            family: assignedAuthor?.getAt(ns.assignedPerson)?.getAt(ns.name)?.getAt(ns.family)?.text(),
            organization: assignedAuthor?.getAt(ns.representedOrganization)?.getAt(ns.name)?.text(),
        ],
        prescriber: [
            prefix: (prescriber) ? prescriber?.getAt(ns.name)?.prefix?.text() : "",
            given: prescriber?.getAt(ns.name)?.getAt(ns.given)?.text() ?: "",
            family: prescriber?.getAt(ns.name)?.getAt(ns.family)?.text() ?: ""
        ],
        code: getCodeDetails(material?.getAt(ns.code)?.getAt(0))
    ]).toString()
    return medicationsJson
  }

  //TODO: get more real data...
  String getPhysicalExaminationAsJson(entry, section) {
    def observation = entry?.getAt(ns.observation)
    def code = observation?.getAt(ns.code)?.getAt(0)
    def effectiveTime = observation?.getAt(ns.effectiveTime)

    new JsonBuilder([
        name: getText(section, observation),
        effectiveDate: [
            low: parseDate(effectiveTime?.getAt(ns.low)?.@value?.getAt(0)),
            high: parseDate(effectiveTime?.getAt(ns.high)?.@value?.getAt(0))
        ],
        code: getCodeDetails(code)
    ]).toString()
  }

  String getProceduresAsJson(entry, section) {
    def procedure = entry?.getAt(ns.procedure)
    def code = procedure?.getAt(ns.code)?.getAt(0)

    new JsonBuilder([
        name: getText(section, procedure),
        effectiveTime: parseDate(procedure?.getAt(ns.effectiveTime)?.@value?.getAt(0)),
        status: procedure.getAt(ns.statusCode)?.@code?.getAt(0),
        code: getCodeDetails(code)
    ]).toString()
  }

  String getLabLevel(value, refRange) {
    def rangeText = refRange?.getAt(ns.observationRange)?.getAt(ns.text)
    def rangeValue = refRange?.getAt(ns.observationRange)?.getAt(ns.value)
    if (rangeText?.text()) {

      //If lower then the normal low

      def range = rangeText?.text()?.split('-')
      def lowLimit = range[0]
      if (lowLimit > value) {
        return "low"
      }
      //If higher then the normal high
      def highLimit = range.size() > 1 ? range[1] : null
      if (highLimit && highLimit < value) {
        return "high"
      }

    } else if (rangeValue) {

      def obsValue = rangeValue
      //If lower then the normal low
      def lowLimit = obsValue?.getAt(ns.low)?.@value?.getAt(0)
      if (lowLimit > value) {
        return "low"
      }
      //If higher then the normal high
      def highLimit = obsValue?.getAt(ns.high)?.@value?.getAt(0)
      if (highLimit < value) {
        return "high"
      }
    }
    return ''
  }

  String getPlanOfCareAsJson(def entry, def section) {
    def theMap = entry.children().collect { getPlanOfCareAsMap(it, section) }.find{it}
    return new JsonBuilder(theMap).toString()
  }

  def Map<String, String> getPlanOfCareAsMap(groovy.util.Node element, groovy.util.Node section) {
    def tagName = element.name() instanceof QName ? element.name().localPart : (element.name() instanceof String ? element.name() : null)
    def ccdSource = element.parent().getAt(ns.text)
    switch (tagName) {
      case 'act':
        return [
            type: 'act',
            status: element[ns.statusCode].@code?.getAt(0),
            text: getText(section, element),
            source: [
                name: ccdSource?.@source?.getAt(0),
                id: ccdSource?.getAt(ns.ccd)?.text(),
            ],
            code: getCodeDetails(element?.getAt(ns.code)?.getAt(0))
        ]
      case 'encounter':
        return [
            type: 'encounter',
            status: element[ns.entryRelationship][ns.statusCode].@code?.getAt(0),
            text: getText(section, element[ns.entryRelationship]),
            effectiveDate: printEffectiveDate(element?.getAt(ns.effectiveTime)?.getAt(0)),
            source: [
                name: ccdSource?.@source?.getAt(0),
                id: ccdSource?.getAt(ns.ccd)?.text(),
            ],
            code: getCodeDetails(element?.getAt(ns.code)?.getAt(0))
        ]
      case 'procedure':
        return [
            type: 'procedure',
            status: element[ns.statusCode].@code?.getAt(0),
            text: getText(section, element),
            effectiveDate: printEffectiveDate(element?.getAt(ns.effectiveTime)?.getAt(0)),
            source: [
                name: ccdSource?.@source?.getAt(0),
                id: ccdSource?.getAt(ns.ccd)?.text(),
            ],
            code: getCodeDetails(element?.getAt(ns.code)?.getAt(0))
        ]
      case 'observation':
        return [
            type: 'observation',
            status: element[ns.statusCode].@code?.getAt(0),
            text: getText(section, element),
            effectiveDate: printEffectiveDate(element?.getAt(ns.effectiveTime)?.getAt(0)),
            source: [
                name: ccdSource?.@source?.getAt(0),
                id: ccdSource?.getAt(ns.ccd)?.text(),
            ],
            code: getCodeDetails(element?.getAt(ns.code)?.getAt(0))
        ]
      case 'substanceAdmin':
        return [
            type: 'substanceAdmin',
            text: 'substanceAdmin not supported',
            source: [
                name: ccdSource?.@source?.getAt(0),
                id: ccdSource?.getAt(ns.ccd)?.text(),
            ],
        ]
      case 'supply':
        return [
            type: 'supply',
            text: 'supply not supported',
            source: [
                name: ccdSource?.@source?.getAt(0),
                id: ccdSource?.getAt(ns.ccd)?.text(),
            ],
        ]
      case 'text':
        log.debug 'Skipping text section'
        return null
      default:
        log.warn "Cannot jsonize ${element.name()} with class ${element.name().getClass()} in planOfCare"
        return null
    }
  }
}
