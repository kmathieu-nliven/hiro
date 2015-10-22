package com.cds.hiro.x12

import com.cds.hiro.x12.structures.Message
import groovy.transform.PackageScope
import groovy.transform.builder.Builder
import groovy.transform.builder.SimpleStrategy

import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

/**
 * Parses a X12 Document encoded as EDI
 *
 * @author Rahul Somasunderam
 */
@Builder(builderStrategy = SimpleStrategy, prefix = '')
class EdiParser {
  String segmentSeparator = '\n'
  String fieldSeparator = '*'
  String compositeSeparator = ':'
  String repetitionSeparator = '^'

  static final DateTimeFormatter DateTimeFormat = DateTimeFormatter.ofPattern('yyyyMMddHHmm')
  static final DateTimeFormatter DateFormat = DateTimeFormatter.BASIC_ISO_DATE
  static final DateTimeFormatter TimeFormat = DateTimeFormatter.ofPattern('HHmm')


  public <T extends Message> T parse(Class<T> clazz, String input) {
    def retval = clazz.newInstance()
    List<List<List<List<String>>>> tree = extractTree(input)
    println tree
    retval
  }

  private String p(String separator) {
    Pattern.quote(separator)
  }

  @PackageScope
  List<List<List<List<String>>>> extractTree(String input) {
    input.split(p(segmentSeparator)).collect {
      it.split(p (fieldSeparator)).collect {
        it.split(p (repetitionSeparator)).collect {
          it.split(p (compositeSeparator)).toList()*.trim()
        }
      }
    }
  }

  String toEdi(List<List<List<List<String>>>> tokens) {
    tokens.collect { seg ->
      seg.collect { rep ->
        rep.collect { composite ->
          composite.join(compositeSeparator)
        }.join(repetitionSeparator)
      }.join(fieldSeparator)
    }.join(segmentSeparator)
  }
}
