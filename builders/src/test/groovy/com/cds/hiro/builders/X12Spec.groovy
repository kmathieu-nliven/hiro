package com.cds.hiro.builders

import com.cds.hiro.x12.batch.Interchange
import com.github.rahulsom.cda.CD
import com.github.rahulsom.cda.CE
import groovy.transform.CompileStatic
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDateTime

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

        id '1.2.3.4', 'pat_E7064980-7624-4700-9663-A555769DF64F'

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

      diagnosis {
        code SnomedCt('77528005') on '20130130000000'
      }

    }

    def interchange = Interchange.createInterchange([x12], LocalDateTime.parse('2007-12-03T10:15:30'))
    def edi = X12.serialize(interchange, true)

    new File('build/test.edi').text = edi
    def controlValue = '''\
        |ISA*00*          *00*          *ZZ*99999999999    *ZZ*88888888888    *071203*1015*^*00501*000000023*0*T*:
        |  GS*HC*99999999999*88888888888*071203*1015*1234567890*X*005010X222A1
        |  ST*837*1234567890*005010X222A1
        |    BHT*0019*00*1234567890*20071203*0215*CH
        |    REF*D9*1234567890*Claim*
        |    NM1*41*2*Alpine Family Physicians*****46*2.16.840.1.113883.3.771***
        |      PER*IC*Kimberly Johnson*TE*4082361234*****
        |    NM1*40*2*Humana*****46*2.16.840.1.113883.19***
        |    HL*1**20*
        |      NM1*IL*1*Wilson*Paul****MI*pat_E7064980-7624-4700-9663-A555769DF64F***
        |        N3*500 Washington Blvd*
        |        N4*San Jose*CA*95129*USA*H**
        |    HL*2**22*
        |      NM1*IL*1*Wilson*Paul****MI*pat_E7064980-7624-4700-9663-A555769DF64F***
        |    HL*1**23*
        |      PAT*18*O*EO**DT*200712030215**192.3*N
        |      CLM********************
        |        DTP*454*DT*201108070000
        |        DTP*691*DT*201112090000
        |        NM1*1P*1*Chen*Peter****EI*1225652938001060***
        |      CLM********************
        |        DTP*431*DT*201108050000
        |        DTP*450*DT*201112310000
        |        HI*BJ:415.0:::::::***********
        |      CLM********************
        |        DTP*431*DT*201108050000
        |        DTP*450*DT*201112310000
        |        HI*BJ:415.0:::::::***********
        |      CLM********************
        |        DTP*431*DT*201108050000
        |        HI*BJ:724.5:::::::***********
        |    HL*1**23*
        |      CLM********************
        |        DTP*431*DT*201301300000
        |        HI*AAA:77528005:::::::***********
        |    HL*1**23*
        |      CLM********************
        |        LX*1
        |          SV1*CJ:99203:::::Code 99203:********************
        |          DTP*196*DT*201011201000
        |    HL*1**23*
        |      CLM********************
        |        LX*1
        |          SV1*CJ:99203:::::Code 99203:********************
        |          DTP*196*DT*201011200000
        |          DTP*197*DT*201312200000
        |    HL*1**23*
        |      CLM********************
        |        LX*1
        |          SV1*LD:77528005:::::Code 77528005:********************
        |          DTP*196*DT*201011200000
        |          DTP*197*DT*201312200000
        |    SE*51*1234567890
        |  GE*1*1234567890
        |  IEA*1*23
        '''.stripMargin().replaceAll(/\n *$/, '')

    then: "All is well"
    edi == controlValue

  }

}
