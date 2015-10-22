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

  private static final def xsiType = new QName('http://www.w3.org/2001/XMLSchema-instance', 'type')
  private static final def ns = new Namespace('urn:hl7-org:v3')

  void renderText(Node node, StringWriter sb) {
    node?.children()?.each {
      if (it instanceof String) {
        sb.append it.replaceAll(/\\n/, '<br/>\n')
      } else {
        renderText(it as Node, sb)
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

      def freq = administration?.getAt(ns.effectiveTime)?.
          find { et ->
            et.attributes().find { it.key == xsiType }?.value =~ 'PIVL_TS'
          }?.period?.
          with { p ->
            p.@value[0] + ' ' + p.@unit[0]
          }

      if (dq && unit && freq) {
        return "$dq $unit every $freq"
      }
    }

    if (administration?.getAt(ns.text)?.getAt(ns.reference)?.@value?.getAt(0)) {
      return findTextById(section, administration?.getAt(ns.text)?.getAt(ns.reference)?.@value?.getAt(0) as String)
    }

    return ''
  }

  def String findTextById(section, String id) {
    if (!id) {
      return null
    }

    StringWriter sw = new StringWriter()
    def referencedElement = section?.depthFirst()?.find { it instanceof Node && it.@ID == id[1..-1] }
    if (referencedElement) {
      renderText(referencedElement, sw)
      sw.toString()
    } else {
      null
    }
  }

  private static def printEffectiveDate(Node effectiveDate) {
    if (!effectiveDate)
      return null

    if (effectiveDate.@value) {
      parseDate effectiveDate?.@value
    } else if (effectiveDate?.getAt(ns.center)?.@value?.getAt(0)) {
      parseDate(effectiveDate?.getAt(ns.center)?.@value?.getAt(0))
    } else if (effectiveDate?.getAt(ns.low)?.@value?.getAt(0) && effectiveDate?.getAt(ns.high)?.@value?.getAt(0)) {
      [start: parseDate(effectiveDate?.getAt(ns.low)?.@value?.getAt(0)),
       stop : parseDate(effectiveDate?.getAt(ns.high)?.@value?.getAt(0))]
    } else if (effectiveDate?.getAt(ns.low)?.@value?.getAt(0)) {
      [start: parseDate(effectiveDate?.getAt(ns.low)?.@value?.getAt(0))]
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
  static def printEffectiveDateForJson(effectiveDate) {
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

  private static String getValue(section, dt, row) {
    if (!section) {
      return null
    }
    def val = section?.getAt(ns.entry)?.getAt(ns.organizer)?.find { o ->
      o?.getAt(ns.effectiveTime)?.@value?.getAt(0) == dt
    }?.getAt(ns.component)?.getAt(ns.observation)?.find { o ->
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

  static String reprintDateForJson(inString) {
    if (!inString) {
      return null
    }
    def effectiveDate = parseDate(inString)
    if (effectiveDate) {
      return "new Date(${effectiveDate.format('yyyy')}, ${effectiveDate.month}, ${effectiveDate.date} )"
    }
    return inString
  }

  private static String getNonNullString(StringBuilder sb) {
    return sb != null && sb.toString().length() != 4 ? sb.toString() : ''
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
      possibleValues.find { !it.is(null) } ?: defaultValue
    }
  }

  private static String normalizeObservationText(String observation) {

    observation.equalsIgnoreCase("null") ? null : observation
  }

  String getAlert(alert, section) {
    def observation = alert?.getAt(ns.act)?.getAt(ns.entryRelationship)?.getAt(ns.observation)
    def playingEntity = observation?.getAt(ns.participant)?.getAt(ns.participantRole)?.getAt(ns.playingEntity)
    def observation2 = observation?.getAt(ns.entryRelationship)?.find { er -> er.@typeCode == 'MFST' }?.getAt(ns.observation)
    def alerts = new JsonBuilder([
        actParticipant: getText(section, playingEntity),
        actObservation: normalizeObservationText(getText(section, observation2, 'Unknown')),
        code          : getCodeDetails(playingEntity?.getAt(ns.code)?.getAt(0)) //TODO: Do we need code for reactions of the allgery too?
    ]).toString()
    return alerts
  }

  String getSummaryPurpose(purpose) {
    def source = purpose?.getAt(ns.text)?.@source
    def code = purpose?.getAt(ns.act)?.getAt(ns.entryRelationship)?.getAt(ns.act)?.getAt(ns.code)?.getAt(0)
    new JsonBuilder([
        reason: getText(purpose, purpose?.getAt(ns.act)?.getAt(ns.entryRelationship)?.getAt(ns.act)),
        date  : parseDate(purpose?.getAt(ns.act)?.getAt(ns.effectiveTime)?.text()),
        source: [
            name: source ? source.get(0) : null,
            id  : purpose?.getAt(ns.text)?.getAt(ns.ccd)?.text()
        ],
        code  : getCodeDetails(code)
    ])
  }

  String getVisitSummary(entry, section) {
    def name = entry?.getAt(ns.encounter)?.getAt(ns.performer)?.getAt(ns.assignedEntity)?.getAt(ns.assignedPerson)?.getAt(ns.name)
    def source = entry?.getAt(ns.text)?.@source
    def reasonForVisit = entry?.getAt(ns.encounter)?.getAt(ns.entryRelationship)?.find { it.@typeCode == 'RSON' }
    def visitDateObj = printEffectiveDate(entry?.getAt(ns.encounter)?.getAt(ns.effectiveTime)?.getAt(0))

    new JsonBuilder([
        visitDate: getVisitDate(visitDateObj),
        type     : getText(section, entry?.getAt(ns.encounter)),
        reason   : getText(section, reasonForVisit?.getAt(ns.observation)),
        performer: [
            name: (name?.getAt(ns.given)?.text() ?: '') + " " + (name?.getAt(ns.family)?.text() ?: ''),
            date: visitDateObj
        ],
        source   : [
            name: source ? source.get(0) : null,
            id  : entry?.getAt(ns.text)?.getAt(ns.ccd)?.text()
        ],
        code     : getCodeDetails(entry?.getAt(ns.encounter)?.getAt(ns.code)?.getAt(0))
    ]).toString()
  }


  static Map getCodeDetails(def codeElement) {
    return [
        code          : codeElement?.@code, codeSystem: codeElement?.@codeSystem,
        codeSystemName: codeElement?.@codeSystemName, displayName: codeElement?.@displayName
    ]
  }

  static Date getVisitDate(def visitDateObj) {
    if (visitDateObj instanceof Date)
      return visitDateObj
    else if (visitDateObj?.start)
      return visitDateObj.start
    else if (visitDateObj?.stop)
      return visitDateObj.stop
    else
      return null
  }

  String getProblem(problem, section) {
    def observation = problem?.getAt(ns.act)?.getAt(ns.entryRelationship)?.getAt(ns.observation)
    def ccdSource = problem.getAt(ns.text).find { it.@id == 'CcdSource' }
    def code = observation?.getAt(ns.value)?.getAt(0)
    new JsonBuilder([
        name   : getText(section, observation),
        started: parseDate(observation?.getAt(ns.effectiveTime)?.getAt(ns.low)?.@value?.getAt(0)),
        source : [
            name: ccdSource?.@source,
            id  : ccdSource?.getAt(ns.ccd)?.text(),
        ],
        status : observation?.getAt(ns.statusCode)?.@code?.getAt(0),
        code   : getCodeDetails(code)
    ]).toString()
  }

  String getPastMedical(pastMedical, section) {
    def observation = pastMedical?.getAt(ns.observation)
    new JsonBuilder([
        name: getText(section, observation),
        code: getCodeDetails(observation?.getAt(ns.value)?.getAt(0))
    ]).toString()
  }

  static String getFamilyHistory(family) {
    def observation = family?.getAt(ns.observation)
    def relatedSubject = observation?.getAt(ns.subject)?.getAt(ns.relatedSubject)
    def code = relatedSubject?.getAt(ns.code)?.get(0)
    new JsonBuilder([
        name: relatedSubject?.getAt(ns.code)?.@displayName?.getAt(0) + ": " + observation?.getAt(ns.value)?.text(),
        code: getCodeDetails(code)
    ]).toString()
  }

  String getSocialHistory(social, section) {
    new JsonBuilder([
        name  : getText(section, social?.getAt(ns.observation)),
        status: social?.getAt(ns.observation)?.getAt(ns.value)?.getAt(0)?.text(),
        code  : getCodeDetails(social?.getAt(ns.observation)?.getAt(ns.code)?.get(0))
    ]).toString()
  }

  static String getImmunizationHistory(immunization) {
    def substanceAdministration = immunization?.getAt(ns.substanceAdministration)
    def code = substanceAdministration?.getAt(ns.consumable)?.getAt(ns.manufacturedProduct)?.getAt(ns.manufacturedMaterial)?.getAt(ns.code)?.getAt(0)
    Date parsedDate = parseDate(substanceAdministration?.getAt(ns.effectiveTime)?.getAt(ns.center)?.@value?.getAt(0))
    return new JsonBuilder([
        name: code?.@displayName,
        date: parsedDate?.format("yyyy-MM-dd'T'HH:mm:ss'Z'"),
        code: getCodeDetails(code)
    ]).toString()
  }

  static Date parseDate(String dateString) {
    def formats = [/\d{8}/              : 'yyyyMMdd',
                   /\d{14}/             : 'yyyyMMddHHmmss',
                   /\d{14}-\d{4}/       : 'yyyyMMddHHmmssZ',
                   /\d{14}\.\d{3}-\d{4}/: 'yyyyMMddHHmmss.SSSZ',
    ]
    if (dateString) {
      def format = formats.find { k, v -> dateString ==~ k }
      if (format) {
        new SimpleDateFormat(format.value).parse(dateString)
      } else {
        null
      }
    } else {
      return null
    }
  }

  String getMedication(entry, medications = null) {
    def material = entry?.getAt(ns.substanceAdministration)?.getAt(ns.consumable)?.getAt(ns.manufacturedProduct)?.getAt(ns.manufacturedMaterial)
    def source = entry?.getAt(ns.text)?.@source
    def medication = material?.getAt(ns.name)?.text() ? "${material?.getAt(ns.code)?.@displayName?.getAt(0)} (${material?.getAt(ns.name)?.text()})" : "${material?.getAt(ns.code)?.@displayName?.getAt(0)}"
    def effectiveTime = entry?.getAt(ns.substanceAdministration)?.getAt(ns.effectiveTime)
    def assignedAuthor = entry?.getAt(ns.substanceAdministration)?.getAt(ns.author)?.getAt(ns.assignedAuthor)
    def prescriber = entry?.getAt(ns.substanceAdministration)?.getAt(ns.performer)?.getAt(ns.assignedEntity)?.getAt(ns.assignedPerson)

    String medicationsJson = new JsonBuilder([
        status       : entry?.getAt(ns.substanceAdministration)?.getAt(ns.statusCode)?.@code?.getAt(0),
        material     : medication,
        frequency    : getMedicationFrequency(medications, entry),
        source       : [
            name: source ? source.get(0) : "",
            id  : entry?.getAt(ns.text)?.getAt(ns.ccd)?.text() ?: ""
        ],
        effectiveDate: [
            low : parseDate(effectiveTime?.getAt(ns.low)?.@value?.getAt(0)),
            high: parseDate(effectiveTime?.getAt(ns.high)?.@value?.getAt(0))
        ],
        author       : [
            given       : assignedAuthor?.getAt(ns.assignedPerson)?.getAt(ns.name)?.getAt(ns.given)?.text(),
            family      : assignedAuthor?.getAt(ns.assignedPerson)?.getAt(ns.name)?.getAt(ns.family)?.text(),
            organization: assignedAuthor?.getAt(ns.representedOrganization)?.getAt(ns.name)?.text(),
        ],
        prescriber   : [
            prefix: (prescriber) ? prescriber?.getAt(ns.name)?.prefix?.text() : "",
            given : prescriber?.getAt(ns.name)?.getAt(ns.given)?.text() ?: "",
            family: prescriber?.getAt(ns.name)?.getAt(ns.family)?.text() ?: ""
        ],
        code         : getCodeDetails(material?.getAt(ns.code)?.getAt(0))
    ]).toString()
    return medicationsJson
  }

  static String getObservation(observation) {
    def val = observation?.getAt(ns.value)?.getAt(0)

    def organizer = observation?.parent()?.parent()
    def ccdSource = organizer?.parent()?.getAt(ns.text)
    def refRange = observation?.getAt(ns.referenceRange)?.getAt(0)


    String observations = new JsonBuilder([
        test         : observation?.getAt(ns.code)?.@displayName?.getAt(0),
        result       : [
            value         : val?.@value ?: val?.text(),
            unit          : val?.@unit,
            level         : getLabLevel(val?.@value, refRange),
            code          : val?.@code,
            codeSystemName: val?.@codeSystemName,
        ].findAll { k, v -> v != null },
        date         : parseDate(observation?.getAt(ns.effectiveTime)?.@value?.getAt(0) ?: organizer?.getAt(ns.effectiveTime)?.@value?.getAt(0)),
        effectiveDate: [
            low : parseDate(observation?.getAt(ns.effectiveTime)?.@value?.getAt(0) ?: organizer?.getAt(ns.effectiveTime)?.@value?.getAt(0)),
            high: parseDate(observation?.getAt(ns.effectiveTime)?.@value?.getAt(0) ?: organizer?.getAt(ns.effectiveTime)?.@value?.getAt(0)),
        ],
        source       : [
            name: ccdSource?.@source?.getAt(0),
            id  : ccdSource?.getAt(ns.ccd)?.text(),
        ],
        status       : observation?.getAt(ns.statusCode)?.@code?.getAt(0),
        code         : getCodeDetails(observation?.getAt(ns.code)?.getAt(0))
    ]).toString()
    return observations
  }

  static String getAssessment(observation) {
    def val = observation?.getAt(ns.value)?.getAt(0)
    def ccdSource = null

    String observations = new JsonBuilder([
        result       : [
            unit          : val?.@unit,
            code          : val?.@code,
            codeSystemName: val?.@codeSystemName,
        ].findAll { k, v -> v != null },
        date         : parseDate(observation?.getAt(ns.effectiveTime)?.@value?.getAt(0)),
        effectiveDate: [
            low : parseDate(observation?.getAt(ns.effectiveTime)?.@value?.getAt(0)),
            high: parseDate(observation?.getAt(ns.effectiveTime)?.@value?.getAt(0)),
        ],
        source       : [
            name: ccdSource?.@source?.getAt(0),
            id  : ccdSource?.getAt(ns.ccd)?.text(),
        ],
        code         : getCodeDetails(observation?.getAt(ns.code)?.getAt(0))
    ]).toString()
    return observations
  }

  String getDischargeDiagnosis(entry, section) {
    def observation = entry?.getAt(ns.act)?.getAt(ns.entryRelationship)?.getAt(ns.observation)
    def code = observation?.getAt(ns.code)?.getAt(0)

    new JsonBuilder([
        name         : getText(section, observation),
        started      : parseDate(observation?.getAt(ns.effectiveTime)?.getAt(ns.low)?.@value?.getAt(0)),
        effectiveDate: [
            low : parseDate(observation?.getAt(ns.effectiveTime)?.getAt(ns.low)?.@value?.getAt(0)),
            high: null,
        ],
        code         : getCodeDetails(code)
    ]).toString()
  }

  String getDischargeMedications(entry, section) {
    def substanceAdministration = entry?.getAt(ns.act)?.getAt(ns.entryRelationship)?.getAt(ns.substanceAdministration)
    def material = substanceAdministration?.getAt(ns.consumable)?.getAt(ns.manufacturedProduct)?.getAt(ns.manufacturedMaterial)
    def medication = material?.getAt(ns.name)?.text() ? "${material?.getAt(ns.code)?.@displayName?.getAt(0)} (${material?.getAt(ns.name)?.text()})" : "${material?.getAt(ns.code)?.@displayName?.getAt(0)}"
    def effectiveTime = substanceAdministration?.getAt(ns.effectiveTime)
    def assignedAuthor = substanceAdministration?.getAt(ns.author)?.getAt(ns.assignedAuthor)
    def prescriber = substanceAdministration?.getAt(ns.performer)?.getAt(ns.assignedEntity)?.getAt(ns.assignedPerson)

    String medicationsJson = new JsonBuilder([
        status       : substanceAdministration?.getAt(ns.statusCode)?.@code?.getAt(0),
        material     : medication,
        frequency    : getMedicationFrequency(section, entry),
        effectiveDate: [
            low : parseDate(effectiveTime?.getAt(ns.low)?.@value?.getAt(0)),
            high: parseDate(effectiveTime?.getAt(ns.high)?.@value?.getAt(0))
        ],
        author       : [
            given       : assignedAuthor?.getAt(ns.assignedPerson)?.getAt(ns.name)?.getAt(ns.given)?.text(),
            family      : assignedAuthor?.getAt(ns.assignedPerson)?.getAt(ns.name)?.getAt(ns.family)?.text(),
            organization: assignedAuthor?.getAt(ns.representedOrganization)?.getAt(ns.name)?.text(),
        ],
        prescriber   : [
            prefix: (prescriber) ? prescriber?.getAt(ns.name)?.prefix?.text() : "",
            given : prescriber?.getAt(ns.name)?.getAt(ns.given)?.text() ?: "",
            family: prescriber?.getAt(ns.name)?.getAt(ns.family)?.text() ?: ""
        ],
        code         : getCodeDetails(material?.getAt(ns.code)?.getAt(0))
    ]).toString()
    return medicationsJson
  }

  //TODO: get more real data...
  String getPhysicalExamination(entry, section) {
    def observation = entry?.getAt(ns.observation)
    def code = observation?.getAt(ns.code)?.getAt(0)
    def effectiveTime = observation?.getAt(ns.effectiveTime)

    new JsonBuilder([
        name         : getText(section, observation),
        effectiveDate: [
            low : parseDate(effectiveTime?.getAt(ns.low)?.@value?.getAt(0)),
            high: parseDate(effectiveTime?.getAt(ns.high)?.@value?.getAt(0))
        ],
        code         : getCodeDetails(code)
    ]).toString()
  }

  String getProcedures(entry, section) {
    def procedure = entry?.getAt(ns.procedure)
    def code = procedure?.getAt(ns.code)?.getAt(0)
    def effectiveTime = procedure?.getAt(ns.effectiveTime)

    if (effectiveTime?.@value?.getAt(0)) {
      new JsonBuilder([
          name         : getText(section, procedure),
          effectiveTime: parseDate(effectiveTime?.@value?.getAt(0)),
          effectiveDate: [
              low: parseDate(effectiveTime?.@value?.getAt(0)),
              high: parseDate(effectiveTime?.@value?.getAt(0)),
          ],
          status       : procedure.getAt(ns.statusCode)?.@code?.getAt(0),
          code         : getCodeDetails(code)
      ]).toString()
    } else if (effectiveTime?.getAt(ns.low)?.@value?.getAt(0)) {
      new JsonBuilder([
          name         : getText(section, procedure),
          effectiveTime: [
              low : parseDate(effectiveTime?.getAt(ns.low)?.@value?.getAt(0)),
              high: parseDate(effectiveTime?.getAt(ns.high)?.@value?.getAt(0))
          ],
          effectiveDate: [
              low : parseDate(effectiveTime?.getAt(ns.low)?.@value?.getAt(0)),
              high: parseDate(effectiveTime?.getAt(ns.high)?.@value?.getAt(0))
          ],
          status       : procedure.getAt(ns.statusCode)?.@code?.getAt(0),
          code         : getCodeDetails(code)
      ]).toString()
    } else {
      new JsonBuilder([
          name  : getText(section, procedure),
          status: procedure.getAt(ns.statusCode)?.@code?.getAt(0),
          code  : getCodeDetails(code)
      ]).toString()

    }
  }

  static String getLabLevel(value, refRange) {
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
    return null
  }

  String getPlanOfCare(def entry, def section) {
    def theMap = entry.children().collect { getPlanOfCareAsMap(it, section) }.find { it }
    return new JsonBuilder(theMap).toString()
  }

  def Map<String, Object> getPlanOfCareAsMap(Node element, Node section) {
    def tagName = element.name() instanceof QName ? element.name().localPart : (element.name() instanceof String ? element.name() : null)
    def ccdSource = element.parent().getAt(ns.text)
    switch (tagName) {
      case 'act':
        return [
            type  : 'act',
            status: element[ns.statusCode].@code?.getAt(0),
            text  : getText(section, element),
            source: [
                name: ccdSource?.@source?.getAt(0),
                id  : ccdSource?.getAt(ns.ccd)?.text(),
            ],
            code  : getCodeDetails(element?.getAt(ns.code)?.getAt(0))
        ]
      case 'encounter':
        return [
            type         : 'encounter',
            status       : element[ns.entryRelationship][ns.statusCode].@code?.getAt(0),
            text         : getText(section, element[ns.entryRelationship]),
            effectiveDate: printEffectiveDate(element?.getAt(ns.effectiveTime)?.getAt(0)),
            source       : [
                name: ccdSource?.@source?.getAt(0),
                id  : ccdSource?.getAt(ns.ccd)?.text(),
            ],
            code         : getCodeDetails(element?.getAt(ns.code)?.getAt(0))
        ]
      case 'procedure':
        return [
            type         : 'procedure',
            status       : element[ns.statusCode].@code?.getAt(0),
            text         : getText(section, element),
            effectiveDate: printEffectiveDate(element?.getAt(ns.effectiveTime)?.getAt(0)),
            source       : [
                name: ccdSource?.@source?.getAt(0),
                id  : ccdSource?.getAt(ns.ccd)?.text(),
            ],
            code         : getCodeDetails(element?.getAt(ns.code)?.getAt(0))
        ]
      case 'observation':
        return [
            type         : 'observation',
            status       : element[ns.statusCode].@code?.getAt(0),
            text         : getText(section, element),
            effectiveDate: printEffectiveDate(element?.getAt(ns.effectiveTime)?.getAt(0)),
            source       : [
                name: ccdSource?.@source?.getAt(0),
                id  : ccdSource?.getAt(ns.ccd)?.text(),
            ],
            code         : getCodeDetails(element?.getAt(ns.code)?.getAt(0))
        ]
      case 'substanceAdmin':
        return [
            type  : 'substanceAdmin',
            text  : 'substanceAdmin not supported',
            source: [
                name: ccdSource?.@source?.getAt(0),
                id  : ccdSource?.getAt(ns.ccd)?.text(),
            ],
        ]
      case 'supply':
        return [
            type  : 'supply',
            text  : 'supply not supported',
            source: [
                name: ccdSource?.@source?.getAt(0),
                id  : ccdSource?.getAt(ns.ccd)?.text(),
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
