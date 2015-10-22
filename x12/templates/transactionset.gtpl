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

  <% children.each { detail ->
  def tsd = null
  %>
  <%   if (detail.class.simpleName == 'Loop') {
    tsd = details.find{it.loopId == detail.loopId}
  %>
  /**
   * From sethead.txt
   * <pre>
   * "${tsd}"
   * </pre>
   */
  <% if (tsd.loopRepeat == '1') {%>
  ${detail.name} ${detail.name.toLowerCase()}
  <% } else {%>
  List<${detail.name}> ${detail.name.toLowerCase()} = new ArrayList<${detail.name}>()
  ${className} with${detail.methodName}(${detail.name} item) {
    this.${detail.name.toLowerCase()}.add(item)
    return this
  }
<% } %>
  <%   } else {
    tsd = details.find{it.segmentId == detail.segmentId}
  %>
  /**
   * From sethead.txt
   * <pre>
   * "${tsd}"
   * </pre>
   */
  ${detail.segmentId} ${detail.segmentId.toLowerCase()}
  <%   } %>
  <% } %>

  void parse(List<List<List<List<String>>>> input) {
    // TODO
  }

  List<List<List<List<String>>>> toTokens(int indent = -1) {
    def retval = []
    def indentOld = indent > -1 ? indent     : -1
    def indentNew = indent > -1 ? (indent+1) : -1

    <% children.eachWithIndex { detail, idx ->
      def tsd = null
         if (detail.class.simpleName == 'Loop') {
           tsd = details.find{it.loopId == detail.loopId}
            if (tsd.loopRepeat == '1') {
%>
    if (${detail.name.toLowerCase()}) retval.addAll (${detail.name.toLowerCase()}.toTokens(<%
    if (idx == 0) {%>indentOld<%} else {%>indentNew<%}%>))<%
            } else {
%>
    ${detail.name.toLowerCase()}*.toTokens(<%if (idx == 0) {%>indentOld<%} else {%>indentNew<%}%>).each {retval.addAll(it)}
    <%
            }

          } else {
            tsd = details.find{it.segmentId == detail.segmentId}
        %>
    if (${detail.segmentId.toLowerCase()}) retval.add (${detail.segmentId.toLowerCase()}.toTokens(<%
            if (idx == 0) {%>indentOld<%} else {%>indentNew<%}%>))<%
          }
       }
    %>
    retval
  }
}
