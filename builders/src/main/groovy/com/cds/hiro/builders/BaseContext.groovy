package com.cds.hiro.builders

import com.cds.hiro.builders.contexts.Author
import com.cds.hiro.builders.contexts.Diagnosis
import com.cds.hiro.builders.contexts.Encounter
import com.cds.hiro.builders.contexts.Patient
import com.cds.hiro.builders.contexts.Payer
import com.cds.hiro.builders.contexts.Problem
import com.cds.hiro.builders.contexts.Procedure
import com.cds.hiro.builders.contexts.ServiceEvent
import com.github.rahulsom.cda.CD
import com.github.rahulsom.cda.CE
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.builder.Builder
import groovy.transform.builder.SimpleStrategy

import java.time.Instant

/**
 * Created by rahul on 10/20/15.
 */
@CompileStatic
@AutoClone
@Builder(builderStrategy = SimpleStrategy, prefix = '')
class BaseContext {
  /** Document timestamp */
  Instant created = Instant.now()

  /** Document Identifier */
  String id = (new Random().nextInt(Integer.MAX_VALUE)).toString()


  Patient patient
  void patient(@DelegatesTo(Patient) Closure closure) {
    closure.resolveStrategy = Closure.DELEGATE_FIRST
    closure.delegate = patient = new Patient()
    closure.call()
  }
  /** Generate the default patient */
  Patient patient() {
    this.patient Patient.defaultPatient()
  }
  Patient patient(Patient patient) {
    this.patient = patient
    this.patient
  }


  Encounter encounter
  void encounter(@DelegatesTo(Encounter) Closure closure) {
    closure.resolveStrategy = Closure.DELEGATE_FIRST
    closure.delegate = encounter = new Encounter()
    closure.call()
  }


  List<Procedure> procedures = []
  Procedure performed(CD code) {
    def procedure = new Procedure().code(code)
    procedures << procedure
    procedure
  }


  Author author
  Author authoredBy(String family, String given) {
    author = new Author().family(family).given(given)
  }


  List<Payer> payers = []
  Payer payer(String name) {
    def payer = new Payer().name(name)
    payers << payer
    payer
  }

  ServiceEvent serviceEvent
  void serviceEvent(@DelegatesTo(ServiceEvent) Closure closure) {
    closure.resolveStrategy = Closure.DELEGATE_FIRST
    serviceEvent = closure.delegate = new ServiceEvent()
    closure.call()
  }


  List<Problem> problems = []
  Problem suffered(CE code) {
    def problem = new Problem().code(code).active(false)
    problems << problem
    return problem
  }
  Problem suffers(CE code) {
    def problem = new Problem().code(code).active(true)
    problems << problem
    return problem
  }


  List<Diagnosis> diagnoses = []
  void diagnosis(@DelegatesTo(Diagnosis) Closure closure) {
    def diagnosis = closure.delegate = new Diagnosis()
    diagnoses << diagnosis
    closure.resolveStrategy = Closure.DELEGATE_FIRST
    closure.call()
  }

  Facility facility
  void setFacility(Facility fac) {
    this.facility = fac
  }

  Facility getFacility() {
    return this.facility
  }
}
