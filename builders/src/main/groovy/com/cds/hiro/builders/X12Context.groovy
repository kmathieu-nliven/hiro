package com.cds.hiro.builders

import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.builder.Builder
import groovy.transform.builder.SimpleStrategy

/**
 * Created by rahul on 10/20/15.
 */
@CompileStatic
@AutoClone
@Builder(builderStrategy = SimpleStrategy, prefix = '')
class X12Context extends BaseContext {
  Double patientWeight
  String conventionReference  = '005010X222A1'
}
