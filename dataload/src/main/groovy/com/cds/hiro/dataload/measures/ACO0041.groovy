package com.cds.hiro.dataload.measures

import com.cds.hiro.builders.CdaContext
import com.cds.hiro.builders.X12Context

/**
 * Created by rahul on 10/15/15.
 */
class ACO0041 extends MeasureGenerator {
  @Override
  boolean applyComplement(CdaContext cdaContext, X12Context x12Context) {
    if (randBoolean()) {
      cdaContext.with {
        performed CPT('99202') from '20150304' to '20150304'
        performed SnomedCt('252779009') from '20151010' to '20151011'
        suffered Icd9CM('250.03') between '20150304' and '20150304'
      }
    } else {
      x12Context.with {
        performed CPT('99202') from '20150304' to '20150304'
        performed SnomedCt('252779009') from '20151010' to '20151011'
        suffered Icd9CM('250.03') between '20150304' and '20150304'
      }
    }
    true
  }

  @Override
  boolean applyCompliant(CdaContext cdaContext, X12Context x12Context) {
    if (randBoolean()) {
      cdaContext.with {
        performed CPT('99202') from '20150304' to '20150304'
        suffered Icd9CM('250.03') between '20150304' and '20150304'
      }
    } else {
      x12Context.with {
        performed CPT('99202') from '20150304' to '20150304'
        suffered Icd9CM('250.03') between '20150304' and '20150304'
      }
    }
    true
  }
}
