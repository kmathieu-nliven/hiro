package com.cds.hiro.ccd

import groovy.util.logging.Log4j
import groovy.xml.Namespace

import java.util.concurrent.Executors

/**
 * An adapter to facilitate streaming of new ccd entries ( A U B - A) to the next layers
 */
@Log4j
class CcdAggregator {
  def ccd
  // Specify bunch of closures to get called on some event while aggregating ccd
  // for e.g onNewEntry, onUpdateEntry, onKnownEntry
  Map closuresMap
  def ns = new Namespace('urn:hl7-org:v3')

  synchronized def aggregate(def newCcd, int noOfThreads = 5) {
    def normalizeCcdUtil = new NormalizeCcdUtil()
    normalizeCcdUtil.executorService = Executors.newFixedThreadPool(noOfThreads)

    if (!ccd) {
      log.debug("Streaming single ccd entries")
      def componentArray = newCcd?.getAt(ns.component)?.getAt(ns.structuredBody)?.getAt(ns.component)
      componentArray?.each {component ->
        def sectionCode = component?.getAt(ns.section)?.getAt(ns.code)?.@code?.getAt(0)
        List latestEntries = component?.getAt(ns.section)?.getAt(ns.entry)
        normalizeCcdUtil.streamEntries(sectionCode, latestEntries, closuresMap)
      }
      ccd = newCcd
    } else if (newCcd) {
      log.debug("Aggregating new ccd...")
      def aggregatedCcd = normalizeCcdUtil.getAggregatedCcd(ccd, [newCcd], [:], true, true, false, closuresMap)
      normalizeCcdUtil.executorService?.shutdown()
      ccd = aggregatedCcd
    }
    ccd
  }

  def updateCcd(def newCcd) {
    if (newCcd) {
      ccd = aggregate(newCcd)
    }
    ccd
  }
}
