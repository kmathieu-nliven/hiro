package com.cds.hiro.dataload.measures

import com.cds.hiro.builders.CdaContext
import com.cds.hiro.builders.X12Context
import com.cds.hiro.dataload.X12ContextFactory


class ACO0039 extends MeasureGenerator {
  @Override
  boolean applyComplement(CdaContext cdaContext, X12ContextFactory x12ContextFactory) {
    if (randBoolean()) {
      cdaContext.with {
        performed CPT('99341') from '20150511' to '20150511' withStatus 'PERFORMED'
      }
    } else {
      x12ContextFactory.context.with {
        performed CPT('99341') from '20150511' to '20150511' withStatus 'PERFORMED'
      }
    }
    true
  }

  @Override
  boolean applyCompliant(CdaContext cdaContext, X12ContextFactory x12ContextFactory) {
    if (randBoolean()) {
      cdaContext.with {
        performed CPT('99203') from '20150505' to '20150505'
        performed SnomedCt('428191000124101') from '20150505' to '20150505' withStatus 'PERFORMED'
      }
    } else {
      x12ContextFactory.context.with {
        performed CPT('99203') from '20150505' to '20150505'
        performed SnomedCt('428191000124101') from '20150505' to '20150505' withStatus 'PERFORMED'
      }
    }
    true
  }
}
