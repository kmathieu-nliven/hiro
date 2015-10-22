package com.cds.hiro.dataload.measures

import com.cds.hiro.builders.CdaContext


class ACO0039 extends MeasureGenerator {
  @Override
  void applyComplement(CdaContext cdaContext) {
    cdaContext.with {
      performed CPT('99341') from '20150511' to '20150511' withStatus 'PERFORMED'
    }
  }

  @Override
  void applyCompliant(CdaContext cdaContext) {
    cdaContext.with {
      performed CPT('99203') from '20150505' to '20150505'
      performed SnomedCt('428191000124101') from '20150505' to '20150505' withStatus 'PERFORMED'

    }
  }
}
