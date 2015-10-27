package com.cds.hiro.dataload.measures

import com.cds.hiro.builders.CdaContext
import com.cds.hiro.builders.X12Context
import com.cds.hiro.dataload.X12ContextFactory

import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter

/**
 * Created by rahul on 10/15/15.
 */
class ACO0013 extends MeasureGenerator {
  private int calcAge(String birthTime) {
    LocalDate today = LocalDate.now()
    LocalDate birthday = LocalDate.parse(birthTime, DateTimeFormatter.ofPattern('yyyyMMdd'))
    Period period = Period.between(birthday, today)
    period.years
  }

  @Override
  boolean applyComplement(CdaContext cdaContext, X12ContextFactory x12ContextFactory) {
    if (randBoolean()) {
      cdaContext.with {
        performed CPT('99341') from '20151003' to '20151003'
        performed CPT('99341') from '20150615' to '20150615'
      }
    } else {
      x12ContextFactory.context.with {
        performed CPT('99341') from '20151003' to '20151003'
        performed CPT('99341') from '20150615' to '20150615'
      }
    }
    return calcAge(cdaContext.patient.birthTime) > 65
  }

  @Override
  boolean applyCompliant(CdaContext cdaContext, X12ContextFactory x12ContextFactory) {
    if (randBoolean()) {
      cdaContext.with {
        performed SnomedCt('12843005') from '20141120' to '20141120'
        performed CPT('99341') from '20151003' to '20151003'
        performed CPT('99341') from '20150615' to '20150615'
        performed SnomedCt('442333005') from '20141120' to '20141120' withStatus 'PERFORMED'
      }
    } else {
      x12ContextFactory.context.with {
        performed SnomedCt('12843005') from '20141120' to '20141120'
        performed CPT('99341') from '20151003' to '20151003'
        performed CPT('99341') from '20150615' to '20150615'
        performed SnomedCt('442333005') from '20141120' to '20141120' withStatus 'PERFORMED'
      }
    }
    return calcAge(cdaContext.patient.birthTime) > 65
  }
}
