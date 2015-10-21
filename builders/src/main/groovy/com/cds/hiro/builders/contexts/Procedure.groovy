package com.cds.hiro.builders.contexts

import com.github.rahulsom.cda.CD
import groovy.transform.builder.Builder
import groovy.transform.builder.SimpleStrategy

@Builder(builderStrategy = SimpleStrategy, prefix = '')
class Procedure {
  CD code
  String on, from, to, withStatus
}
