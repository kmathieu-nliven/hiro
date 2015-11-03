package com.cds.hiro.dataload.measures

import com.cds.hiro.builders.CdaContext
import com.cds.hiro.builders.X12Context
import com.cds.hiro.dataload.*

/**
 * Created by jle on 10/26/15.
 */
class ACO0008 extends MeasureGenerator {
  @Override
  boolean applyComplement(CdaContext cdaContext, X12ContextFactory x12ContextFactory) {

    x12ContextFactory.context.with {
      suffered Icd9CM('27700') between '20150620' and '20150625'
      performed CPT('99254') from '20150620' to '20150625'
    }

    x12ContextFactory.getContextWithNewFacility().with {
      performed CPT('99254') from '20150620' to '20150625'
      suffered Icd9CM('01.21') between '20150620' and '20150625'
    }

    x12ContextFactory.getContextWithNewFacility().with {
      suffered Icd9CM('27700') between '20150511' and '20150515'
      performed CPT('99254') from '20150511' to '20150515'
    }

    true
  }

  @Override
  boolean applyCompliant(CdaContext cdaContext, X12ContextFactory x12ContextFactory) {

    X12Context context1 = x12ContextFactory.context
    context1.with {
      suffered Icd9CM('277.00') between '20150720' and '20150725'
      performed CPT('99254') from '20150720' to '20150725'
    }

    x12ContextFactory.getContext(context1.facility).with {
      suffered Icd9CM('01.21') between '20150520' and '20150525'
      performed CPT('99254') from '20150520' to '20150525'
    }

    x12ContextFactory.getContext(context1.facility).with {
      suffered Icd9CM('045.00') between '20150628' and '20150701'
      performed CPT('99254') from '20150628' to '20150701'
    }


    X12Context context2 = x12ContextFactory.getContextWithNewFacility()
    context2.with {
      suffered Icd9CM('1400') between '20150820' and '20150825'
      performed CPT('99254') from '20150820' to '20150825'
    }

    x12ContextFactory.getContext(context2.facility).with {
      suffered Icd9CM('277.00') between '20150528' and '20150529'
      performed CPT('99254') from '20150528' to '20150529'
    }


    X12Context context3 = x12ContextFactory.getContextWithNewFacility()
    context3.with {
      suffered Icd9CM('01.21') between '20150511' and '20150511'
      performed CPT('99254') from '20150511' to '20150515'
    }

    x12ContextFactory.getContext(context3.facility).with {
      suffered Icd9CM('277.00') between '20150711' and '20150715'
      performed CPT('99254') from '20150711' to '20150715'
    }


    x12ContextFactory.getContextWithNewFacility().with {
      suffered Icd9CM('045.00') between '20150622' and '20150623'
      performed CPT('99254') from '20150622' to '20150623'
    }


    x12ContextFactory.getContextWithNewFacility().with {
      suffered Icd9CM('277.00') between '20150605' and '20150607'
      performed CPT('99254') from '20150605' to '20150607'
    }

    x12ContextFactory.getContextWithNewFacility().with {
      suffered Icd9CM('394.0') between '20150618' and '20150621'
      performed CPT('99254') from '20150618' to '20150621'
    }

    x12ContextFactory.getContextWithNewFacility().with {
      suffered Icd9CM('394.0') between '20150610' and '20150612'
      performed CPT('99254') from '20150610' to '20150612'
    }

    true
  }
}
