package com.cds.hiro.dataload.measures

import com.cds.hiro.builders.CdaContext

/**
 * Created by rahul on 10/15/15.
 */
class ACO0013 extends MeasureGenerator {
  @Override
  void applyComplement(CdaContext cdaContext) {
    cdaContext.with {
      performed CPT('99341') from '20151003' to '20151003'
      performed CPT('99341') from '20150615' to '20150615'
    }
  }

  @Override
  void applyCompliant(CdaContext cdaContext) {
    cdaContext.with {
      performed SnomedCt('12843005') from '20141120' to '20141120'
      performed CPT('99341') from '20151003' to '20151003'
      performed CPT('99341') from '20150615' to '20150615'
      performed SnomedCt('442333005') from '20141120' to '20141120' withStatus 'PERFORMED'
    }
  }
}
