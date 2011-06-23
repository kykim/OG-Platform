/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.instrument.future;

import javax.time.calendar.Period;
import javax.time.calendar.ZonedDateTime;

import com.opengamma.financial.convention.businessday.BusinessDayConvention;
import com.opengamma.financial.convention.businessday.BusinessDayConventionFactory;
import com.opengamma.financial.convention.calendar.Calendar;
import com.opengamma.financial.convention.calendar.MondayToFridayCalendar;
import com.opengamma.financial.convention.daycount.DayCount;
import com.opengamma.financial.convention.daycount.DayCountFactory;
import com.opengamma.financial.instrument.index.IborIndex;
import com.opengamma.financial.interestrate.future.definition.InterestRateFutureSecurity;
import com.opengamma.financial.interestrate.future.definition.InterestRateFutureTransaction;
import com.opengamma.financial.schedule.ScheduleCalculator;
import com.opengamma.util.money.Currency;
import com.opengamma.util.time.DateUtil;

/**
 * Contains a set of Future instruments that can be used in tests.
 */
public class FutureInstrumentsDescriptionDataSet {
  //EURIBOR 3M Index
  private static final Period TENOR = Period.ofMonths(3);
  private static final int SETTLEMENT_DAYS = 2;
  private static final Calendar CALENDAR = new MondayToFridayCalendar("A");
  private static final DayCount DAY_COUNT_INDEX = DayCountFactory.INSTANCE.getDayCount("Actual/360");
  private static final BusinessDayConvention BUSINESS_DAY = BusinessDayConventionFactory.INSTANCE.getBusinessDayConvention("Modified Following");
  private static final boolean IS_EOM = true;
  private static final Currency CUR = Currency.EUR;
  private static final IborIndex IBOR_INDEX = new IborIndex(CUR, TENOR, SETTLEMENT_DAYS, CALENDAR, DAY_COUNT_INDEX, BUSINESS_DAY, IS_EOM);
  // Future
  private static final ZonedDateTime SPOT_LAST_TRADING_DATE = DateUtil.getUTCDate(2012, 9, 19);
  private static final ZonedDateTime LAST_TRADING_DATE = ScheduleCalculator.getAdjustedDate(SPOT_LAST_TRADING_DATE, CALENDAR, -SETTLEMENT_DAYS);
  private static final double NOTIONAL = 1000000.0; // 1m
  private static final double FUTURE_FACTOR = 0.25;
  private static final String NAME = "ERU2";
  private static final InterestRateFutureSecurityDefinition ERU2_DEFINITION = new InterestRateFutureSecurityDefinition(LAST_TRADING_DATE, IBOR_INDEX, NOTIONAL, FUTURE_FACTOR, NAME);
  // Transaction
  private static final int QUANTITY = -123;
  private static final ZonedDateTime TRADE_DATE = DateUtil.getUTCDate(2011, 5, 12);
  private static final double TRADE_PRICE = 0.985;
  private static final double LAST_MARGIN_PRICE = 0.99;
  // Derivatives
  private static final ZonedDateTime REFERENCE_DATE = DateUtil.getUTCDate(2011, 5, 13);
  private static final String DISCOUNTING_CURVE_NAME = "Funding";
  private static final String FORWARD_CURVE_NAME = "Forward";
  private static final String[] CURVES = {DISCOUNTING_CURVE_NAME, FORWARD_CURVE_NAME};

  public static InterestRateFutureSecurityDefinition createInterestRateFutureSecurityDefinition() {
    return new InterestRateFutureSecurityDefinition(LAST_TRADING_DATE, IBOR_INDEX, NOTIONAL, FUTURE_FACTOR, NAME);
  }

  public static InterestRateFutureSecurity createInterestRateFutureSecurity() {
    return createInterestRateFutureSecurityDefinition().toDerivative(REFERENCE_DATE, CURVES);
  }

  public static InterestRateFutureTransactionDefinition createInterestRateFutureTransactionDefinition() {
    return new InterestRateFutureTransactionDefinition(ERU2_DEFINITION, QUANTITY, TRADE_DATE, TRADE_PRICE);
  }

  public static InterestRateFutureTransaction createInterestRateFutureTransaction() {
    return createInterestRateFutureTransactionDefinition().toDerivative(REFERENCE_DATE, LAST_MARGIN_PRICE, CURVES);
  }
}