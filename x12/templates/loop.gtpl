package ${packageName}

import com.cds.hiro.x12.structures.*
import com.cds.hiro.x12_837p.segments.*

/**
 * From nowhere
 * <pre>
 * "${name}","${loopId}""
 * </pre>
 */
@groovy.transform.CompileStatic
@groovy.transform.ToString
@groovy.transform.builder.Builder(builderStrategy = groovy.transform.builder.SimpleStrategy, prefix = 'with')
class ${className} extends Loop {

  <% children.eachWithIndex { detail, idx -> %>
  /**
   * From seghead.txt
   * <pre>
   * "${detail}"
   * </pre>
   */
  <% if (detail.class.simpleName == 'Loop') {
  %>${detail.name} ${detail.name.toLowerCase()}_${idx+1}<%
  } else {
  %>${detail.segmentId} ${detail.segmentId.toLowerCase()}_${idx+1}<%
  } %>
  <% } %>

  void parse(List<List<List<List<String>>>> input) {
    // TODO
  }


  List<List<List<List<String>>>> toTokens(int indent) {
    def retval = []

    def indentOld = indent > -1 ? indent     : -1
    def indentNew = indent > -1 ? (indent+1) : -1

<%    children.eachWithIndex { detail, idx ->
      if (detail.class.simpleName == 'Loop') {
%>
    if (${detail.name.toLowerCase()}_${idx+1}) retval.addAll (${detail.name.toLowerCase()}_${idx+1}.toTokens(<%
        if (idx == 0) {%>indentOld<%} else {%>indentNew<%}%>))<%
      } else { %>
    if (${detail.segmentId.toLowerCase()}_${idx+1}) retval.add (${detail.segmentId.toLowerCase()}_${idx+1}.toTokens(<%
              if (idx == 0) {%>indentOld<%} else {%>indentNew<%}%>))<%
      }
    }
%>
    retval
  }
}
