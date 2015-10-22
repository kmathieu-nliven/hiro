package com.cds.hiro.dataload.measures

import com.cds.hiro.builders.CdaContext

/**
 * Created by rahul on 10/15/15.
 */
class ACO0015 extends MeasureGenerator {
  @Override
  void applyComplement(CdaContext cdaContext) {
    cdaContext.with {
      performed CPT('99203') from '20150505' to '20150505'
    }
  }

  @Override
  void applyCompliant(CdaContext cdaContext) {
    cdaContext.with {
      performed SnomedCt('394678003') from '19980505' to '19980505' withStatus 'PERFORMED'

      performed CPT('99203') from '20150505' to '20150505'
    }
  }
}
