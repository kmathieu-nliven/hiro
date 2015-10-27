package com.cds.hiro.dataload.measures

import com.cds.hiro.builders.CdaContext
import com.cds.hiro.builders.X12Context
import com.cds.hiro.dataload.X12ContextFactory

/**
 * Created by rahul on 10/15/15.
 */
class ACO0014 extends MeasureGenerator {

  @Override
  boolean applyComplement(CdaContext cdaContext, X12ContextFactory x12ContextFactory) {
    if (randBoolean()) {
      cdaContext.with {
        performed CPT('99341') from '20151003' to '20151003'
        performed CPT('99341') from '20150615' to '20150615'
      }
    } else {
      x12ContextFactory.context.with {
        performed CPT('99341') from '20151003' to '20151003'
        performed CPT('99341') from '20150615' to '20150615'
      }
    }
    true
  }

  @Override
  boolean applyCompliant(CdaContext cdaContext, X12ContextFactory x12ContextFactory) {
    if (randBoolean()) {
      cdaContext.with {
        performed SnomedCt('12843005') from '20141120' to '20141120'

        performed CPT('99341') from '20151003' to '20151003'
        performed CPT('99341') from '20150615' to '20150615'

        performed SnomedCt('442333005') from '20141120' to '20141120'
      }
    } else {
      x12ContextFactory.context.with {
        performed SnomedCt('12843005') from '20141120' to '20141120'

        performed CPT('99341') from '20151003' to '20151003'
        performed CPT('99341') from '20150615' to '20150615'

        performed SnomedCt('442333005') from '20141120' to '20141120'
      }

    }
    true
  }

}
