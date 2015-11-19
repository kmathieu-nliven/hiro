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
class CdaACO15MeasuresSpec extends Specification {

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
    //Test For Above 65 at Measurement Period and Encounter, Performed: Office Visit
    def "ACO-15-BirthDateAbove65PerformedOV.json"() {
        when: "A ccd is generated"
        def ccd = Cda.create {
            code LOINC('34133-9')
            confidentiality Conf('N')

            patient {
                name 'Joe', 'Michael'
                gender 'M'
                birthTime '19451119'
                maritalStatus 'M'

                //id '99.1.2', 'ACO15N'
                id '1.2.3.4', 'ACO15'

                addr {
                    street '500 Washington Blvd'
                    city 'San Jose'
                    state 'CA'
                    postalCode '95129'
                    country 'USA'
                }
            }

            authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'

            performed CVX('133') from '20120605' to '20120605' withStatus 'ADMINISTERED'

            performed CPT('99201') from '20120505' to '20120505' withStatus 'PERFORMED'
        }
        new File('build/ACO-15-BirthDateAbove65PerformedOV.xml').text = Cda.serialize(ccd, true)

        then: "All is well"
        1 == 1
    }

    //Test For Above 65 at Measurement Period and Encounter, Performed: Face-to-Face Interaction
    def "ACO-15-BirthDateAbove65PerformedFFI.json"() {
        when: "A ccd is generated"
        def ccd = Cda.create {
            code LOINC('34133-9')
            confidentiality Conf('N')

            patient {
                name 'Rider', 'Jackson'
                gender 'M'
                birthTime '19451119'
                maritalStatus 'M'

                //id '99.1.2', 'ACO15N'
                id '1.2.3.4', 'ACO15'

                addr {
                    street '500 Washington Blvd'
                    city 'San Jose'
                    state 'CA'
                    postalCode '95129'
                    country 'USA'
                }
            }

            authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'

            performed SnomedCt('12843005') from '20120505' to '20120505' withStatus 'PERFORMED'

            performed CVX('133') from '20120605' to '20120605' withStatus 'ADMINISTERED'
        }
        new File('build/ACO-15-BirthDateAbove65PerformedFFI.xml').text = Cda.serialize(ccd, true)

        then: "All is well"
        1 == 1
    }

    //Test For Above 65 at Measurement Period and Encounter, Performed: Annual Wellness Visit
    def "ACO-15-BirthDateAbove65PerformedAWV.json"() {
        when: "A ccd is generated"
        def ccd = Cda.create {
            code LOINC('34133-9')
            confidentiality Conf('N')

            patient {
                name 'Ebbert', 'Terry'
                gender 'M'
                birthTime '19451119'
                maritalStatus 'M'

                //id '99.1.2', 'ACO15N'
                id '1.2.3.4', 'ACO15'

                addr {
                    street '500 Washington Blvd'
                    city 'San Jose'
                    state 'CA'
                    postalCode '95129'
                    country 'USA'
                }
            }

            authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'

            performed CPT('G0438') from '20120505' to '20120505' withStatus 'PERFORMED'

            performed CVX('133') from '20120605' to '20120605' withStatus 'ADMINISTERED'
        }
        new File('build/ACO-15-BirthDateAbove65PerformedAWV.xml').text = Cda.serialize(ccd, true)

        then: "All is well"
        1 == 1
    }

    //Test For Above 65 at Measurement Period and Encounter, Performed: Home HealthCare Services
    def "ACO-15-BirthDateAbove65PerformedHHS.json"() {
        when: "A ccd is generated"
        def ccd = Cda.create {
            code LOINC('34133-9')
            confidentiality Conf('N')

            patient {
                name 'Jones', 'Justin'
                gender 'M'
                birthTime '19451119'
                maritalStatus 'M'

                //id '99.1.2', 'ACO15N'
                id '1.2.3.4', 'ACO15'

                addr {
                    street '500 Washington Blvd'
                    city 'San Jose'
                    state 'CA'
                    postalCode '95129'
                    country 'USA'
                }
            }

            authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'

            performed CPT('99341') from '20120505' to '20120505' withStatus 'PERFORMED'

            performed CVX('133') from '20120605' to '20120605' withStatus 'ADMINISTERED'
        }
        new File('build/ACO-15-BirthDateAbove65PerformedHHS.xml').text = Cda.serialize(ccd, true)

        then: "All is well"
        1 == 1
    }

    //Test For Above 65 at Measurement Period and Encounter, Performed: Preventive Care Services - Established Office Visit, 18 and Up
    def "ACO-15-BirthDateAbove65PerformedPCSEOV.json"() {
        when: "A ccd is generated"
        def ccd = Cda.create {
            code LOINC('34133-9')
            confidentiality Conf('N')

            patient {
                name 'Thompson', 'David'
                gender 'M'
                birthTime '19451119'
                maritalStatus 'M'

                //id '99.1.2', 'ACO15N'
                id '1.2.3.4', 'ACO15'

                addr {
                    street '500 Washington Blvd'
                    city 'San Jose'
                    state 'CA'
                    postalCode '95129'
                    country 'USA'
                }
            }

            authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'

            performed CPT('99395') from '20120505' to '20120505' withStatus 'PERFORMED'

            performed CVX('133') from '20120605' to '20120605' withStatus 'ADMINISTERED'
        }
        new File('build/ACO-15-BirthDateAbove65PerformedPCSEOV.xml').text = Cda.serialize(ccd, true)

        then: "All is well"
        1 == 1
    }

    //Test For Above 65 at Measurement Period and Encounter, Performed: Preventive Care Services-Initial Office Visit, 18 and Up
    def "ACO-15-BirthDateAbove65PerformedPCSIOV.json"() {
        when: "A ccd is generated"
        def ccd = Cda.create {
            code LOINC('34133-9')
            confidentiality Conf('N')

            patient {
                name 'Smith', 'Luis'
                gender 'M'
                birthTime '19451119'
                maritalStatus 'M'

                //id '99.1.2', 'ACO15N'
                id '1.2.3.4', 'ACO15'

                addr {
                    street '500 Washington Blvd'
                    city 'San Jose'
                    state 'CA'
                    postalCode '95129'
                    country 'USA'
                }
            }

            authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'

            performed CPT('99385') from '20120505' to '20120505' withStatus 'PERFORMED'

            performed CVX('133') from '20120605' to '20120605' withStatus 'ADMINISTERED'
        }
        new File('build/ACO-15-BirthDateAbove65PerformedPCSIOV.xml').text = Cda.serialize(ccd, true)

        then: "All is well"
        1 == 1
    }

    //NUMERATOR: Test For Above 65 at Measurement Period and Procedure, Performed: Pneumococcal Vaccine Administered
    def "ACO-15-BirthDateAbove65PerformedPVA.json"() {
        when: "A ccd is generated"
        def ccd = Cda.create {
            code LOINC('34133-9')
            confidentiality Conf('N')

            patient {
                name 'Donovan', 'Billy'
                gender 'M'
                birthTime '19451119'
                maritalStatus 'M'

                //id '99.1.2', 'ACO15N'
                id '1.2.3.4', 'ACO15'

                addr {
                    street '500 Washington Blvd'
                    city 'San Jose'
                    state 'CA'
                    postalCode '95129'
                    country 'USA'
                }
            }

            authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'

            performed CPT('99385') from '20120505' to '20120505' withStatus 'PERFORMED'

            performed SnomedCt('12866006') from '20120505' to '20120505' withStatus 'PERFORMED'
        }
        new File('build/ACO-15-BirthDateAbove65PerformedPVA.xml').text = Cda.serialize(ccd, true)

        then: "All is well"
        1 == 1
    }

    //NUMERATOR: Test For Above 65 at Measurement Period and Procedure, Performed: Pneumococcal Vaccine Administered
    def "ACO-15-BirthDateAbove65PerformedPVA2.json"() {
        when: "A ccd is generated"
        def ccd = Cda.create {
            code LOINC('34133-9')
            confidentiality Conf('N')

            patient {
                name 'Johnson', 'Philip'
                gender 'M'
                birthTime '19451119'
                maritalStatus 'M'

                //id '99.1.2', 'ACO15N'
                id '1.2.3.4', 'ACO15'

                addr {
                    street '500 Washington Blvd'
                    city 'San Jose'
                    state 'CA'
                    postalCode '95129'
                    country 'USA'
                }
            }

            authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'

            performed CPT('99385') from '20120505' to '20120505' withStatus 'PERFORMED'

            performed SnomedCt('473165003') from '20120505' to '20120505' withStatus 'PERFORMED'
        }
        new File('build/ACO-15-BirthDateAbove65PerformedPVA2.xml').text = Cda.serialize(ccd, true)

        then: "All is well"
        1 == 1
    }

    //Test For Age =65  IPP=0, denom=0, num=0
    def "ACO-15-BirthDateEqual65-IPP0-DENOM0-NUM0.json"() {
        when: "A ccd is generated"
        def ccd = Cda.create {
            code LOINC('34133-9')
            confidentiality Conf('N')

            patient {
                name 'Peterson', 'Robert'
                gender 'M'
                birthTime '19470101'
                maritalStatus 'M'

                //id '99.1.2', 'ACO15N'
                id '1.2.3.4', 'ACO15'

                addr {
                    street '500 Washington Blvd'
                    city 'San Jose'
                    state 'CA'
                    postalCode '95129'
                    country 'USA'
                }
            }

            authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'

            performed CPT('99385') from '20120505' to '20120505' withStatus 'PERFORMED'

            performed SnomedCt('473165003') from '20120605' to '20120605' withStatus 'PERFORMED'
        }
        new File('build/ACO-15-BirthDateEqual65-IPP0-DENOM0-NUM0.xml').text = Cda.serialize(ccd, true)

        then: "All is well"
        1 == 1
    }

    //Test For Age <65  IPP=0, denom=0, num=0
    def "ACO-15-BirthDateLessThan65-IPP0-DENOM0-NUM0.json"() {
        when: "A ccd is generated"
        def ccd = Cda.create {
            code LOINC('34133-9')
            confidentiality Conf('N')

            patient {
                name 'Wheaton', 'Will'
                gender 'M'
                birthTime '19480101'
                maritalStatus 'M'

                //id '99.1.2', 'ACO15N'
                id '1.2.3.4', 'ACO15'

                addr {
                    street '500 Washington Blvd'
                    city 'San Jose'
                    state 'CA'
                    postalCode '95129'
                    country 'USA'
                }
            }

            authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'

            performed CPT('99385') from '20120505' to '20120505' withStatus 'PERFORMED'

            performed SnomedCt('473165003') from '20120605' to '20120605' withStatus 'PERFORMED'
        }
        new File('build/ACO-15-BirthDateLessThan65-IPP0-DENOM0-NUM0.xml').text = Cda.serialize(ccd, true)

        then: "All is well"
        1 == 1
    }

    //Test For Age <65  IPP=0, denom=0, num=0
    //one Day Before Measure Period
    def "ACO-15-BirthDateLessThan65-IPP0-DENOM0-NUM0-BeforeMP.json"() {
        when: "A ccd is generated"
        def ccd = Cda.create {
            code LOINC('34133-9')
            confidentiality Conf('N')

            patient {
                name 'Thompson', 'Sheldon'
                gender 'M'
                birthTime '19551119'
                maritalStatus 'M'

                //id '99.1.2', 'ACO15N'
                id '1.2.3.4', 'ACO15'

                addr {
                    street '500 Washington Blvd'
                    city 'San Jose'
                    state 'CA'
                    postalCode '95129'
                    country 'USA'
                }
            }

            authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'

            //Test For Above 65 one day before Measure Period and Encounter, Performed: Office Visit
            performed CPT('99201') from '20111225' to '20111226' withStatus 'PERFORMED'

            performed SnomedCt('473165003') from '20120605' to '20120605' withStatus 'PERFORMED'
        }
        new File('build/ACO-15-BirthDateLessThan65-IPP0-DENOM0-NUM0-BeforeMP.xml').text = Cda.serialize(ccd, true)

        then: "All is well"
        1 == 1
    }

    //Test For Age <65  IPP=0, denom=0, num=0
    //one Day After Measure Period
    def "ACO-15-BirthDateLessThan65-IPP0-DENOM0-NUM0-AfterMP.json"() {
        when: "A ccd is generated"
        def ccd = Cda.create {
            code LOINC('34133-9')
            confidentiality Conf('N')

            patient {
                name 'Scott', 'Leonard'
                gender 'M'
                birthTime '19551119'
                maritalStatus 'M'

                //id '99.1.2', 'ACO15N'
                id '1.2.3.4', 'ACO15'

                addr {
                    street '500 Washington Blvd'
                    city 'San Jose'
                    state 'CA'
                    postalCode '95129'
                    country 'USA'
                }
            }

            authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'

            //Test For Above 65 one day After Measure Period and Encounter, Performed: Office Visit
            performed CPT('99201') from '20130101' to '20130112' withStatus 'PERFORMED'

            performed SnomedCt('473165003') from '20120605' to '20120605' withStatus 'PERFORMED'
        }
        new File('build/ACO-15-BirthDateLessThan65-IPP0-DENOM0-NUM0-AfterMP.xml').text = Cda.serialize(ccd, true)

        then: "All is well"
        1 == 1
    }

    //Test For Age <65  IPP=0, denom=0, num=0
    //one Day Before Measure Period of Face to Face Interaction
    def "ACO-15-BirthDateLessThan65-IPP0-DENOM0-NUM0-BeforeMP-FFI.json"() {
        when: "A ccd is generated"
        def ccd = Cda.create {
            code LOINC('34133-9')
            confidentiality Conf('N')

            patient {
                name 'Fischer', 'Anthony'
                gender 'M'
                birthTime '19551119'
                maritalStatus 'M'

                //id '99.1.2', 'ACO15N'
                id '1.2.3.4', 'ACO15'

                addr {
                    street '500 Washington Blvd'
                    city 'San Jose'
                    state 'CA'
                    postalCode '95129'
                    country 'USA'
                }
            }

            authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'

            //Test For Below 65 Before Measurement Period and Encounter, Performed: Face-to-Face Interaction
            performed SnomedCt('12843005') from '20111229' to '20111229' withStatus 'PERFORMED'

            //Test For Below 65 at Measurement Period and "Risk Category Assessment: History of Pneumococcal Vaccine"
            performed SnomedCt('473165003') from '20120605' to '20120605' withStatus 'PERFORMED'
        }
        new File('build/ACO-15-BirthDateLessThan65-IPP0-DENOM0-NUM0-BeforeMP-FFI.xml').text = Cda.serialize(ccd, true)

        then: "All is well"
        1 == 1
    }

    //Test For Age <65  IPP=0, denom=0, num=0
    //one Day After Measure Period of Face to Face Interaction
    def "ACO-15-BirthDateLessThan65-IPP0-DENOM0-NUM0-AfterMP-FFI.json"() {
        when: "A ccd is generated"
        def ccd = Cda.create {
            code LOINC('34133-9')
            confidentiality Conf('N')

            patient {
                name 'Corso', 'Lee'
                gender 'M'
                birthTime '19551119'
                maritalStatus 'M'

                //id '99.1.2', 'ACO15N'
                id '1.2.3.4', 'ACO15'

                addr {
                    street '500 Washington Blvd'
                    city 'San Jose'
                    state 'CA'
                    postalCode '95129'
                    country 'USA'
                }
            }

            authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'

            //Test For Below 65 After Measurement Period and Encounter, Performed: Face-to-Face Interaction
            performed SnomedCt('12843005') from '20130101' to '20130101' withStatus 'PERFORMED'

            //Test For Below 65 at Measurement Period and "Risk Category Assessment: History of Pneumococcal Vaccine"
            performed SnomedCt('473165003') from '20120605' to '20120605' withStatus 'PERFORMED'
        }
        new File('build/ACO-15-BirthDateLessThan65-IPP0-DENOM0-NUM0-AfterMP-FFI.xml').text = Cda.serialize(ccd, true)

        then: "All is well"
        1 == 1
    }

    //Test For Age <65  IPP=0, denom=0, num=0
    //one Day Before Measure Period of Annual Wellness Visit Interaction
    def "ACO-15-BirthDateLessThan65-IPP0-DENOM0-NUM0-BeforeMP-AWVI.json"() {
        when: "A ccd is generated"
        def ccd = Cda.create {
            code LOINC('34133-9')
            confidentiality Conf('N')

            patient {
                name 'Curry', 'Donald'
                gender 'M'
                birthTime '19551119'
                maritalStatus 'M'

                //id '99.1.2', 'ACO15N'
                id '1.2.3.4', 'ACO15'

                addr {
                    street '500 Washington Blvd'
                    city 'San Jose'
                    state 'CA'
                    postalCode '95129'
                    country 'USA'
                }
            }

            authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'

            //Test for one Day Before Measure Period of Annual Wellness Visit Interaction
            performed CPT('G0438') from '20111229' to '20111229' withStatus 'PERFORMED'

            //Test For Below 65 at Measurement Period and "Risk Category Assessment: History of Pneumococcal Vaccine"
            performed SnomedCt('473165003') from '20120605' to '20120605' withStatus 'PERFORMED'
        }
        new File('build/ACO-15-BirthDateLessThan65-IPP0-DENOM0-NUM0-BeforeMP-AWVI.xml').text = Cda.serialize(ccd, true)

        then: "All is well"
        1 == 1
    }

    //Test For Age <65  IPP=0, denom=0, num=0
    //one Day After Measure Period of Annual Wellness Visit Interaction
    def "ACO-15-BirthDateLessThan65-IPP0-DENOM0-NUM0-AfterMP-AWVI.json"() {
        when: "A ccd is generated"
        def ccd = Cda.create {
            code LOINC('34133-9')
            confidentiality Conf('N')

            patient {
                name 'Busser', 'Jerry'
                gender 'M'
                birthTime '19551119'
                maritalStatus 'M'

                //id '99.1.2', 'ACO15N'
                id '1.2.3.4', 'ACO15'

                addr {
                    street '500 Washington Blvd'
                    city 'San Jose'
                    state 'CA'
                    postalCode '95129'
                    country 'USA'
                }
            }

            authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'

            //Test for one Day After Measure Period of Annual Wellness Visit Interaction
            performed CPT('G0438') from '20130101' to '20130101' withStatus 'PERFORMED'

            //Test For Below 65 at Measurement Period and "Risk Category Assessment: History of Pneumococcal Vaccine"
            performed SnomedCt('473165003') from '20120605' to '20120605' withStatus 'PERFORMED'
        }
        new File('build/ACO-15-BirthDateLessThan65-IPP0-DENOM0-NUM0-AfterMP-AWVI.xml').text = Cda.serialize(ccd, true)

        then: "All is well"
        1 == 1
    }

    //Test For Age <65  IPP=0, denom=0, num=0
    //one Day Before Measure Period of Encounter, Performed: Home Healthcare Services
    def "ACO-15-BirthDateLessThan65-IPP0-DENOM0-NUM0-BeforeMP-HHS.json"() {
        when: "A ccd is generated"
        def ccd = Cda.create {
            code LOINC('34133-9')
            confidentiality Conf('N')

            patient {
                name 'Cruise', 'Thomas'
                gender 'M'
                birthTime '19551119'
                maritalStatus 'M'

                //id '99.1.2', 'ACO15N'
                id '1.2.3.4', 'ACO15'

                addr {
                    street '500 Washington Blvd'
                    city 'San Jose'
                    state 'CA'
                    postalCode '95129'
                    country 'USA'
                }
            }

            authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'

            //Test For Below 65 Before Measurement Period and Encounter, Performed: Home HealthCare Services
            performed CPT('99341') from '20111229' to '20111229' withStatus 'PERFORMED'

            //Test For Below 65 at Measurement Period and "Risk Category Assessment: History of Pneumococcal Vaccine"
            performed SnomedCt('473165003') from '20120605' to '20120605' withStatus 'PERFORMED'
        }
        new File('build/ACO-15-BirthDateLessThan65-IPP0-DENOM0-NUM0-BeforeMP-HHS.xml').text = Cda.serialize(ccd, true)

        then: "All is well"
        1 == 1
    }

    //Test For Age <65  IPP=0, denom=0, num=0
    //one Day After Measure Period of Encounter, Performed: Home Healthcare Services
    def "ACO-15-BirthDateLessThan65-IPP0-DENOM0-NUM0-AfterMP-HHS.json"() {
        when: "A ccd is generated"
        def ccd = Cda.create {
            code LOINC('34133-9')
            confidentiality Conf('N')

            patient {
                name 'Jones', 'James'
                gender 'M'
                birthTime '19551119'
                maritalStatus 'M'

                //id '99.1.2', 'ACO15N'
                id '1.2.3.4', 'ACO15'

                addr {
                    street '500 Washington Blvd'
                    city 'San Jose'
                    state 'CA'
                    postalCode '95129'
                    country 'USA'
                }
            }

            authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'

            //Test For Below 65 After Measurement Period and Encounter, Performed: Home HealthCare Services
            performed CPT('99341') from '20130101' to '20130101' withStatus 'PERFORMED'

            //Test For Below 65 at Measurement Period and "Risk Category Assessment: History of Pneumococcal Vaccine"
            performed SnomedCt('473165003') from '20120605' to '20120605' withStatus 'PERFORMED'
        }
        new File('build/ACO-15-BirthDateLessThan65-IPP0-DENOM0-NUM0-AfterMP-HHS.xml').text = Cda.serialize(ccd, true)

        then: "All is well"
        1 == 1
    }

    //Test For Age <65  IPP=0, denom=0, num=0
    //one Day Before Measure Period of Encounter, Performed: Preventive Care Services-Estabilished Office Visit, 18 and Up
    def "ACO-15-BirthDateLessThan65-IPP0-DENOM0-NUM0-BeforeMP-PCSEOV.json"() {
        when: "A ccd is generated"
        def ccd = Cda.create {
            code LOINC('34133-9')
            confidentiality Conf('N')

            patient {
                name 'Vinyard', 'Jackie'
                gender 'M'
                birthTime '19551119'
                maritalStatus 'M'

                //id '99.1.2', 'ACO15N'
                id '1.2.3.4', 'ACO15'

                addr {
                    street '500 Washington Blvd'
                    city 'San Jose'
                    state 'CA'
                    postalCode '95129'
                    country 'USA'
                }
            }

            authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'

            //Test For Below 65 Before Measurement Period and Encounter, Performed: Preventive Care Services - Established Office Visit, 18 and Up
            performed CPT('99395') from '20111229' to '20111229' withStatus 'PERFORMED'

            //Test For Above 65 at Measurement Period and "Risk Category Assessment: History of Pneumococcal Vaccine"
            performed SnomedCt('473165003') from '20120605' to '20120605' withStatus 'PERFORMED'
        }
        new File('build/ACO-15-BirthDateLessThan65-IPP0-DENOM0-NUM0-BeforeMP-PCSEOV.xml').text = Cda.serialize(ccd, true)

        then: "All is well"
        1 == 1
    }

    //Test For Age <65  IPP=0, denom=0, num=0
    //one Day After Measure Period of Encounter, Performed: Preventive Care Services-Estabilished Office Visit, 18 and Up
    def "ACO-15-BirthDateLessThan65-IPP0-DENOM0-NUM0-AfterMP-PCSEOV.json"() {
        when: "A ccd is generated"
        def ccd = Cda.create {
            code LOINC('34133-9')
            confidentiality Conf('N')

            patient {
                name 'Oglby', 'Peter'
                gender 'M'
                birthTime '19551119'
                maritalStatus 'M'

                //id '99.1.2', 'ACO15N'
                id '1.2.3.4', 'ACO15'

                addr {
                    street '500 Washington Blvd'
                    city 'San Jose'
                    state 'CA'
                    postalCode '95129'
                    country 'USA'
                }
            }

            authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'

            //Test For Below 65 above Measurement Period and Encounter, Performed: Preventive Care Services - Established Office Visit, 18 and Up
            performed CPT('99395') from '20130129' to '20130129' withStatus 'PERFORMED'

            //Test For Below 65 at Measurement Period and "Risk Category Assessment: History of Pneumococcal Vaccine"
            performed SnomedCt('473165003') from '20120605' to '20120605' withStatus 'PERFORMED'
        }
        new File('build/ACO-15-BirthDateLessThan65-IPP0-DENOM0-NUM0-AfterMP-PCSEOV.xml').text = Cda.serialize(ccd, true)

        then: "All is well"
        1 == 1
    }

    //Test For Age <65  IPP=0, denom=0, num=0
    //one Day Before Measure Period of Encounter, Performed: Preventive Care Services-Initial Office Visit, 18 and Up
    def "ACO-15-BirthDateLessThan65-IPP0-DENOM0-NUM0-BeforeMP-PCSIOV.json"() {
        when: "A ccd is generated"
        def ccd = Cda.create {
            code LOINC('34133-9')
            confidentiality Conf('N')

            patient {
                name 'Martinez', 'Eduardo'
                gender 'M'
                birthTime '19551119'
                maritalStatus 'M'

                //id '99.1.2', 'ACO15N'
                id '1.2.3.4', 'ACO15'

                addr {
                    street '500 Washington Blvd'
                    city 'San Jose'
                    state 'CA'
                    postalCode '95129'
                    country 'USA'
                }
            }

            authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'

            //Test For Below 65 below Measurement Period and Preventive Care Services-Initial Office Visit, 18 and Up
            performed CPT('99395') from '20111229' to '20111229' withStatus 'PERFORMED'

            //Test For Above 65 at Measurement Period and "Risk Category Assessment: History of Pneumococcal Vaccine"
            performed SnomedCt('473165003') from '20120605' to '20120605' withStatus 'PERFORMED'
        }
        new File('build/ACO-15-BirthDateLessThan65-IPP0-DENOM0-NUM0-BeforeMP-PCSIOV.xml').text = Cda.serialize(ccd, true)

        then: "All is well"
        1 == 1
    }

    //Test For Age <65  IPP=0, denom=0, num=0
    //one Day After Measure Period of Encounter, Performed: Preventive Care Services-Initial Office Visit, 18 and Up
    def "ACO-15-BirthDateLessThan65-IPP0-DENOM0-NUM0-AfterMP-PCSIOV.json"() {
        when: "A ccd is generated"
        def ccd = Cda.create {
            code LOINC('34133-9')
            confidentiality Conf('N')

            patient {
                name 'Reyes', 'Alex'
                gender 'M'
                birthTime '19551119'
                maritalStatus 'M'

                //id '99.1.2', 'ACO15N'
                id '1.2.3.4', 'ACO15'

                addr {
                    street '500 Washington Blvd'
                    city 'San Jose'
                    state 'CA'
                    postalCode '95129'
                    country 'USA'
                }
            }

            authoredBy 'Johnson', 'Kimberly' of 'Alpine Family Physicians' identifiedAs '2.16.840.1.113883.3.771' at '20111118014000'

            //Test For Below 65 After Measurement Period and Preventive Care Services-Initial Office Visit, 18 and Up
            performed CPT('99395') from '20130101' to '20130101' withStatus 'PERFORMED'

            //Test For Above 65 at Measurement Period and "Risk Category Assessment: History of Pneumococcal Vaccine"
            performed SnomedCt('473165003') from '20120605' to '20120605' withStatus 'PERFORMED'
        }
        new File('build/ACO-15-BirthDateLessThan65-IPP0-DENOM0-NUM0-AfterMP-PCSIOV.xml').text = Cda.serialize(ccd, true)

        then: "All is well"
        1 == 1
    }
}
