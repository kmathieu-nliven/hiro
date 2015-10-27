package com.cds.hiro.dataload

import com.cds.hiro.builders.X12Context
import com.github.rahulsom.genealogy.Person
import com.github.rahulsom.geocoder.domain.Address

/**
 * Created by rahul on 10/27/15.
 */

class X12ContextFactory {

  Person person
  String dob
  Facility facility
  String identifier
  Address address

  X12ContextFactory(Person person, String dob, Facility facility, String identifier, Address address) {
    this.person = person
    this.dob = dob
    this.facility = facility
    this.identifier = identifier
    this.address = address
  }

  private static Random rnd = new Random()

  private List<X12Context> _contextList = []

  X12Context getContext() {
    def context = new X12Context()
    context.with {
      patient {
        name person.lastName, person.firstName
        gender person.gender
        birthTime dob
        maritalStatus rnd.nextBoolean() ? 'M' : 'S'

        id facility.identifier, identifier

        addr {
          street address.street
          city address.city
          state address.state
          postalCode address.zip
          country address.country
        }
      }
      authoredBy 'Johnson', 'Kimberly' of facility.name identifiedAs facility.identifier at new Date().format('yyyyMMdd')
    }
    _contextList << context
    context
  }

  public List<X12Context> getContextList() {
    this._contextList
  }
}
