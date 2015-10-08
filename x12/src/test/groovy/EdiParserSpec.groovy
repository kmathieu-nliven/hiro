import com.cds.hiro.x12.EdiParser
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
            [['23']]
        ],
        [
            [['ST']],
            [['Foo Bar']],
            [['Fubar']]
        ]
    ]
  }
  def "Tokenizing can be customized"() {
    given: "A builder"
    def parser = new EdiParser().setSegmentSeperator('~').setFieldSeperator(/\|/)

    when: "I give it inputs without composites or reps"
    def tree = parser.extractTree("""ISA|12|23~ST|Foo Bar|Fubar""")

    then: "It works"
    tree == [
        [
            [['ISA']],
            [['12']],
            [['23']]
        ],
        [
            [['ST']],
            [['Foo Bar']],
            [['Fubar']]
        ]
    ]
  }
}
