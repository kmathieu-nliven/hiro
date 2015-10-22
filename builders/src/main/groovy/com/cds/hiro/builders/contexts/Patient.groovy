package com.cds.hiro.builders.contexts

import groovy.transform.builder.Builder
import groovy.transform.builder.SimpleStrategy

/**
 * The patient to whom this document is tied
 */
@Builder(builderStrategy = SimpleStrategy, prefix = '')
class Patient {
  /** given name of patient */
  String given
  /** family name of patient*/
  String family

  /** Shortcut to set family and given name */
  void name(String family, String given) {
    this.family = family
    this.given = given
  }

  /** patient gender */
  String gender
  /** patient birth time */
  String birthTime

  /** patient marital status */
  String maritalStatus

  /** patient identifer */
  String root, extension

  /** shortcut to set patient identifier */
  void id(String root, String extension) {
    this.root = root
    this.extension = extension
  }

  /**
   * Represents patient address
   */
  @Builder(builderStrategy = SimpleStrategy, prefix = '')
  static class Address {
    String street, city, state, country, postalCode
  }
  Address addr

  void addr(@DelegatesTo(Address) Closure closure) {
    closure.resolveStrategy = Closure.DELEGATE_FIRST
    closure.delegate = addr = new Address()
    closure.call()
  }

  void addr(Address address) {
    addr = address
  }

  static Patient defaultPatient() {
    new Patient().with {
      name 'Doe', 'John'
      gender 'M'
      birthTime '19520413'
      maritalStatus 'M'

      id '1.2.3.4', '42'

      addr {
        street '123 Main St'
        city 'San Jose'
        state 'CA'
        postalCode '95129'
        country 'USA'
      }
      it
    }
  }
}
