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
class CdaACOMeasuresSpec extends Specification {

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
  def "ACO-15-IPP-and-Denom.json"() {
    when: "A ccd is generated"
    def ccd = Cda.create {
      code LOINC('34133-9')
      confidentiality Conf('N')

      patient {
        name 'Canolas', 'Raja'
        gender 'M'
        birthTime '19450901'
        maritalStatus 'M'

        id '99.1.2', 'ACO15ID'

        addr {
          street '500 Washington Blvd'
          city 'San Jose'
          state 'CA'
          postalCode '95129'
          country 'USA'
        }
      }

      authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'

      performed CPT('99203') from '20150505' to '20150505'
    }
    new File('build/ACO-15-IPP-and-Denom.xml').text = Cda.serialize(ccd, true)

    then: "All is well"
    1 == 1
  }

  def "ACO-15-Numer.json"() {
    when: "A ccd is generated"
    def ccd = Cda.create {
      code LOINC('34133-9')
      confidentiality Conf('N')

      patient {
        name 'Lanes', 'Jo'
        gender 'M'
        birthTime '19450901'
        maritalStatus 'M'

        id '99.1.2', 'ACO15N'

        addr {
          street '500 Washington Blvd'
          city 'San Jose'
          state 'CA'
          postalCode '95129'
          country 'USA'
        }
      }

      authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'

      performed SnomedCt('394678003') from '19980505' to '19980505' withStatus 'PERFORMED'

      performed CPT('99203') from '20150505' to '20150505'
    }
    new File('build/ACO-15-Numer.xml').text = Cda.serialize(ccd, true)

    then: "All is well"
    1 == 1
  }

  def "ACO-13-Numer.json"() {
    when: "A ccd is generated"
    def ccd = Cda.create {
      code LOINC('34133-9')
      confidentiality Conf('N')

      patient {
        name 'Hurds', 'Ricy'
        gender 'M'
        birthTime '19400203'
        maritalStatus 'M'

        id '99.1.2', 'ACO13N'

        addr {
          street '500 Washington Blvd'
          city 'San Jose'
          state 'CA'
          postalCode '95129'
          country 'USA'
        }
      }

      authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'

      performed LOINC('57254-5') from '20151003' to '20151003'

      performed CPT('99341') from '20151003' to '20151003'
    }
    new File('build/ACO-13-Numer.xml').text = Cda.serialize(ccd, true)

    then: "All is well"
    1 == 1
  }

  def "ACO-14-IPP-and-Denom.json"() {
    when: "A ccd is generated"
    def ccd = Cda.create {
      code LOINC('34133-9')
      confidentiality Conf('N')

      patient {
        name 'Grandle', 'Naze'
        gender 'M'
        birthTime '19400203'
        maritalStatus 'M'

        id '99.1.2', 'ACO14ID'

        addr {
          street '500 Washington Blvd'
          city 'San Jose'
          state 'CA'
          postalCode '95129'
          country 'USA'
        }
      }

      authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'

      performed CPT('99341') from '20151003' to '20151003'
      performed CPT('99341') from '20150615' to '20150615'

    }
    new File('build/ACO-14-IPP-and-Denom.xml').text = Cda.serialize(ccd, true)

    then: "All is well"
    1 == 1
  }

  def "ACO-14-Numer.json"() {
    when: "A ccd is generated"
    def ccd = Cda.create {
      code LOINC('34133-9')
      confidentiality Conf('N')

      patient {
        name 'Tachina', 'Honky'
        gender 'M'
        birthTime '19400203'
        maritalStatus 'M'

        id '99.1.2', 'ACO14N'

        addr {
          street '500 Washington Blvd'
          city 'San Jose'
          state 'CA'
          postalCode '95129'
          country 'USA'
        }
      }

      authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'

      performed SnomedCt('12843005') from '20141120' to '20141120'

      performed CPT('99341') from '20151003' to '20151003'
      performed CPT('99341') from '20150615' to '20150615'

      performed SnomedCt('442333005') from '20141120' to '20141120' withStatus 'PERFORMED'
    }
    new File('build/ACO-14-Numer.xml').text = Cda.serialize(ccd, true)

    then: "All is well"
    1 == 1
  }

  def "ACO-16-Numer.json"() {
    when: "A ccd is generated"
    def ccd = Cda.create {
      code LOINC('34133-9')
      confidentiality Conf('N')

      patient {
        name 'Sooth', 'Dadgum'
        gender 'M'
        birthTime '19400203'
        maritalStatus 'M'

        id '99.1.2', 'ACO16N'

        addr {
          street '500 Washington Blvd'
          city 'San Jose'
          state 'CA'
          postalCode '95129'
          country 'USA'
        }
      }

      authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'


      performed CPT('99397') from '20150615' to '20150615'

      results {
        on '20150320'
        measured LOINC('39156-5') at '25 kg/m2' of 'PQ'
      }
    }
    new File('build/ACO-16-Numer.xml').text = Cda.serialize(ccd, true)

    then: "All is well"
    1 == 1
  }

  def "ACO-17-IPP-and-Denom.json"() {
    when: "A ccd is generated"
    def ccd = Cda.create {
      code LOINC('34133-9')
      confidentiality Conf('N')

      patient {
        name 'Riviere', 'Hallian'
        gender 'M'
        birthTime '19400203'
        maritalStatus 'M'

        id '99.1.2', 'ACO17ID'

        addr {
          street '500 Washington Blvd'
          city 'San Jose'
          state 'CA'
          postalCode '95129'
          country 'USA'
        }
      }

      authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'

      performed CPT('99411') from '20150615' to '20150615'

    }
    new File('build/ACO-17-IPP-and-Denom.xml').text = Cda.serialize(ccd, true)

    then: "All is well"
    1 == 1
  }

  def "ACO-17-Numer.json"() {
    when: "A ccd is generated"
    def ccd = Cda.create {
      code LOINC('34133-9')
      confidentiality Conf('N')

      patient {
        name 'Kiddish', 'Craven'
        gender 'M'
        birthTime '19400203'
        maritalStatus 'M'

        id '99.1.2', 'ACO17N'

        addr {
          street '500 Washington Blvd'
          city 'San Jose'
          state 'CA'
          postalCode '95129'
          country 'USA'
        }
      }

      authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'

      performed CPT('99411') from '20150615' to '20150615'

      performed LOINC('68535-4') from '20140820' to '20140820'
      performed SnomedCt('105539002') from '20141120' to '20141120'

    }
    new File('build/ACO-17-Numer.xml').text = Cda.serialize(ccd, true)

    then: "All is well"
    1 == 1
  }

  def "ACO-19-IPP-and-Denom.json"() {
    when: "A ccd is generated"
    def ccd = Cda.create {
      code LOINC('34133-9')
      confidentiality Conf('N')

      patient {
        name 'Tres', 'Hard'
        gender 'M'
        birthTime '19550901'
        maritalStatus 'M'

        id '99.1.2', 'ACO19ID'

        addr {
          street '500 Washington Blvd'
          city 'San Jose'
          state 'CA'
          postalCode '95129'
          country 'USA'
        }
      }

      authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'

      performed CPT('99203') from '20150505' to '20150505'

    }
    new File('build/ACO-19-IPP-and-Denom.xml').text = Cda.serialize(ccd, true)

    then: "All is well"
    1 == 1
  }

  def "ACO-19-IPP-and-Denom-and-DenomEx.json"() {
    when: "A ccd is generated"
    def ccd = Cda.create {
      code LOINC('34133-9')
      confidentiality Conf('N')

      patient {
        name 'Rimes', 'Kith'
        gender 'M'
        birthTime '19550901'
        maritalStatus 'M'

        id '99.1.2', 'ACO19IDX'

        addr {
          street '500 Washington Blvd'
          city 'San Jose'
          state 'CA'
          postalCode '95129'
          country 'USA'
        }
      }

      authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'


      suffered Icd9CM('153.5') between '20080505' and '20080505' withStatus 'ACTIVE'
      performed SnomedCt('446745002') from '20050505' to '20050505' withStatus 'PERFORMED'
      performed CPT('99203') from '20150505' to '20150505'

    }
    new File('build/ACO-19-IPP-and-Denom-and-DenomEx.xml').text = Cda.serialize(ccd, true)

    then: "All is well"
    1 == 1
  }

  def "ACO-19-Numer.json"() {
    when: "A ccd is generated"
    def ccd = Cda.create {
      code LOINC('34133-9')
      confidentiality Conf('N')

      patient {
        name 'Keb', 'Caved'
        gender 'M'
        birthTime '19550901'
        maritalStatus 'M'

        id '99.1.2', 'ACO19N'

        addr {
          street '500 Washington Blvd'
          city 'San Jose'
          state 'CA'
          postalCode '95129'
          country 'USA'
        }
      }

      authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'


      performed CPT('99203') from '20150505' to '20150505'
      performed SnomedCt('446745002') from '20090505' to '20090505' withStatus 'PERFORMED'

    }
    new File('build/ACO-19-Numer.xml').text = Cda.serialize(ccd, true)

    then: "All is well"
    1 == 1
  }

  def "ACO-21-Numer.json"() {
    when: "A ccd is generated"
    def ccd = Cda.create {
      code SnomedCt('103705002')
      confidentiality Conf('N')

      patient {
        name 'Ahoy', 'Aurar'
        gender 'M'
        birthTime '19400901'
        maritalStatus 'M'

        id '99.1.2', 'ACO21N'

        addr {
          street '500 Washington Blvd'
          city 'San Jose'
          state 'CA'
          postalCode '95129'
          country 'USA'
        }
      }

      authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'

      performed SnomedCt('103705002') from '20150615' to '20150615'

      // results group
      results {
        on '20150615'
        measured LOINC('8480-6') at '100 mmHg' of 'PQ' withRange '70-125' was 'High'
      }

      results {
        on '20150615'
        measured LOINC('8462-4') at '60 mmHg' of 'PQ' withRange '70-125' was 'High' withStatus 'PERFORMED'
      }
    }
    new File('build/ACO-21-Numer.xml').text = Cda.serialize(ccd, true)

    then: "All is well"
    1 == 1
  }


  def "ACO-27-IPP-and-Denom.json"() {
    when: "A ccd is generated"
    def ccd = Cda.create {
      code LOINC('34133-9')
      confidentiality Conf('N')

      patient {
        name 'Bill', 'Clinton'
        gender 'M'
        birthTime '19450511'
        maritalStatus 'M'

        id '99.1.2', 'ACO27ID'

        addr {
          street '500 Washington Blvd'
          city 'San Jose'
          state 'CA'
          postalCode '95129'
          country 'USA'
        }
      }

      authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'

      performed SnomedCt('270427003') from '20150511' to '20150511' withStatus 'PERFORMED'
      performed SnomedCt('4783006') from '20141228' to '20150105' withStatus 'ACTIVE'


    }
    new File('build/ACO-27-IPP-and-Denom.xml').text = Cda.serialize(ccd, true)

    then: "All is well"
    1 == 1
  }

  def "ACO-27-Numer.json"() {
    when: "A ccd is generated"
    def ccd = Cda.create {
      code LOINC('34133-9')
      confidentiality Conf('N')

      patient {
        name 'George', 'Clooney'
        gender 'M'
        birthTime '19450511'
        maritalStatus 'M'

        id '99.1.2', 'ACO27N'

        addr {
          street '500 Washington Blvd'
          city 'San Jose'
          state 'CA'
          postalCode '95129'
          country 'USA'
        }
      }

      authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'

      performed SnomedCt('270427003') from '20150511' to '20150511' withStatus 'PERFORMED'
      performed SnomedCt('4783006') from '20141228' to '20150105' withStatus 'ACTIVE'
      performed LOINC('17856-6') from '20150111' to '20150113' withStatus 'PERFORMED'

    }
    new File('build/ACO-27-Numer.xml').text = Cda.serialize(ccd, true)

    then: "All is well"
    1 == 1
  }

  def "ACO-30-IPP-and-Denom.json"() {
    when: "A ccd is generated"
    def ccd = Cda.create {
      code SnomedCt('103705002')
      confidentiality Conf('N')

      patient {
        name 'Piercers', 'Zit'
        gender 'F'
        birthTime '19450511'
        maritalStatus 'M'

        id '99.1.2', 'ACO30ID'

        addr {
          street '500 Washington Blvd'
          city 'San Jose'
          state 'CA'
          postalCode '95129'
          country 'USA'
        }
      }

      authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'

      performed SnomedCt('11101003') from '20150511' to '20150511' withStatus 'PERFORMED'
      performed CPT('99213') from '20150511' to '20150511' withStatus 'PERFORMED'
      performed SnomedCt('52035003') from '20140511' to '20140511' withStatus 'ACTIVE'
      performed SnomedCt('1055001') from '20141230' to '20150111' withStatus 'ACTIVE'

    }
    new File('build/ACO-30-IPP-and-Denom.xml').text = Cda.serialize(ccd, true)

    then: "All is well"
    1 == 1
  }

  def "ACO-30-Numer.json"() {
    when: "A ccd is generated"
    def ccd = Cda.create {
      code SnomedCt('103705002')
      confidentiality Conf('N')

      patient {
        name 'Maki', 'Humite'
        gender 'F'
        birthTime '19450511'
        maritalStatus 'M'

        id '99.1.2', 'ACO30N'

        addr {
          street '500 Washington Blvd'
          city 'San Jose'
          state 'CA'
          postalCode '95129'
          country 'USA'
        }
      }

      authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'

      performed SnomedCt('52035003') from '20140511' to '20140511' withStatus 'ACTIVE'
      performed SnomedCt('11101003') from '20150511' to '20150511' withStatus 'PERFORMED'

      prescribed RxNorm('432389') from '20141228' to '20150211' withStatus 'ACTIVE'
      performed SnomedCt('1055001') from '20141230' to '20150111' withStatus 'ACTIVE'
      performed CPT('99213') from '20150511' to '20150511' withStatus 'PERFORMED'

    }
    new File('build/ACO-30-Numer.xml').text = Cda.serialize(ccd, true)

    then: "All is well"
    1 == 1
  }

  def "ACO-31-Ipp-and-Denom.json"() {
    when: "A ccd is generated"
    def ccd = Cda.create {
      code LOINC('34133-9')
      confidentiality Conf('N')

      patient {
        name 'Riggs', 'Bricky'
        gender 'M'
        birthTime '19700511'
        maritalStatus 'M'

        id '99.1.2', 'ACO31ID'

        addr {
          street '500 Washington Blvd'
          city 'San Jose'
          state 'CA'
          postalCode '95129'
          country 'USA'
        }
      }

      authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'

      performed CPT('99244') from '20150515' to '20150518' withStatus 'PERFORMED'
      performed SnomedCt('981000124106') from '20150516' to '20150517' withStatus 'ACTIVE'
      performed SnomedCt('10091002') from '20150509' to '20150515' withStatus 'PERFORMED'
      performed CPT('99202') from '20150511' to '20150511' withStatus 'PERFORMED'

    }
    new File('build/ACO-31-IPP-and-DENOM.xml').text = Cda.serialize(ccd, true)

    then: "All is well"
    1 == 1
  }

  def "ACO-31-Numer.json"() {
    when: "A ccd is generated"
    def ccd = Cda.create {
      code LOINC('34133-9')
      confidentiality Conf('N')

      patient {
        name 'Laree', 'Kain'
        gender 'M'
        birthTime '19700511'
        maritalStatus 'M'

        id '99.1.2', 'ACO31N'

        addr {
          street '500 Washington Blvd'
          city 'San Jose'
          state 'CA'
          postalCode '95129'
          country 'USA'
        }
      }

      authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'

      performed CPT('99244') from '20150515' to '20150518' withStatus 'PERFORMED'
      performed RxNorm('200031') from '20150516' to '20150517' withStatus 'ACTIVE'
      performed SnomedCt('981000124106') from '20150516' to '20150517' withStatus 'ACTIVE'
      performed SnomedCt('10091002') from '20150509' to '20150515' withStatus 'PERFORMED'
      performed CPT('99202') from '20150510' to '20150515' withStatus 'PERFORMED'

    }
    new File('build/ACO-31-Numer.xml').text = Cda.serialize(ccd, true)

    then: "All is well"
    1 == 1
  }

  def "ACO-39-IPP-and-Denom.json"() {
    when: "A ccd is generated"
    def ccd = Cda.create {
      confidentiality Conf('N')

      patient {
        name 'Birler', 'Newton'
        gender 'M'
        birthTime '19450203'
        maritalStatus 'M'

        id '99.1.2', 'ACO39ID'

        addr {
          street '500 Washington Blvd'
          city 'San Jose'
          state 'CA'
          postalCode '95129'
          country 'USA'
        }
      }

      authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'

      performed CPT('99341') from '20150511' to '20150511' withStatus 'PERFORMED'

    }
    new File('build/ACO-39-IPP-and-Denom.xml').text = Cda.serialize(ccd, true)

    then: "All is well"
    1 == 1
  }

  def "ACO-39-Numer.json"() {
    when: "A ccd is generated"
    def ccd = Cda.create {
      confidentiality Conf('N')

      patient {
        name 'Shin', 'Jai'
        gender 'M'
        birthTime '19450203'
        maritalStatus 'M'

        id '99.1.2', 'ACO39N'

        addr {
          street '500 Washington Blvd'
          city 'San Jose'
          state 'CA'
          postalCode '95129'
          country 'USA'
        }
      }

      authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'

      performed CPT('99203') from '20150505' to '20150505'
      performed SnomedCt('428191000124101') from '20150505' to '20150505' withStatus 'PERFORMED'

    }
    new File('build/ACO-39-Numer.xml').text = Cda.serialize(ccd, true)

    then: "All is well"
    1 == 1
  }

  def "ACO-41-Denom.json"() {
    when: "A ccd is generated"
    def ccd = Cda.create {
      confidentiality Conf('N')

      patient {
        name 'Lecher', 'Spinny'
        gender 'M'
        birthTime '19501106'
        maritalStatus 'M'

        id '99.1.2', 'ACO41D'

        addr {
          street '500 Washington Blvd'
          city 'San Jose'
          state 'CA'
          postalCode '95129'
          country 'USA'
        }
      }

      authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'

      performed CPT('99202') from '20150304' to '20150304'
      suffered Icd9CM('250.03') between '20150304' and '20150304'

    }
    new File('build/ACO-41-Denom.xml').text = Cda.serialize(ccd, true)

    then: "All is well"
    1 == 1
  }

  def "ACO-41-Numer.json"() {
    when: "A ccd is generated"
    def ccd = Cda.create {
      confidentiality Conf('N')

      patient {
        name 'Verdant', 'Rizas'
        gender 'M'
        birthTime '19500203'
        maritalStatus 'M'

        id '99.1.2', 'ACO41N'

        addr {
          street '500 Washington Blvd'
          city 'San Jose'
          state 'CA'
          postalCode '95129'
          country 'USA'
        }
      }

      authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'

      performed CPT('99202') from '20150304' to '20150304'
      performed SnomedCt('252779009') from '20151010' to '20151011'
      suffered Icd9CM('250.03') between '20150304' and '20150304'

    }
    new File('build/ACO-41-Numer.xml').text = Cda.serialize(ccd, true)

    then: "All is well"
    1 == 1
  }

  def "ACO-20-IPP-and-Denom.json"() {
    when: "A ccd is generated"
    def ccd = Cda.create {
      confidentiality Conf('N')

      patient {
        name 'Dreed', 'Trivia'
        gender 'M'
        birthTime '19701106'
        maritalStatus 'M'

        id '99.1.2', 'ACO20ID'

        addr {
          street '500 Washington Blvd'
          city 'San Jose'
          state 'CA'
          postalCode '95129'
          country 'USA'
        }
      }

      authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'

      performed CPT('99397') from '20150511' to '20150512' withStatus 'PERFORMED'

    }
    new File('build/ACO-20-IPP-and-Denom.xml').text = Cda.serialize(ccd, true)

    then: "All is well"
    1 == 1
  }

  def "ACO-20-Numer.json"() {
    when: "A ccd is generated"
    def ccd = Cda.create {
      code LOINC('34133-9')
      confidentiality Conf('N')

      patient {
        name 'Pendu', 'Hermit'
        gender 'M'
        birthTime '19700203'
        maritalStatus 'M'

        id '99.1.2', 'ACO20N'

        addr {
          street '500 Washington Blvd'
          city 'San Jose'
          state 'CA'
          postalCode '95129'
          country 'USA'
        }
      }

      authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'

      performed CPT('99397') from '20150511' to '20150512'
      performed LOINC('46351-3') from '20140511' to '20140513' withStatus 'PERFORMED'

    }
    new File('build/ACO-20-Numer.xml').text = Cda.serialize(ccd, true)

    then: "All is well"
    1 == 1
  }

  def "ACO-18-IPP-and-Denom.json"() {
    when: "A ccd is generated"
    def ccd = Cda.create {
      code LOINC('34133-9')
      confidentiality Conf('N')

      patient {
        name 'Gomers', 'Gorier'
        gender 'F'
        birthTime '19700511'
        maritalStatus 'M'

        id '99.1.2', 'ACO18ID'

        addr {
          street '500 Washington Blvd'
          city 'San Jose'
          state 'CA'
          postalCode '95129'
          country 'USA'
        }
      }

      authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'


      performed SnomedCt('10197000') from '20150511' to '20150513' withStatus 'PERFORMED'

    }
    new File('build/ACO-18-IPP-and-Denom.xml').text = Cda.serialize(ccd, true)

    then: "All is well"
    1 == 1
  }

  def "ACO-18-Numer.json"() {
    when: "A ccd is generated"
    def ccd = Cda.create {
      code LOINC('34133-9')
      confidentiality Conf('N')

      patient {
        name 'Bluewood', 'Darken'
        gender 'F'
        birthTime '20000511'
        maritalStatus 'M'

        id '99.1.2', 'ACO18N'

        addr {
          street '500 Washington Blvd'
          city 'San Jose'
          state 'CA'
          postalCode '95129'
          country 'USA'
        }
      }

      authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'


      performed SnomedCt('10197000') from '20150511' to '20150520' withStatus 'PERFORMED'

      assessed LOINC('73831-0') toBe SnomedCt('428171000124102') on '20150512' withStatus 'PERFORMED'
    }
    new File('build/ACO-18-Numer.xml').text = Cda.serialize(ccd, true)

    then: "All is well"
    1 == 1
  }

  def "ACO-28-IPP-and-Denom.json"() {
    when: "A ccd is generated"
    def ccd = Cda.create {
      confidentiality Conf('N')

      patient {
        name 'Island', 'Gunny'
        gender 'F'
        birthTime '19451106'
        maritalStatus 'M'

        id '99.1.2', 'ACO28ID'

        addr {
          street '500 Washington Blvd'
          city 'San Jose'
          state 'CA'
          postalCode '95129'
          country 'USA'
        }
      }

      authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'

      performed SnomedCt('12843005') from '20150511' to '20150511' withStatus 'PERFORMED'
      performed SnomedCt('12843005') from '20150509' to '20150514' withStatus 'PERFORMED'
      performed SnomedCt('10725009') from '20150511' to '20150515' withStatus 'ACTIVE'

      results {
        on '20150507'
        measured LOINC('8480-6') at '130 mmHg' of 'PQ' withRange '70-125' was 'High' withStatus 'PERFORMED'
      }

      results {
        on '20150507'
        measured LOINC('8462-4') at '80 mmHg' of 'PQ' withRange '70-125' was 'High' withStatus 'PERFORMED'
      }
    }
    new File('build/ACO-28-IPP-and-Denom.xml').text = Cda.serialize(ccd, true)

    then: "All is well"
    1 == 1
  }

  def "ACO-28-Numer.json"() {
    when: "A ccd is generated"
    def ccd = Cda.create {
      confidentiality Conf('N')

      patient {
        name 'More', 'Gent'
        gender 'F'
        birthTime '19451106'
        maritalStatus 'M'

        id '99.1.2', 'ACO28N'

        addr {
          street '500 Washington Blvd'
          city 'San Jose'
          state 'CA'
          postalCode '95129'
          country 'USA'
        }
      }

      authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'

      performed SnomedCt('12843005') from '20150511' to '20150512' withStatus 'PERFORMED'
      performed SnomedCt('12843005') from '20150509' to '20150514' withStatus 'PERFORMED'
      performed SnomedCt('10725009') from '20150511' to '20150515' withStatus 'ACTIVE'

      results {
        on '20150507'
        measured LOINC('8480-6') at '130 mmHg' of 'PQ' withRange '70-125' was 'High' withStatus 'PERFORMED'
      }

      results {
        on '20150510'
        measured LOINC('8462-4') at '80 mmHg' of 'PQ' withRange '70-125' was 'High' withStatus 'PERFORMED'
      }
    }
    new File('build/ACO-28-Numer.xml').text = Cda.serialize(ccd, true)

    then: "All is well"
    1 == 1
  }
}
