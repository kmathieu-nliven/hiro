package ${packageName};

import java.util.Map;
import java.util.HashMap;

/**
 * From seghead.txt
 * <pre>
 * "${dataElementNumber}","${dataElementName}"
 * </pre>
 */
public enum ${className} {

<%
  def size = enumerations.size()
  enumerations.eachWithIndex { enumeration, idx ->
%>
  /** ${enumeration.name} */
  ${enumeration.representation}_${enumeration.code}("${enumeration.code}")<%
    if (size - 1 > idx) {%>,<% } else { %>;<%}
  }
%>

  static Map<String, ${className}> reverseLookupMap = new HashMap<String, ${className}>();
  static {
    ${className}[] values = ${className}.values();
    for (${className} value: values) {
      reverseLookupMap.put(value.code, value);
    }
  }

  static ${className} byCode(String code) { return reverseLookupMap.get(code); }

  String code;
  ${className}(String code) {this.code = code;}

  @Override public String toString() { return this.code; }
}
