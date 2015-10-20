package com.cds.hiro.builders.contexts

import groovy.transform.builder.Builder
import groovy.transform.builder.SimpleStrategy

@Builder(builderStrategy = SimpleStrategy, prefix = '')
class Encounter {
  void met(String family, String given) {
    this.given = given
    this.family = family
  }
  String given, family, at
  String between, and
}
