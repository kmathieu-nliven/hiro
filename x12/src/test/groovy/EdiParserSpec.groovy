import com.cds.hiro.x12.EdiParser
import com.cds.hiro.x12_837p.enums.CodeListQualifierCode
import com.cds.hiro.x12_837p.enums.DateTimePeriodFormatQualifier
import com.cds.hiro.x12_837p.enums.GenderCode
import com.cds.hiro.x12_837p.enums.HierarchicalStructureCode
import com.cds.hiro.x12_837p.enums.MaritalStatusCode
import com.cds.hiro.x12_837p.enums.RaceorEthnicityCode
import com.cds.hiro.x12_837p.enums.TransactionSetIdentifierCode
import com.cds.hiro.x12_837p.enums.TransactionSetPurposeCode
import com.cds.hiro.x12_837p.enums.TransactionTypeCode
import com.cds.hiro.x12_837p.loops.L1000A
import com.cds.hiro.x12_837p.loops.L1000B
import com.cds.hiro.x12_837p.segments.BHT
import com.cds.hiro.x12_837p.segments.CLM
import com.cds.hiro.x12_837p.segments.DMG
import com.cds.hiro.x12_837p.segments.REF
import com.cds.hiro.x12_837p.segments.SE
import com.cds.hiro.x12_837p.segments.ST
import com.cds.hiro.x12_837p.transactionsets.M837Q1
import spock.lang.Specification

import java.time.LocalDate
import java.time.LocalTime

/**
 * Tests parsing and creation of X12 documents based on the EDI format
 *
 * @author Rahul Somasunderam
 */
class EdiParserSpec extends Specification {
  def "Tokenizing works with defaults for a simple message"() {
    given: "A builder"
    def parser = new EdiParser()

    when: "I give it inputs without composites or reps"
    def tree = parser.extractTree("""\
ISA*12*23
ST*Foo Bar*Fubar
""")

    then: "It works"
    tree == [
        [
            [['ISA']],
            [['12']],
            [['23']],
        ],
        [
            [['ST']],
            [['Foo Bar']],
            [['Fubar']],
        ]
    ]
  }

  def "Tokenizing can be customized"() {
    given: "A builder"
    def parser = new EdiParser().segmentSeparator('~').fieldSeparator('|')

    when: "I give it inputs without composites or reps"
    def tree = parser.extractTree("""ISA|12|23~ST|Foo Bar|Fubar""")

    then: "It works"
    tree == [
        [
            [['ISA']],
            [['12']],
            [['23']],
        ],
        [
            [['ST']],
            [['Foo Bar']],
            [['Fubar']],
        ]
    ]
  }

  def "A segment can be parsed"() {
    given: "A segment tree"
    def segmentTree =
        [
            [['ST']],
            [['837']],
            [['Fubar']],
            [['Snafu']],
        ]

    when: "I parse it into an ST"
    ST segment = new ST()
    segment.parse(segmentTree)

    then: "Segment is valid"
    segment.transactionSetIdentifierCode_01 == TransactionSetIdentifierCode.HealthCareClaim_837
    segment.transactionSetControlNumber_02 == 'Fubar'
    segment.implementationConventionReference_03 == 'Snafu'
  }

  def "A segment can be parsed even if it is incomplete"() {
    given: "A segment tree"
    def segmentTree =
        [
            [['ST']],
            [['837']],
            [['Fubar']],
        ]

    when: "I parse it into an ST"
    ST segment = new ST()
    segment.parse(segmentTree)

    then: "Segment is valid"
    segment.transactionSetIdentifierCode_01 == TransactionSetIdentifierCode.HealthCareClaim_837
    segment.transactionSetControlNumber_02 == 'Fubar'
    segment.implementationConventionReference_03 == null
  }

  def "A segment can be parsed when there are repetitions of a field"() {
    given: "A segment tree"
    def segmentTree =
        [
            [['DMG']],
            [['D8']],
            [['19330706']],
            [['M']],
            [['I']],
            [['H','0','4'], ['G','1']],
        ]

    when: "I parse it into an ST"
    DMG segment = new DMG()
    segment.parse(segmentTree)

    then: "Segment is valid"
    segment.dateTimePeriodFormatQualifier_01 == DateTimePeriodFormatQualifier.DateExpressedinFormatCCYYMMDD_D8
    segment.dateTimePeriod_02 == '19330706'
    segment.genderCode_03 == GenderCode.Male_M
    segment.maritalStatusCode_04 == MaritalStatusCode.Single_I
    segment.compositeRaceorEthnicityInformation_05.size() == 2
    with (segment.compositeRaceorEthnicityInformation_05[0]) {
      raceorEthnicityCode_01 == RaceorEthnicityCode.Hispanic_H
      codeListQualifierCode_02 == CodeListQualifierCode.DocumentIdentificationCode_0
      industryCode_03 == '4'
    }
    with (segment.compositeRaceorEthnicityInformation_05[1]) {
      raceorEthnicityCode_01 == RaceorEthnicityCode.NativeAmerican_G
      codeListQualifierCode_02 == CodeListQualifierCode.FreeOnBoardSiteCode_1
      industryCode_03 == null
    }
  }

  def "A segment can be parsed when there is an Integer"() {
    given: "A segment tree"
    def segmentTree =
        [
            [['SE']],
            [['31']],
            [['3701']],
        ]

    when: "I parse it into an ST"
    SE segment = new SE()
    segment.parse(segmentTree)

    then: "Segment is valid"
    segment.numberofIncludedSegments_01 == 31
    segment.transactionSetControlNumber_02 == '3701'
  }

  def "A segment can be parsed when there is a Double"() {
    given: "A segment tree"
    def segmentTree =
        [
            [['CLM']],
            [['ABC7001']],
            [['65']],
            [[]],
            [[]],
        ]

    when: "I parse it into an ST"
    CLM segment = new CLM()
    segment.parse(segmentTree)

    then: "Segment is valid"
    segment.claimSubmittersIdentifier_01 == 'ABC7001'
    segment.monetaryAmount_02 == 65.0
  }

  def "A message can be turned into a tree"() {
    given: "An x12 message"
    def x12 = new M837Q1().
        withSt(new ST().
            withTransactionSetIdentifierCode_01(TransactionSetIdentifierCode.HealthCareClaim_837).
            withTransactionSetControlNumber_02('123').
            withImplementationConventionReference_03('ABC')
        ).
        withBht(new BHT().
            withHierarchicalStructureCode_01(HierarchicalStructureCode.ReportingAgencyClaimAdministratorInsurerInsuredEmployerClaimantPayment_0211).
            withTransactionSetPurposeCode_02(TransactionSetPurposeCode.Initial_57).
            withReferenceIdentification_03('12345').
            withDate_04(LocalDate.parse('20140903', EdiParser.DateFormat)).
            withTime_05(LocalTime.parse('2145', EdiParser.TimeFormat)).
            withTransactionTypeCode_06(TransactionTypeCode.ClaimSubmission_CK)
        ).
        withRef(new REF()).
        withL1000a(new L1000A()).
        withL1000b(new L1000B())

    when: "I convert it to tokens"
    def tokens = x12.toTokens()

    then: "Tokens match expectations"
    tokens == [
        [
            [['ST']],
            [['837']],
            [['123']],
            [['ABC']],
        ],
        [
            [['BHT']],
            [['0211']],
            [['57']],
            [['12345']],
            [['20140903']],
            [['2145']],
            [['CK']],
        ],
        [
            [['REF']],
            [[]],
            [[]],
            [[]],
            [[]],
        ]
    ]

    when: "I convert it to edi"
    def edi = new EdiParser().toEdi(tokens)

    def expected ='''\
        |ST*837*123*ABC
        |BHT*0211*57*12345*20140903*2145*CK
        |REF****'''.stripMargin()

    then: "Edi matches expectation"
    edi == expected
  }
}
