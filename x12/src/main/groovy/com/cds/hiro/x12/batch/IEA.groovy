package com.cds.hiro.x12.batch

import com.cds.hiro.x12.structures.Segment

/**
 * Created by rahul on 10/23/15.
 */
@groovy.transform.CompileStatic
@groovy.transform.ToString
@groovy.transform.builder.Builder(builderStrategy = groovy.transform.builder.SimpleStrategy, prefix = 'with')
class IEA extends Segment {
  int numberOfFunctionalGroups = 0
  int interchangeControlNumber = 0

  @Override
  void parse(List<List<List<String>>> input) {

  }

  @Override
  List<List<List<String>>> toTokens(int indent) {
    def indentString = indent > -1 ? ('  ' * indent) : ''

    def retval = new ArrayList().with {
      add new ArrayList().with {
        add new ArrayList().with {
          add "${indentString}IEA".toString()
          it
        }
        it
      }
      it
    }

    retval.add([numberOfFunctionalGroups].collect { rep -> rep ? [rep.toString()] : [] })
    retval.add([interchangeControlNumber].collect { rep -> rep ? [rep.toString()] : [] })

    retval
  }
}
