package com.cds.hiro.dataload

import groovy.transform.ToString

/**
 * Created by rahul on 10/15/15.
 */
@ToString(includePackage = false)
class ExecutionConfig {
  String geonamesUsername
  Double lat = 32.902071
  Double lng = -117.207741

  @ToString(includePackage = false)
  static class Baymax {
    String username = 'admin'
    String password = 'admin'
    String baseUrl
  }

  @ToString(includePackage = false)
  static class Measure {
    String name
    Double compliant = 1.25
    Double complement = 1.6
  }

  Integer facilities = 4
  Integer patients = 16
  Integer startingPort = 9000

  Baymax baymax = new Baymax()
  List<Measure> measures

  @ToString(includePackage = false)
  static class Aco {
    String namespaceId = 'ACO'
    String universalId = '1.2.4.1'
  }

  Aco aco = new Aco()

}
