package com.cds.hiro.ccd

import groovy.json.JsonSlurper

/**
 * CCD parser spec
 *
 * Created by yuanyao on 9/4/15.
 */
import spock.lang.Specification

class CcdParserSpec extends Specification {
  def "the code runs"() {
    given: "A ccd"

    def ccdString = this.class.classLoader.getResourceAsStream('sample-ccd.xml').text

    when: "Parse it using CcdStreamingUtil"
    def aggregator = new CcdAggregator(ccd: new XmlParser().parseText(
        """<?xml version="1.0" encoding="UTF-8"?>
              <ClinicalDocument xmlns="urn:hl7-org:v3"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
              </ClinicalDocument>"""))
    def csu = new CcdStreamingUtil(
        originalRequestId: '1234'
    )
    csu.streamCcdEntries(aggregator, [ccdString])

    then:
    csu.events.size() == 58

  }

  def "test CcdToEventsMapper"() {
    given: "a sample ccd"
    def ccdString = this.class.classLoader.getResourceAsStream('sample-ccd.xml').text

    when: "parse it and map it to events"
    def obj = new CcdToEventsMapper()
    def results = obj.getEvents(ccdString)
    println(results)

    and: "parse both events and expected text to json"
    JsonSlurper slurper = new JsonSlurper()
    def expectedJson = slurper.parseText(this.class.classLoader.getResourceAsStream('sample-output.json').text)
    def resultJson = slurper.parseText(results)

    then: "result json should match expected json"
    expectedJson == resultJson

  }

  private List<Map> jsonizeSingleComponent(def component) {
    def normalizeCcdUtil = new NormalizeCcdUtil()
    def ccdStreamingUtil = new CcdStreamingUtil(originalRequestId: '1234')
    normalizeCcdUtil.streamComponent(component, null, [onNewEntry: ccdStreamingUtil.onNewEntry])
    ccdStreamingUtil.events
  }

  def "test effectiveTime low high values"() {
    given: "A ccd"

    def ccdString = this.class.classLoader.getResourceAsStream('sample-ccd.xml').text

    when: "Parse it using CcdStreamingUtil"
    def aggregator = new CcdAggregator(ccd: new XmlParser().parseText(
        """<?xml version="1.0" encoding="UTF-8"?>
              <ClinicalDocument xmlns="urn:hl7-org:v3"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
              </ClinicalDocument>"""))
    def csu = new CcdStreamingUtil(
        originalRequestId: '1234'
    )
    csu.streamCcdEntries(aggregator, [ccdString])

    then:
    // procedure with event time should have high low values
    csu.events[55].data.effectiveTime.low != null
    csu.events[55].data.effectiveTime.high != null

  }
}
