package com.cds.hiro.ccd

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
    csu.events.size() == 56

  }

  def "test CcdToEventsMapper"() {
    given: "a sample ccd"
    def ccdString = this.class.classLoader.getResourceAsStream('sample-ccd.xml').text

    when: "parse it and map it to events"
    def obj = new CcdToEventsMapper()
    def results = obj.getEvents(ccdString)

    println(results)

    then:
    assert true
  }
}
