package com.cds.hiro.builders.contexts

import com.github.rahulsom.cda.CE
import groovy.transform.builder.Builder
import groovy.transform.builder.SimpleStrategy

@Builder(builderStrategy = SimpleStrategy, prefix = '')
class Diagnosis {
  CE code
  String performerGiven
  String performerFamily

  Diagnosis by(String family, String given) {
    performerFamily = family
    performerGiven = given
    this
  }
  String at
  String facilityRoot
  String performerId

  Diagnosis identifiedAs(String root, String extension) {
    facilityRoot = root
    performerId = extension
    this
  }
  String on
}