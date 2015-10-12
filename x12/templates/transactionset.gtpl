package ${packageName}

import com.cds.hiro.x12.structures.*
import com.cds.hiro.x12_837p.segments.*
import com.cds.hiro.x12_837p.loops.*

/**
 * From sethead.txt
 * <pre>
 * "${transactionSetId}","${transactionSetName}","${functionalGroupId}"
 * </pre>
 */
@groovy.transform.CompileStatic
@groovy.transform.ToString
@groovy.transform.builder.Builder(builderStrategy = groovy.transform.builder.SimpleStrategy, prefix = 'with')
class ${className} extends Message {

  <% children.each { detail -> %>
   <% if (detail.class.simpleName == 'Loop') { %>
  /**
   * From sethead.txt
   * <pre>
   * "${detail}"
   * </pre>
   */
   ${detail.name} ${detail.name.toLowerCase()}
   <% } else { %>
  /**
   * From sethead.txt
   * <pre>
   *
   * </pre>
   */
   ${detail.segmentId} ${detail.segmentId.toLowerCase()}
   <% } %>
  <% } %>

  void parse(List<List<List<List<String>>>> input) {
    // TODO
  }

  List<List<List<List<String>>>> toTokens(int indent = -1) {
    def retval = []
    def indentOld = indent > -1 ? indent     : -1
    def indentNew = indent > -1 ? (indent+1) : -1

    <% children.eachWithIndex { detail, idx ->
      if (idx) {

      } else {

      }
      if (detail.class.simpleName == 'Loop') {
        %>
    if (${detail.name.toLowerCase()}) retval.addAll (${detail.name.toLowerCase()}.toTokens(<%
            if (idx == 0) {%>indentOld<%} else {%>indentNew<%}%>))<%
      } else {
        %>
    if (${detail.segmentId.toLowerCase()}) retval.add (${detail.segmentId.toLowerCase()}.toTokens(<%
            if (idx == 0) {%>indentOld<%} else {%>indentNew<%}%>))<%
      }
    } %>
    retval
  }
}
