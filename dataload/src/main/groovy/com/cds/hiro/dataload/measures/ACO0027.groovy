package com.cds.hiro.dataload.measures

import com.cds.hiro.builders.CdaContext


class ACO0027 extends MeasureGenerator {
  @Override
  void applyComplement(CdaContext cdaContext) {
    cdaContext.with {
      performed SnomedCt('270427003') from '20150511' to '20150511' withStatus 'PERFORMED'
      performed SnomedCt('4783006') from '20141228' to '20150105' withStatus 'ACTIVE'

    }
  }

  @Override
  void applyCompliant(CdaContext cdaContext) {
    cdaContext.with {
      performed SnomedCt('270427003') from '20150511' to '20150511' withStatus 'PERFORMED'
      performed SnomedCt('4783006') from '20141228' to '20150105' withStatus 'ACTIVE'
      performed LOINC('17856-6') from '20150111' to '20150113' withStatus 'PERFORMED'

    }
  }
}
