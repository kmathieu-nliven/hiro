package com.cds.hiro.dataload.measures

import com.cds.hiro.builders.CdaContext
import com.cds.hiro.builders.X12Context
import com.cds.hiro.dataload.X12ContextFactory

/**
 * Created by rahul on 10/15/15.
 */
class ACO0017 extends MeasureGenerator {
  @Override
  boolean applyComplement(CdaContext cdaContext, X12ContextFactory x12ContextFactory) {
    if (randBoolean()) {
      cdaContext.with {
        performed CPT('99411') from '20150615' to '20150615'
      }
    } else {
      x12ContextFactory.context.with {
        performed CPT('99411') from '20150615' to '20150615'
      }
    }
    true
  }

  @Override
  boolean applyCompliant(CdaContext cdaContext, X12ContextFactory x12ContextFactory) {
    if (randBoolean()) {
      cdaContext.with {
        performed CPT('99411') from '20150615' to '20150615'
        performed LOINC('68535-4') from '20140820' to '20140820'
        performed SnomedCt('105539002') from '20141120' to '20141120'
      }
    } else {
      x12ContextFactory.context.with {
        performed CPT('99411') from '20150615' to '20150615'
        performed LOINC('68535-4') from '20140820' to '20140820'
        performed SnomedCt('105539002') from '20141120' to '20141120'
      }
    }

    true
  }
}
