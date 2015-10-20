package com.cds.hiro.builders.contexts

import groovy.transform.builder.Builder
import groovy.transform.builder.SimpleStrategy

@Builder(builderStrategy = SimpleStrategy, prefix = '')
class Author {
  String of, identifiedAs, at, given, family, phone
}
