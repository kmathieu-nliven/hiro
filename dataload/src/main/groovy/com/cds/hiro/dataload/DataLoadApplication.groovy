package com.cds.hiro.dataload

import com.cds.hiro.builders.Cda
import com.cds.hiro.builders.CdaContext
import com.cds.hiro.builders.X12
import com.cds.hiro.builders.X12Context
import com.cds.hiro.dataload.measures.MeasureGenerator
import com.cds.hiro.x12.EdiParser
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
  public static final int YEAR = 365


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
    def ll = new LatLng(executionConfig.lat, executionConfig.lng)
    def geocoderName = executionConfig.geonamesUsername
    def g2 = new GeonamesCoder(geocoderName)
    Address addr = null
    while (!addr) {
      addr = g2.decode(new LatLng(ll.lat + rnd.nextGaussian() * 0.05, ll.lng + rnd.nextGaussian() * 0.05))
    }
    addr
  }

  static String generateMD5_A(String s, int len) {
    MessageDigest.getInstance("MD5").digest(s.bytes).encodeHex().toString()[0..len - 1].toUpperCase()
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

    def facilitiesFile = new File('build/facilities.csv').
        with {
          text = ''
          newWriter()
        }
    def patientsFile = new File('build/patients.csv').
        with {
          text = ''
          newWriter()
        }

    facilitiesFile.println(['name', 'nsid', 'uid', 'uidtype'].collect { $/"${it}"/$ }.join(','))
    patientsFile.println([
        'local.id', 'local.nsid', 'local.uid', 'local.uidtype',
        'aco.id', 'aco.nsid', 'aco.uid', 'aco.uidtype',
        'firstName', 'lastName', 'gender', 'dob',
        'measures'
    ].collect { $/"${it}"/$ }.join(','))
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

          String identifier = baymax.createFacility(facility, idx, execCon.aco)
          facility.idx = idx.toString()
          facility.identifier = identifier
          facilities << facility
          facilitiesFile.println([name, facility.idx, facility.identifier, 'ISO'].collect { $/"${it}"/$ }.join(','))

        }

    execCon.patients.
        times { idx ->
          def facility = facilities[rnd.nextInt(facilities.size())]
          def person = names.person
          def address = getAddress(execCon)
          def dob = (new SimpleDateFormat('yyyyMMdd').parse('19700101') + rnd.nextInt(YEAR * 80) - YEAR * 40).format('yyyyMMdd')
          def localId = generateMD5_A("${person.firstName} ${person.lastName} ${dob.format('yyyyMMdd')}", 8)
          def acoId = generateMD5_A("${person.firstName} ${person.lastName} ${dob.format('yyyyMMdd')} ${execCon.aco.universalId}", 8)
          log.info "Creating patient ${person.firstName} ${person.lastName} at ${facility.nickName} as ${localId}"

          baymax.createPatient(person, address, dob, localId, facility, execCon.aco, acoId)
          CdaContext cdaContext = createCdaContext(person, dob, facility, localId, address)
          X12Context x12Context = createX12Context(person, dob, facility, localId, address)

          def measures = ''

          execCon.measures.
              each { measure ->
                def random = Math.abs(rnd.nextGaussian())

                def measureClass = Class.forName("com.cds.hiro.dataload.measures.${measure.name}".toString())
                def measureGenerator = measureClass.newInstance() as MeasureGenerator

                switch (evalMeasure(random, measure)) {
                  case MeasureInfo.Compliant:
                    if (measureGenerator.applyCompliant(cdaContext, x12Context)) {
                      log.info "${measure.name.padLeft(20)} : + Compliant"
                      measures += "+${measure.name} "
                    } else {
                      log.info "${measure.name.padLeft(20)} : + Compliant *** Not Applied ***"
                    }
                    break
                  case MeasureInfo.Complement:
                    if (measureGenerator.applyComplement(cdaContext, x12Context)) {
                      log.info "${measure.name.padLeft(20)} : - Complement"
                      measures += "-${measure.name} "
                    } else {
                      log.info "${measure.name.padLeft(20)} : - Complement *** Not Applied ***"
                    }
                    break
                  default:
                    log.debug "Will not apply ${measure} for ${localId}"
                }

              }

          patientsFile.println([
              localId, facility.idx, facility.identifier, 'ISO',
              acoId, execCon.aco.namespaceId, execCon.aco.universalId, 'ISO',
              person.firstName, person.lastName, person.gender, dob,
              measures.trim()
          ].collect { $/"${it}"/$ }.join(','))

          def cda = Cda.createCcd(cdaContext)
          baymax.addDocument(cda, facility)
          def x12 = X12.createX12(x12Context)
          new File("build/${acoId}.edi").text = new EdiParser().toEdi(x12.toTokens(0))
        }

    patientsFile.close()
    facilitiesFile.close()
  }

  static MeasureInfo evalMeasure(double random, ExecutionConfig.Measure measure) {
    if (measure.complement > measure.compliant) {
      random > measure.complement ? MeasureInfo.Complement :
          random > measure.compliant ? MeasureInfo.Compliant :
              MeasureInfo.Ignore
    } else {
      random > measure.compliant ? MeasureInfo.Compliant :
          random > measure.complement ? MeasureInfo.Complement :
              MeasureInfo.Ignore
    }
  }

  private static CdaContext createCdaContext(
      Person person, String dob, Facility facility, String identifier, Address address
  ) {
    def cdaContext = new CdaContext()
    cdaContext.with {
      code LOINC('34133-9')
      confidentiality Conf('N')
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
    cdaContext
  }

  private static X12Context createX12Context(
      Person person, String dob, Facility facility, String identifier, Address address
  ) {
    def cdaContext = new X12Context()
    cdaContext.with {
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
