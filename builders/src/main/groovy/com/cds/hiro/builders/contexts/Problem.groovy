package com.cds.hiro.builders.contexts

import com.github.rahulsom.cda.CE
import groovy.transform.builder.Builder
import groovy.transform.builder.SimpleStrategy

@Builder(builderStrategy = SimpleStrategy, prefix = '')
class Problem {
  CE code
  String between, and, since, withStatus
  boolean active = false
}
