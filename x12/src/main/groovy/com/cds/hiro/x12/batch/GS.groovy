package com.cds.hiro.x12.batch

import com.cds.hiro.x12.structures.Segment

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Created by rahul on 10/23/15.
 */
@groovy.transform.CompileStatic
@groovy.transform.ToString
@groovy.transform.builder.Builder(builderStrategy = groovy.transform.builder.SimpleStrategy, prefix = 'with')
class GS extends Segment {
  String functionalIdentifierCode_01
  String applicationSenderCode_02
  String applicationReceiverCode_03
  LocalDate date_04 = LocalDate.now()
  LocalTime time_05 = LocalTime.now()
  int groupControlNumber_06
  String reponsibleAgencyCode_07 = 'X'
  String version_08 = '005010X222A1'

  @Override
  void parse(List<List<List<String>>> input) {

  }

  @Override
  List<List<List<String>>> toTokens(int indent) {
    def indentString = indent > -1 ? ('  ' * indent) : ''

    def retval = new ArrayList().with {
      add new ArrayList().with {
        add new ArrayList().with {
          add "${indentString}GS".toString()
          it
        }
        it
      }
      it
    }

    retval.add([functionalIdentifierCode_01].collect { rep -> rep ? [rep.toString()] : [] })
    retval.add([applicationSenderCode_02].collect { rep -> rep ? [rep.toString()] : [] })
    retval.add([applicationReceiverCode_03].collect { rep -> rep ? [rep.toString()] : [] })
    retval.add([date_04].collect { rep -> rep ? [DateTimeFormatter.ofPattern('yyMMdd').format(rep)] : [] })
    retval.add([time_05].collect { rep -> rep ? [DateTimeFormatter.ofPattern('HHmm').format(rep)] : [] })
    retval.add([groupControlNumber_06].collect { rep -> rep ? [rep.toString()] : [] })
    retval.add([reponsibleAgencyCode_07].collect { rep -> rep ? [rep.toString()] : [] })
    retval.add([version_08].collect { rep -> rep ? [rep.toString()] : [] })

    retval
  }
}
