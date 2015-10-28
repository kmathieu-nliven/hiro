package com.cds.hiro.dataload.measures

import com.cds.hiro.builders.CdaContext
import com.cds.hiro.builders.X12Context
import com.cds.hiro.dataload.X12ContextFactory


class ACO0027 extends MeasureGenerator {
  @Override
  boolean applyComplement(CdaContext cdaContext, X12ContextFactory x12ContextFactory) {
    if (randBoolean()) {
      cdaContext.with {
        performed SnomedCt('270427003') from '20150511' to '20150511' withStatus 'PERFORMED'
        performed SnomedCt('4783006') from '20141228' to '20150105' withStatus 'ACTIVE'
      }
    } else {
      x12ContextFactory.context.with {
        performed SnomedCt('270427003') from '20150511' to '20150511' withStatus 'PERFORMED'
        performed SnomedCt('4783006') from '20141228' to '20150105' withStatus 'ACTIVE'
      }
    }
    true
  }

  @Override
  boolean applyCompliant(CdaContext cdaContext, X12ContextFactory x12ContextFactory) {
    if (randBoolean()) {
      cdaContext.with {
        performed SnomedCt('270427003') from '20150511' to '20150511' withStatus 'PERFORMED'
        performed SnomedCt('4783006') from '20141228' to '20150105' withStatus 'ACTIVE'
        performed LOINC('17856-6') from '20150111' to '20150113' withStatus 'PERFORMED'
      }
    } else {
      x12ContextFactory.context.with {
        performed SnomedCt('270427003') from '20150511' to '20150511' withStatus 'PERFORMED'
        performed SnomedCt('4783006') from '20141228' to '20150105' withStatus 'ACTIVE'
        performed LOINC('17856-6') from '20150111' to '20150113' withStatus 'PERFORMED'
      }
    }
    true
  }
}
