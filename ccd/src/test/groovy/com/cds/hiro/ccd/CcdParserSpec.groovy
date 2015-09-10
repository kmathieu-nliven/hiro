package com.cds.hiro.ccd

/**
 * CCD parser spec
 *
 * Created by yuanyao on 9/4/15.
 */
import groovy.json.JsonBuilder
import spock.lang.Specification

class CcdParserSpec extends Specification {
  def "the code runs"() {
    given: "A ccd"
    //def outFile = new File('build/foo.txt')

    def ccdString = this.class.classLoader.getResourceAsStream('sample-ccd.xml').text

    when: "I send it into my my foooo"
    def aggregator = new CcdAggregator(ccd: new XmlParser().parseText(
        """<?xml version="1.0" encoding="UTF-8"?>
              <ClinicalDocument xmlns="urn:hl7-org:v3"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
              </ClinicalDocument>"""))
    def csu = new CcdStreamingUtil(
        originalRequestId: '1234'
    )
    csu.streamCcdEntries(aggregator, [ccdString])

//    csu.events.each {
//      outFile << (new JsonBuilder(it).toString())
//      outFile << '\n'
//    }

    then:
    csu.events.size() == 53

  }

  def "test CcdToEventsMapper"() {
    given: "init"

    def outFile = new File('build/foo.txt')
    def ccdString = this.class.classLoader.getResourceAsStream('sample-ccd.xml').text

    when: "act"

    def obj = new CcdToEventsMapper()
    def results = obj.getEvents(ccdString)

    println(results)

    then:

    assert true
  }
}
