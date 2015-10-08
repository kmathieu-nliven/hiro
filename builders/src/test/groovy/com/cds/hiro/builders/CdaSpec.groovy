package com.cds.hiro.builders

import com.github.rahulsom.cda.CD
import com.github.rahulsom.cda.CE
import groovy.transform.CompileStatic
import spock.lang.Specification

/**
 * Tests for the {@link Cda} class.
 *
 * This class contains several cheats to make testing easy. If you are using Cda to generate tests, feel free to
 * extract these cheats into a util class and use them.
 *
 * In production, though, you are better off generating codes from a credible data source.
 *
 * @author Rahul Somasunderam
 */
class CdaSpec extends Specification {

  /*
   * Begin cheats
   */
  static ce(String code, String codeSystem, String codeSystemName) {
    new CE().withCode(code).withCodeSystem(codeSystem).
        withDisplayName("Code $code").withCodeSystemName(codeSystemName)
  }

  static cd(String code, String codeSystem, String codeSystemName) {
    new CD().withCode(code).withCodeSystem(codeSystem).
        withDisplayName("Code $code").withCodeSystemName(codeSystemName)
  }

  static CE LOINC(String input) { ce(input, '2.16.840.1.113883.6.1', 'LOINC') }

  static CE RxNorm(String input) { ce(input, '2.16.840.1.113883.6.88', 'RxNorm') }

  static CE SnomedCt(String input) { ce(input, '2.16.840.1.113883.6.96', 'SNOMED CT') }
  static CD SnomedCtCd(String input) { cd(input, '2.16.840.1.113883.6.96', 'SNOMED CT') }

  static CE Icd9CM(String input) { ce(input, '2.16.840.1.113883.6.103', 'ICD9 CM') }

  static CE CVX(String input) { ce(input, '2.16.840.1.113883.12.292', 'CVX') }

  static CE RouteOfAdministration(String input) { ce(input, '2.16.840.1.113883.5.112', 'RouteOfAdministration') }

  static CE ICNP(String input) { ce(input, '2.16.840.1.113883.6.97', 'ICNP') }

  static CE Conf(String input) { ce(input, '2.16.840.1.113883.5.25', 'Confidentiality Codes') }

  static CD CPT(String input) { cd(input, '2.16.840.1.113883.5.25', 'CPT') }
  /*
   * End cheats
   */

  @CompileStatic
  def "Construct CCD"() {
    when: "A ccd is generated"
    def ccd = Cda.create {
      code LOINC('34133-9')
      confidentiality Conf('N')

      patient {
        name 'Wilson', 'Paul'
        gender 'M'
        birthTime '19520413'
        maritalStatus 'M'

        id '1.2.3.4', '42'

        addr {
          street '500 Washington Blvd'
          city 'San Jose'
          state 'CA'
          postalCode '95129'
          country 'USA'
        }
      }

      authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'
      informant 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771'
      custodian 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771'

      serviceEvent {
        initiatedBy 'Chen', 'Peter'
        id '2.16.840.1.113883.3.771', '1225652938001060'
        at 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771'
      }

      encounter {
        met 'Chen', 'Peter'
        at 'Alpine Family Physicians'
        between '20110807' and '20111209'
      }

      // problems
      suffered Icd9CM('415.0') between '20110805' and '20111231'
      suffered Icd9CM('415.0') between '20110805' and '20111231' withStatus 'ACTIVE'
      suffers Icd9CM('724.5') since '20110805'

      // Family History
      familyMember SnomedCt('65656005') condition 'Survived into her 90s'

      // Social History
      social SnomedCt('229819007') status 'never'

      // Medications
      prescribed RxNorm('123') from '...' to '...'
      prescribed RxNorm('123') from '...' to '...' withStatus 'ACTIVE'

      // immunizations
      immunized CVX('88') by RouteOfAdministration('IM') on '199911'
      immunized CVX('27') by RouteOfAdministration('IM') on '199911' withStatus 'completed'

      // procedures
      performed CPT('99203') on '20101120100000'
      performed CPT('99203') from '20101120' to '20131220'
      performed SnomedCt('77528005') from '20101120' to '20131220' withStatus 'completed'

      // Past Medical History
      diagnosis {
        code SnomedCt('77528005')
        by 'Healthy', 'Bruce'
        at 'Community General Hospital'
        identifiedAs '2.16.840.1.113883.4.6', '1111111111'
        on '20130130000000'
      }

      // vitals
      vitals {
        on '20111209'

        measured ICNP('27113002') at '72 in' of 'PQ'
        measured ICNP('60621009') at '22.83 kg/m2' of 'RTO_PQ_PQ'
      }

      // results group
      results {
        on '20111209'
        measured LOINC('2339-0') at '143 mg/dL' of 'PQ' withRange '70-125' was 'High'
      }

      // allergies
      allergen RxNorm('70618') causes SnomedCt('247472004')

      // payers
      payer 'Humana' identifiedAs '2.16.840.1.113883.19' identifierIs 'HPCG02815-00'
    }
    new File('build/test.xml').text = Cda.serialize(ccd, true)

    then: "All is well"
    1 == 1

  }

  def "John Doe has never smoked"() {
    when: "A ccd is generated"
    def ccd = Cda.create {
      code LOINC('34133-9')
      confidentiality Conf('N')
      patient()

      social SnomedCt('229819007') status 'never'
    }
    new File('build/never-smoked.xml').text = Cda.serialize(ccd, true)

    then: "All is well"
    1 == 1
  }

  def "Jane Doe has never smoked"() {
    when: "A ccd is generated"
    def ccd = Cda.create {
      code LOINC('34133-9')
      confidentiality Conf('N')
      patient().given 'Jane' gender 'F'

      social SnomedCtCd('229819007') status 'never'
    }
    new File('build/jane-doe.xml').text = Cda.serialize(ccd, true)

    then: "All is well"
    1 == 1
  }
}
