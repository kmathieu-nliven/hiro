import com.cds.hiro.x12.EdiParser
import com.cds.hiro.x12_837p.enums.TransactionSetIdentifierCode
import com.cds.hiro.x12_837p.segments.ST
import spock.lang.Specification

/**
 * Created by rahul on 10/8/15.
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
    def parser = new EdiParser().segmentSeperator('~').fieldSeperator(/\|/)

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
}
