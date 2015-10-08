package com.cds.hiro.x12

import com.cds.hiro.x12.structures.Message
import groovy.transform.PackageScope
import groovy.transform.builder.Builder
import groovy.transform.builder.SimpleStrategy

/**
 * Parses a X12 Document using EDI
 *
 * @author Rahul Somasunderam
 */
@Builder(builderStrategy = SimpleStrategy)
class EdiParser {
  String segmentSeperator = '\n'
  String fieldSeperator = /\*/
  String compositeSeperator = /:/
  String repetitionSeperator = /\^/


  public <T extends Message> T parse(Class<T> clazz, String input) {
    def retval = clazz.newInstance()

    println segmentSeperator

    List<List<List<String[]>>> tree = extractTree(input)

    println tree
    // retval.parse()
    retval
  }

  @PackageScope
  List<List<List<String[]>>> extractTree(String input) {
    input.split(segmentSeperator).collect {
      it.split(fieldSeperator).collect {
        it.split(repetitionSeperator).collect {
          it.split(compositeSeperator)
        }
      }
    }
  }
}
