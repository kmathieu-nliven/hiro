package com.cds.hiro.ccd

import groovy.json.JsonBuilder

/**
 * TODO: Explain This
 *
 * Created by seth.darr on 8/20/15.
 */
class CcdToEventsMapper {
  String getEvents(String ccd) {
    def aggregator = new CcdAggregator(ccd: new XmlParser().parseText(
        """<?xml version="1.0" encoding="UTF-8"?>
              <ClinicalDocument xmlns="urn:hl7-org:v3"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
              </ClinicalDocument>"""))
    def csu = new CcdStreamingUtil(
        originalRequestId: '1234'
    )
    csu.streamCcdEntries(aggregator, [ccd])

    def result = csu.events.
        collect {
          new JsonBuilder(it).toString()
        }.
        join(',')
    "[" + result + "]"
  }
}
