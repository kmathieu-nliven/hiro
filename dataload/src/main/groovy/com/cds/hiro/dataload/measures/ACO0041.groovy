package com.cds.hiro.dataload.measures

import com.cds.hiro.builders.CdaContext
import com.cds.hiro.builders.X12Context
import com.cds.hiro.dataload.X12ContextFactory

/**
 * Created by rahul on 10/15/15.
 */
class ACO0041 extends MeasureGenerator {
  @Override
  boolean applyComplement(CdaContext cdaContext, X12ContextFactory x12ContextFactory) {
    if (randBoolean()) {
      cdaContext.with {
        performed CPT('99202') from '20150304' to '20150304'
        performed SnomedCt('252779009') from '20151010' to '20151011'
        suffered Icd9CM('250.03') between '20150304' and '20150304'
      }
    } else {
      x12ContextFactory.context.with {
        performed CPT('99202') from '20150304' to '20150304'
        performed SnomedCt('252779009') from '20151010' to '20151011'
        suffered Icd9CM('250.03') between '20150304' and '20150304'
      }
    }
    true
  }

  @Override
  boolean applyCompliant(CdaContext cdaContext, X12ContextFactory x12ContextFactory) {
    if (randBoolean()) {
      cdaContext.with {
        performed CPT('99202') from '20150304' to '20150304'
        suffered Icd9CM('250.03') between '20150304' and '20150304'
      }
    } else {
      x12ContextFactory.context.with {
        performed CPT('99202') from '20150304' to '20150304'
        suffered Icd9CM('250.03') between '20150304' and '20150304'
      }
    }
    true
  }
}
