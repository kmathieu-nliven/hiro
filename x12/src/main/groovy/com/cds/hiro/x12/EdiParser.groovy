package com.cds.hiro.x12

import com.cds.hiro.x12.structures.Message
import groovy.transform.PackageScope
import groovy.transform.builder.Builder
import groovy.transform.builder.SimpleStrategy

import java.time.format.DateTimeFormatter

/**
 * Parses a X12 Document encoded as EDI
 *
 * @author Rahul Somasunderam
 */
@Builder(builderStrategy = SimpleStrategy, prefix = '')
class EdiParser {
  String segmentSeperator = '\n'
  String fieldSeperator = /\*/
  String compositeSeperator = /:/
  String repetitionSeperator = /\^/

  static final DateTimeFormatter DateFormat = DateTimeFormatter.BASIC_ISO_DATE
  static final DateTimeFormatter TimeFormat = DateTimeFormatter.ofPattern('HHmm')


  public <T extends Message> T parse(Class<T> clazz, String input) {
    def retval = clazz.newInstance()
    List<List<List<List<String>>>> tree = extractTree(input)
    println tree
    retval
  }

  @PackageScope
  List<List<List<List<String>>>> extractTree(String input) {
    input.split(segmentSeperator).collect {
      it.split(fieldSeperator).collect {
        it.split(repetitionSeperator).collect {
          it.split(compositeSeperator).toList()*.trim()
        }
      }
    }
  }
}
