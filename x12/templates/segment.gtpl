package ${packageName}

import com.cds.hiro.x12.structures.*
import com.cds.hiro.x12_837p.composites.*
import com.cds.hiro.x12_837p.enums.*

/**
 * From seghead.txt
 * <pre>
 * "${segmentId}","${segmentName}"
 * </pre>
 */
@groovy.transform.CompileStatic
@groovy.transform.ToString
@groovy.transform.builder.Builder(builderStrategy = groovy.transform.builder.SimpleStrategy, prefix = 'with')
class ${className} extends Segment {

  <% details.each { detail -> %>
  /**
   * From segdetl.txt:
   * <pre>
   * "${detail.segmentID}","${detail.sequence}","${detail.dataElementNumber}","${detail.requirement}","${detail.repeat}"
   * </pre>
   * From elehead.txt + eledetl.txt or comhead.txt:
   * <pre>
   * ${detail.element.toRow()}
   * </pre>
   */
  <% if (detail.requirement == 'C') { %>@Deprecated <% }
  %>${detail.toJavaType()} ${detail.element.toFieldName()}_${detail.sequence}
  <% } %>

  void parse(List<List<List<String>>> input) {
    <% details.eachWithIndex { detail, idx ->
    %>this.${detail.element.toFieldName()}_${detail.sequence} = input.size() > ${idx + 1
          } ? <%if (detail.repeat == '1') {
          %> valueOf(input[${idx + 1}], ${detail.element.toJavaType()})<%
          } else {
          %> listOf(input[${idx + 1}], ${detail.element.toJavaType()})<%
          }%> : null
    <% } %>
  }

  List<List<List<String>>> toTokens(int indent) {
    def indentString = indent > -1 ? ('  ' * indent) : ''

    def retval = new ArrayList().with {
      add new ArrayList().with {
        add new ArrayList().with {
          add "\${indentString}${className}".toString()
          it
        }
        it
      }
      it
    }

    <% details.each { detail ->
    %>retval.add(<%
        if (detail.toJavaType().startsWith('List')) {
        %>${detail.element.toFieldName()}_${detail.sequence}<%
        } else {
        %>[${detail.element.toFieldName()}_${detail.sequence}]<%
        }
        %>.collect { rep -> <%
              if (detail.element.toJavaType() in ['String', 'Integer', 'Double']) {
              %>rep ? [rep.toString()] : []<%
              } else if (detail.element.toJavaType()  ==  'java.time.LocalDate') {
              %>rep ? [rep.format(com.cds.hiro.x12.EdiParser.DateFormat) ] : []<%
              } else if (detail.element.toJavaType()  ==  'java.time.LocalTime') {
              %>rep ? [rep.format(com.cds.hiro.x12.EdiParser.TimeFormat)] : []<%
              } else if (detail.element.isEnum()) {
              %>rep ? [rep.toString() ] : []<%
              } else if (detail.element.isComposite()) {
              %>rep?.toTokens() ?: []<%
              } else {
              %>throw new Exception("tokenize not implemented for \\${rep.class}")<%
              } %> })
    <% } %>
    retval
  }
}
