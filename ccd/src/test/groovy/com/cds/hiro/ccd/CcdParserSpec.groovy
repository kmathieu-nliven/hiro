package com.cds.hiro.ccd

import groovy.json.JsonSlurper

/**
 * CCD parser spec
 *
 * Created by yuanyao on 9/4/15.
 */
import spock.lang.Specification

class CcdParserSpec extends Specification {

  public static final String EMPTY_DOCUMENT = """<?xml version="1.0" encoding="UTF-8"?>
              <ClinicalDocument xmlns="urn:hl7-org:v3"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
              </ClinicalDocument>"""

  def "the code runs"() {
    given: "A ccd"

    def ccdString = this.class.classLoader.getResourceAsStream('sample-ccd.xml').text

    when: "Parse it using CcdStreamingUtil"
    def aggregator = new CcdAggregator(ccd: new XmlParser().parseText(EMPTY_DOCUMENT))
    def csu = new CcdStreamingUtil(originalRequestId: '1234')
    csu.streamCcdEntries(aggregator, [ccdString])

    then:
    csu.events.size() == 59

  }

  def "test CcdToEventsMapper"() {
    given: "a sample ccd"
    def ccdString = this.class.classLoader.getResourceAsStream('sample-ccd.xml').text

    when: "parse it and map it to events"
    def obj = new CcdToEventsMapper()
    def results = obj.getEvents(ccdString)

    and: "parse both events and expected text to json"
    JsonSlurper slurper = new JsonSlurper()
    def expectedJson = slurper.parseText(this.class.classLoader.getResourceAsStream('sample-output.json').text)
    def resultJson = slurper.parseText(results)

    then: "result json should match expected json"
    expectedJson == resultJson

  }

  def "test effectiveTime low high values"() {
    given: "A ccd"

    def ccdString = this.class.classLoader.getResourceAsStream('sample-ccd.xml').text

    when: "Parse it using CcdStreamingUtil"
    def aggregator = new CcdAggregator(ccd: new XmlParser().parseText(EMPTY_DOCUMENT))
    def csu = new CcdStreamingUtil(originalRequestId: '1234')
    csu.streamCcdEntries(aggregator, [ccdString])
    def procedureEvent = csu.events.find {it.section == 'procedures' && it.data.code.code == '77057'}

    then:
    // procedure with event time should have high low values
    procedureEvent.data.effectiveTime?.low != null
    procedureEvent.data.effectiveTime?.high != null

  }
}
