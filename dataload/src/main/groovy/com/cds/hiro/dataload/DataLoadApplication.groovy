package com.cds.hiro.dataload

import com.cds.hiro.builders.Cda
import com.cds.hiro.builders.CdaContext
import com.cds.hiro.dataload.measures.MeasureGenerator
import com.github.rahulsom.cda.CD
import com.github.rahulsom.cda.CE
import com.github.rahulsom.genealogy.NameDbUsa
import com.github.rahulsom.genealogy.Person
import com.github.rahulsom.geocoder.coder.GeonamesCoder
import com.github.rahulsom.geocoder.domain.Address
import com.github.rahulsom.geocoder.domain.LatLng
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import groovy.util.logging.Log4j
import org.yaml.snakeyaml.Yaml

import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat

/**
 * Loads Data into HealthLogix based on probabilities
 */
@Log4j
@CompileStatic
class DataLoadApplication {
  static Random rnd = new Random(new SecureRandom().nextLong())


  static CE ce(String code, String codeSystem, String codeSystemName) {
    new CE().withCode(code).withCodeSystem(codeSystem).
        withDisplayName("Code $code").withCodeSystemName(codeSystemName)
  }

  static CD cd(String code, String codeSystem, String codeSystemName) {
    new CD().withCode(code).withCodeSystem(codeSystem).
        withDisplayName("Code $code").withCodeSystemName(codeSystemName)
  }

  static CE LOINC(String input) { ce(input, '2.16.840.1.113883.6.1', 'LOINC') }
  static CE Conf(String input) { ce(input, '2.16.840.1.113883.5.25', 'Confidentiality Codes') }

  private static Address getAddress(ExecutionConfig executionConfig) {
    def ll = new LatLng(32.902071, -117.207741)
    def geocoderName = executionConfig.geonamesUsername
    def g2 = new GeonamesCoder(geocoderName)
    Address addr = null
    while (!addr) {
      addr = g2.decode(new LatLng(ll.lat + rnd.nextGaussian() * 0.05, ll.lng + rnd.nextGaussian() * 0.05))
    }
    addr
  }

  static String generateMD5_A(String s, int len){
    MessageDigest.getInstance("MD5").digest(s.bytes).encodeHex().toString()[0..len-1].toUpperCase()
  }

  static enum MeasureInfo {
    Complement, Compliant, Ignore
  }
  public static void main(String[] args) {
    LogConfig.init()
    log.info "Starting application..."
    ExecutionConfig execCon = readConfig()

    def baymax = new Baymax(execCon.baymax.baseUrl)
    def userId = baymax.login(execCon.baymax.username, execCon.baymax.password)

    def names = NameDbUsa.instance

    List<Facility> facilities = []
    int portNum = execCon.startingPort
    execCon.facilities.
        times { id ->
          def idx = 1000 + id
          def name = names.lastName.toLowerCase().replaceAll(/[^A-Za-z]/, '')
          while (name in facilities*.nickName) {
            name = names.lastName.toLowerCase().replaceAll(/[^A-Za-z]/, '')
          }
          def facility = new Facility(nickName: name, name: "${name} clinic".toString(),
              type: 'Clinic', testFacility: 'on', mllpPort_number: portNum++,
              atnaConfig_enableLocalService: true, atnaConfig_clientPort: portNum++,
              atnaConfig_atnaProtocol: 'TCP', owner_id: userId
          )

          String identifier = baymax.createFacility(facility, idx)
          facility.idx = idx.toString()
          facility.identifier = identifier
          facilities << facility
        }

    execCon.patients.
        times { idx ->
          def facility = facilities[rnd.nextInt(facilities.size())]
          def person = names.person
          def address = getAddress(execCon)
          def dob = (new SimpleDateFormat('yyyyMMdd').parse('19700101') + rnd.nextInt(365 * 80) - 365 * 40).format('yyyyMMdd')
          def idntfr = generateMD5_A("${person.firstName} ${person.lastName} ${dob.format('yyyyMMdd')}", 8)
          log.info "Creating patient ${person.firstName} ${person.lastName} at ${facility.nickName}"

          baymax.createPatient(person, address, dob, idntfr, facility)
          CdaContext cdaContext = createCdaContext(person, dob, facility, idntfr)

          // TODO Create CCD correctly
          execCon.measures.each { measure ->
            def random = Math.abs(rnd.nextGaussian())

            def measureClass = Class.forName("com.cds.hiro.dataload.measures.${measure.name}".toString())
            def measureGenerator = measureClass.newInstance() as MeasureGenerator
            switch (evalMeasure(random, measure)) {
              case MeasureInfo.Compliant:
                log.info "Will apply ${measure} as Compliant for ${idntfr}"
                measureGenerator.applyCompliant(cdaContext); break
              case MeasureInfo.Complement:
                log.info "Will apply ${measure} as Complement for ${idntfr}"
                measureGenerator.applyComplement(cdaContext); break
              default:
                log.debug "Will not apply ${measure} for ${idntfr}"
            }
          }
          def cda = Cda.createCcd(cdaContext)
          baymax.addDocument(cda, facility)
          // println "${'X12'.padLeft(20)} : ${''}"
        }

  }

  static MeasureInfo evalMeasure(double random, ExecutionConfig.Measure measure) {
    if (measure.complement > measure.compliant ) {
      if (random > measure.complement) {
        MeasureInfo.Complement
      } else if (random > measure.compliant) {
        MeasureInfo.Compliant
      } else {
        MeasureInfo.Ignore
      }
    } else {
      if (random > measure.compliant) {
        MeasureInfo.Compliant
      } else if (random > measure.complement) {
        MeasureInfo.Complement
      } else {
        MeasureInfo.Ignore
      }
    }
  }

  private static CdaContext createCdaContext(Person person, String dob, Facility facility, String idntfr) {
    def cdaContext = new CdaContext()
    cdaContext.with {
      code LOINC('34133-9')
      confidentiality Conf('N')
      patient {
        name person.lastName, person.firstName
        gender person.gender
        birthTime dob
        maritalStatus rnd.nextBoolean() ? 'M' : 'S'

        id facility.identifier, idntfr

        addr {
          street '500 Washington Blvd'
          city 'San Jose'
          state 'CA'
          postalCode '95129'
          country 'USA'
        }
      }
      authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'
    }
    cdaContext
  }

  @CompileStatic(TypeCheckingMode.SKIP)
  private static ExecutionConfig readConfig() {
    log.debug "Parsing dataload.yml"
    def obj = new Yaml().load(new File('dataload.yml').newReader())
    def execCon = new ExecutionConfig(obj as Map)
    execCon.measures = execCon.measures.collect { m -> new ExecutionConfig.Measure(m as Map) }
    log.debug "Done parsing dataload.yml"
    execCon
  }
}
