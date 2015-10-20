package ${packageName}

import com.cds.hiro.x12.structures.*
import com.cds.hiro.x12_837p.enums.*

/**
 * From seghead.txt
 * <pre>
 * "${dataElementNumber}","${dataElementName}"
 * </pre>
 */
@groovy.transform.CompileStatic
@groovy.transform.ToString
@groovy.transform.builder.Builder(builderStrategy = groovy.transform.builder.SimpleStrategy, prefix = 'with')
class ${className} extends Composite {

  <% details.each { detail -> %>
  /**
   * From comdetl.txt:
   * <pre>
   * "${dataElementNumber}","${detail.sequence}","${detail.dataElementNumber}","${detail.requirement}"
   * </pre>
   */
  ${detail.element.toJavaType()} ${detail.element.toFieldName()}_${detail.sequence}
  <% } %>

  void parse(List<String> input) {
    <% details.eachWithIndex { detail, idx ->
    %>this.${detail.element.toFieldName()}_${detail.sequence} = input.size() > ${idx} ? valueOf(input[${idx}],${detail.element.toJavaType()}) : null;
    <% } %>
  }

  @Override
  List<String> toTokens() {
    def retval = []
    <% details.each { detail -> %>
    retval.add(${detail.element.toFieldName()}_${detail.sequence}?.toString() ?: '')
    <% } %>
    retval
  }

}
