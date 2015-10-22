package com.cds.hiro.dataload.measures

import com.cds.hiro.builders.CdaContext

/**
 * Created by rahul on 10/15/15.
 */
class ACO0016 extends MeasureGenerator {

  @Override
  void applyComplement(CdaContext cdaContext) {
  }

  @Override
  void applyCompliant(CdaContext cdaContext) {
    cdaContext.with {
      performed CPT('99397') from '20150615' to '20150615'

      results {
        on '20150320'
        measured LOINC('39156-5') at '25 kg/m2' of 'RTO_PQ_PQ'
      }
    }
  }

}
