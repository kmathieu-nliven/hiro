package com.cds.hiro.dataload.measures

import com.cds.hiro.builders.CdaContext
import com.cds.hiro.builders.X12Context
import com.cds.hiro.dataload.X12ContextFactory


class ACO0031 extends MeasureGenerator {
  @Override
  boolean applyComplement(CdaContext cdaContext, X12ContextFactory x12ContextFactory) {
    if (randBoolean()) {
      cdaContext.with {
        performed CPT('99244') from '20150515' to '20150518' withStatus 'PERFORMED'
        performed SnomedCt('981000124106') from '20150516' to '20150517' withStatus 'ACTIVE'
        performed SnomedCt('10091002') from '20150509' to '20150515' withStatus 'PERFORMED'
        performed CPT('99202') from '20150511' to '20150511' withStatus 'PERFORMED'
      }
    } else {
      x12ContextFactory.context.with {
        performed CPT('99244') from '20150515' to '20150518' withStatus 'PERFORMED'
        performed SnomedCt('981000124106') from '20150516' to '20150517' withStatus 'ACTIVE'
        performed SnomedCt('10091002') from '20150509' to '20150515' withStatus 'PERFORMED'
        performed CPT('99202') from '20150511' to '20150511' withStatus 'PERFORMED'
      }
    }
    true
  }

  @Override
  boolean applyCompliant(CdaContext cdaContext, X12ContextFactory x12ContextFactory) {
    if (randBoolean()) {
      cdaContext.with {
        performed CPT('99244') from '20150515' to '20150518' withStatus 'PERFORMED'
        performed RxNorm('200031') from '20150516' to '20150517' withStatus 'ACTIVE'
        performed SnomedCt('981000124106') from '20150516' to '20150517' withStatus 'ACTIVE'
        performed SnomedCt('10091002') from '20150509' to '20150515' withStatus 'PERFORMED'
        performed CPT('99202') from '20150510' to '20150515' withStatus 'PERFORMED'
      }
    } else {
      x12ContextFactory.context.with {
        performed CPT('99244') from '20150515' to '20150518' withStatus 'PERFORMED'
        performed RxNorm('200031') from '20150516' to '20150517' withStatus 'ACTIVE'
        performed SnomedCt('981000124106') from '20150516' to '20150517' withStatus 'ACTIVE'
        performed SnomedCt('10091002') from '20150509' to '20150515' withStatus 'PERFORMED'
        performed CPT('99202') from '20150510' to '20150515' withStatus 'PERFORMED'
      }
    }
    true
  }
}
