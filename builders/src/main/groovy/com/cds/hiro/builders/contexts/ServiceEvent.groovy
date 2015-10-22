package com.cds.hiro.builders.contexts

import groovy.transform.builder.Builder
import groovy.transform.builder.SimpleStrategy

@Builder(builderStrategy = SimpleStrategy, prefix = '')
class ServiceEvent {
  String given, family, root, extension, at, identifiedAs

  void id(String root, String extension) {
    this.root = root
    this.extension = extension
  }

  void initiatedBy(String family, String given) {
    this.family = family
    this.given = given
  }
}
