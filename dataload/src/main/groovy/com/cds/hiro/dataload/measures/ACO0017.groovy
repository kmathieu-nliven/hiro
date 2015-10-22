package com.cds.hiro.dataload.measures

import com.cds.hiro.builders.CdaContext

/**
 * Created by rahul on 10/15/15.
 */
class ACO0017 extends MeasureGenerator {
  @Override
  void applyComplement(CdaContext cdaContext) {
    cdaContext.with {
      performed CPT('99411') from '20150615' to '20150615'
    }
  }

  @Override
  void applyCompliant(CdaContext cdaContext) {
    cdaContext.with {
      performed CPT('99411') from '20150615' to '20150615'
      performed LOINC('68535-4') from '20140820' to '20140820'
      performed SnomedCt('105539002') from '20141120' to '20141120'
    }
  }
}
