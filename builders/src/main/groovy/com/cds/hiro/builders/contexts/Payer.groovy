package com.cds.hiro.builders.contexts

import groovy.transform.builder.Builder
import groovy.transform.builder.SimpleStrategy

@Builder(builderStrategy = SimpleStrategy, prefix = '')
class Payer {
  String name
  String identifiedAs
  String identifierIs
}
