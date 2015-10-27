package com.cds.hiro.x12.batch

import com.cds.hiro.x12.structures.Segment

import java.time.format.DateTimeFormatter

/**
 * Created by rahul on 10/23/15.
 */
@groovy.transform.CompileStatic
@groovy.transform.ToString
@groovy.transform.builder.Builder(builderStrategy = groovy.transform.builder.SimpleStrategy, prefix = 'with')
class GE extends Segment{
  int numberOfTSIncluded_01 = 0
  int groupControlNumber_02

  @Override
  void parse(List<List<List<String>>> input) {

  }

  @Override
  List<List<List<String>>> toTokens(int indent) {
    def indentString = indent > -1 ? ('  ' * indent) : ''

    def retval = new ArrayList().with {
      add new ArrayList().with {
        add new ArrayList().with {
          add "${indentString}GE".toString()
          it
        }
        it
      }
      it
    }

    retval.add([numberOfTSIncluded_01].collect { rep -> rep ? [rep.toString()] : [] })
    retval.add([groupControlNumber_02].collect { rep -> rep ? [rep.toString()] : [] })

    retval
  }
}
