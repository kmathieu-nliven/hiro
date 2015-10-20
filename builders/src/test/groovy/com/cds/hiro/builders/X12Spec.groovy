package com.cds.hiro.builders

import com.github.rahulsom.cda.CD
import com.github.rahulsom.cda.CE
import groovy.transform.CompileStatic
import spock.lang.Specification

import java.time.Instant

/**
 * Tests for the {@link X12} class.
 *
 * This class contains several cheats to make testing easy. If you are using Cda to generate tests, feel free to
 * extract these cheats into a util class and use them.
 *
 * In production, though, you are better off generating codes from a credible data source.
 *
 * @author Rahul Somasunderam
 */
class X12Spec extends Specification {
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
  static CD LoincCd(String input) { cd(input, '2.16.840.1.113883.6.1', 'LOINC') }

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
  def "Construct X12"() {
    when: "A ccd is generated"
    def x12 = X12.create {
      created Instant.parse('2007-12-03T10:15:30.00Z')

      id '1234567890'

      authoredBy 'Johnson', 'Kimberly' phone '4082361234' of 'Alpine Family Physicians' \
          identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'

      // payers
      payer 'Humana' identifiedAs '2.16.840.1.113883.19' identifierIs 'HPCG02815-00'

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

      patientWeight 192.3d

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

      // procedures
      performed CPT('99203') on '20101120100000'
      performed CPT('99203') from '20101120' to '20131220'
      performed SnomedCt('77528005') from '20101120' to '20131220' withStatus 'completed'
    }

    def edi = X12.serialize(x12, true)
    new File('build/test.edi').text = edi
    def controlValue = '''\
        |ST*837*1234567890*005010X222A1
        |  BHT*0019*00*1234567890*20071203*0215*CH
        |  REF*D9*1234567890*Claim*
        |  NM1*41*2*20111118014000*****46*2.16.840.1.113883.3.771***
        |    PER*IC*Kimberly Johnson*TE*4082361234*****
        |  NM1*40*2*Humana*****46*2.16.840.1.113883.19***
        |  HL****
        |    NM1*IL*1*Wilson*Paul****MI*42***
        |      N3*500 Washington Blvd*
        |      N4*San Jose*CA*95129*USA*H**
        |    PAT*18*O*EO**DT*200712030215**192.3*N
        |      DTP*454*D8*20110807
        |      DTP*691*D8*20111209
        |      NM1*1P*1*Chen*Peter****EI*1225652938001060***
        |      DTP*431*D8*20110805
        |      DTP*450*D8*20111231
        |      HI*BJ:415.0:::::::***********
        |      DTP*431*D8*20110805
        |      DTP*450*D8*20111231
        |      HI*BJ:415.0:::::::***********
        |      DTP*431*D8*20110805
        |      HI*BJ:724.5:::::::***********
        |        SV1*CJ:99203:::::Code 99203:********************
        |        DTP*196*D8*20101120100000
        |        SV1*CJ:99203:::::Code 99203:********************
        |        DTP*196*D8*20101120
        |        DTP*197*D8*20131220
        |        SV1*LD:77528005:::::Code 77528005:********************
        |        DTP*196*D8*20101120
        |        DTP*197*D8*20131220
        '''.stripMargin().replaceAll(/\n *$/, '')

    then: "All is well"
    edi == controlValue

  }

}
