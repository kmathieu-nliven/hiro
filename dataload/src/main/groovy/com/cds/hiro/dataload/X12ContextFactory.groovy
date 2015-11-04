package com.cds.hiro.dataload

import com.cds.hiro.builders.X12Context
import com.cds.hiro.builders.Facility
import com.github.rahulsom.genealogy.Person
import com.github.rahulsom.geocoder.domain.Address

/**
 * Created by rahul on 10/27/15.
 */

class X12ContextFactory {

  Person person
  String dob
  List<Facility> facilities
  String identifier
  Address address

  X12ContextFactory(Person person, String dob, List<Facility> facilities, String identifier, Address address) {
    this.person = person
    this.dob = dob
    this.facilities = facilities
    this.identifier = identifier
    this.address = address
  }

  private static Random rnd = new Random()

  private List<X12Context> _contextList = []
  List<X12Context> getAllContexts() {
    return _contextList
  }

  X12Context getContext() {
    Facility facility = facilities[rnd.nextInt(facilities.size())]
    getContext(facility)
  }

  /**
   * Return a context in the same or different facility.
   * @param facility the facility
   * @param differentFacility if true, the new context will be in a different facility than the one provided.
   * @return
   */
  X12Context getContext(Facility facility, boolean differentFacility) {
    def facilityToUse = facility
    if (differentFacility) {
      facilityToUse = facilities.findAll{it != facility}?.get(rnd.nextInt(facilities.size() - 1))
    }
    getContext(facilityToUse)
  }

  /**
   * return a new context with a new facility which has not been used
   * @return
   */
  X12Context getContextWithNewFacility() {
    def facilitiesNotInUse= facilities.findAll{!_contextList*.facility.contains(it)}
    def facilityToUse = facilitiesNotInUse?.get(rnd.nextInt(facilitiesNotInUse.size()))
    if (!facilityToUse) {
      throw new RuntimeException("Not enough facilities to generate measure. Please increase number of facilities.")
    }
    getContext(facilityToUse)
  }

  X12Context getContext(Facility facility) {
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

      serviceEvent {
        initiatedBy 'Chen', 'Peter'
        id '2.16.840.1.113883.3.771', '1225652938001060'
        at 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771'
      }

      patientWeight 192.3d

      authoredBy 'Johnson', 'Kimberly' of facility.name identifiedAs facility.identifier at new Date().format('yyyyMMdd')
      setFacility facility
    }
    _contextList << context
    context
  }

  public List<X12Context> getContextList() {
    this._contextList
  }
}
