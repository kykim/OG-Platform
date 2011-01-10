/**
 * Copyright (C) 2009 - Present by OpenGamma Inc.
 *
 * Please see distribution for license.
 */
package com.opengamma.util.timeseries.fudge;

import javax.time.calendar.LocalDate;

import org.fudgemsg.mapping.FudgeBuilderFor;

import com.opengamma.util.timeseries.DateTimeConverter;
import com.opengamma.util.timeseries.fast.FastTimeSeries;
import com.opengamma.util.timeseries.fast.integer.FastMutableIntDoubleTimeSeries;
import com.opengamma.util.timeseries.localdate.ListLocalDateDoubleTimeSeries;

/**
 * Fudge message encoder/decoder (builder) for ListLocalDateDoubleTimeSeries
 */
@FudgeBuilderFor(ListLocalDateDoubleTimeSeries.class)
public class ListLocalDateDoubleTimeSeriesBuilder extends FastBackedDoubleTimeSeriesBuilder<LocalDate, ListLocalDateDoubleTimeSeries> {
  @Override
  public ListLocalDateDoubleTimeSeries makeSeries(DateTimeConverter<LocalDate> converter, FastTimeSeries<?> dts) {
    return new ListLocalDateDoubleTimeSeries(converter, (FastMutableIntDoubleTimeSeries) dts);
  }
}
