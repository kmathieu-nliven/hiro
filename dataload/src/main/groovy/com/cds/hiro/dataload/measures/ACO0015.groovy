package com.cds.hiro.dataload.measures

import com.cds.hiro.builders.CdaContext
import com.cds.hiro.builders.X12Context

/**
 * Created by rahul on 10/15/15.
 */
class ACO0015 extends MeasureGenerator {
  @Override
  boolean applyComplement(CdaContext cdaContext, X12Context x12Context) {
    if (randBoolean()) {
      cdaContext.with {
        performed CPT('99203') from '20150505' to '20150505'
      }
    } else {
      x12Context.with {
        performed CPT('99203') from '20150505' to '20150505'
      }
    }
    true
  }

  @Override
  boolean applyCompliant(CdaContext cdaContext, X12Context x12Context) {
    if (randBoolean()) {
      cdaContext.with {
        performed SnomedCt('394678003') from '19980505' to '19980505' withStatus 'PERFORMED'

        performed CPT('99203') from '20150505' to '20150505'
      }
    } else {
      x12Context.with {
        performed SnomedCt('394678003') from '19980505' to '19980505' withStatus 'PERFORMED'

        performed CPT('99203') from '20150505' to '20150505'
      }
    }
    true
  }
}
