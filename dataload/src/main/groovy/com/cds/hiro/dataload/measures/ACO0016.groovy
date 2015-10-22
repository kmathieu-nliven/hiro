package com.cds.hiro.dataload.measures

import com.cds.hiro.builders.CdaContext
import com.cds.hiro.builders.X12Context

/**
 * Created by rahul on 10/15/15.
 */
class ACO0016 extends MeasureGenerator {

  @Override
  boolean applyComplement(CdaContext cdaContext, X12Context x12Context) {
    false
  }

  @Override
  boolean applyCompliant(CdaContext cdaContext, X12Context x12Context) {
    if (randBoolean()) {
      cdaContext.with {
        performed CPT('99397') from '20150615' to '20150615'
      }
    } else {
      x12Context.with {
        performed CPT('99397') from '20150615' to '20150615'
      }
    }
    cdaContext.with {
      results {
        on '20150320'
        measured LOINC('39156-5') at '25 kg/m2' of 'RTO_PQ_PQ'
      }
    }
    true
  }

}
