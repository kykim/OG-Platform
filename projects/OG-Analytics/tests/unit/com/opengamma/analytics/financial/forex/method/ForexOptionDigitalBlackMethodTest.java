/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.analytics.financial.forex.method;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Map;

import javax.time.calendar.Period;
import javax.time.calendar.ZonedDateTime;

import org.testng.annotations.Test;
import org.testng.internal.junit.ArrayAsserts;

import com.opengamma.analytics.financial.forex.calculator.CurrencyExposureBlackForexCalculator;
import com.opengamma.analytics.financial.forex.calculator.PresentValueBlackForexCalculator;
import com.opengamma.analytics.financial.forex.calculator.PresentValueBlackVolatilityQuoteSensitivityForexCalculator;
import com.opengamma.analytics.financial.forex.calculator.PresentValueBlackVolatilitySensitivityBlackForexCalculator;
import com.opengamma.analytics.financial.forex.calculator.PresentValueCurveSensitivityBlackForexCalculator;
import com.opengamma.analytics.financial.forex.definition.ForexDefinition;
import com.opengamma.analytics.financial.forex.definition.ForexOptionDigitalDefinition;
import com.opengamma.analytics.financial.forex.derivative.Forex;
import com.opengamma.analytics.financial.forex.derivative.ForexOptionDigital;
import com.opengamma.analytics.financial.interestrate.InstrumentDerivative;
import com.opengamma.analytics.financial.interestrate.PresentValueCalculator;
import com.opengamma.analytics.financial.interestrate.YieldCurveBundle;
import com.opengamma.analytics.financial.model.interestrate.curve.YieldAndDiscountCurve;
import com.opengamma.analytics.financial.model.interestrate.curve.YieldCurve;
import com.opengamma.analytics.financial.model.option.definition.SmileDeltaTermStructureDataBundle;
import com.opengamma.analytics.financial.model.option.definition.SmileDeltaTermStructureParametersStrikeInterpolation;
import com.opengamma.analytics.financial.schedule.ScheduleCalculator;
import com.opengamma.analytics.math.curve.InterpolatedDoublesCurve;
import com.opengamma.analytics.math.interpolation.LinearInterpolator1D;
import com.opengamma.analytics.math.statistics.distribution.NormalDistribution;
import com.opengamma.analytics.math.statistics.distribution.ProbabilityDistribution;
import com.opengamma.analytics.util.time.TimeCalculator;
import com.opengamma.financial.convention.businessday.BusinessDayConvention;
import com.opengamma.financial.convention.businessday.BusinessDayConventionFactory;
import com.opengamma.financial.convention.calendar.Calendar;
import com.opengamma.financial.convention.calendar.MondayToFridayCalendar;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.time.DateUtils;
import com.opengamma.util.tuple.DoublesPair;
import com.opengamma.util.tuple.ObjectsPair;
import com.opengamma.util.tuple.Pair;
import com.opengamma.util.tuple.Triple;

/**
 * Tests related to the pricing method for digital Forex option transactions with Black function and a volatility provider.
 */
public class ForexOptionDigitalBlackMethodTest {
  // General
  private static final Calendar CALENDAR = new MondayToFridayCalendar("A");
  private static final BusinessDayConvention BUSINESS_DAY = BusinessDayConventionFactory.INSTANCE.getBusinessDayConvention("Modified Following");
  private static final int SETTLEMENT_DAYS = 2;
  // Smile data
  private static final Currency EUR = Currency.EUR;
  private static final Currency USD = Currency.USD;
  private static final ZonedDateTime REFERENCE_DATE = DateUtils.getUTCDate(2011, 6, 13);
  private static final FXMatrix FX_MATRIX = TestsDataSetsForex.fxMatrix();
  private static final double SPOT = FX_MATRIX.getFxRate(EUR, USD);
  private static final SmileDeltaTermStructureParametersStrikeInterpolation SMILE_TERM = TestsDataSetsForex.smile5points(REFERENCE_DATE);
  private static final SmileDeltaTermStructureParametersStrikeInterpolation SMILE_TERM_FLAT = TestsDataSetsForex.smileFlat(REFERENCE_DATE);
  // Methods and curves
  private static final YieldCurveBundle CURVES = TestsDataSetsForex.createCurvesForex();
  private static final String[] CURVES_NAME = TestsDataSetsForex.curveNames();
  private static final Map<String, Currency> CURVE_CURRENCY = TestsDataSetsForex.curveCurrency();
  private static final SmileDeltaTermStructureDataBundle SMILE_BUNDLE = new SmileDeltaTermStructureDataBundle(FX_MATRIX, CURVE_CURRENCY, CURVES, SMILE_TERM, Pair.of(EUR, USD));
  private static final ProbabilityDistribution<Double> NORMAL = new NormalDistribution(0, 1);
  private static final ForexOptionDigitalBlackMethod METHOD_BLACK_DIGITAL = ForexOptionDigitalBlackMethod.getInstance();
  private static final PresentValueBlackForexCalculator PVC_BLACK = PresentValueBlackForexCalculator.getInstance();
  private static final PresentValueCalculator PVC = PresentValueCalculator.getInstance();
  private static final CurrencyExposureBlackForexCalculator CEC_BLACK = CurrencyExposureBlackForexCalculator.getInstance();
  private static final PresentValueCurveSensitivityBlackForexCalculator PVCSC_BLACK = PresentValueCurveSensitivityBlackForexCalculator.getInstance();
  private static final PresentValueBlackVolatilitySensitivityBlackForexCalculator PVVSC_BLACK = PresentValueBlackVolatilitySensitivityBlackForexCalculator.getInstance();
  // option
  private static final double STRIKE = 1.45;
  private static final boolean IS_CALL = true;
  private static final boolean IS_LONG = true;
  private static final double NOTIONAL = 100000000;
  private static final ZonedDateTime OPTION_PAY_DATE = ScheduleCalculator.getAdjustedDate(REFERENCE_DATE, Period.ofMonths(9), BUSINESS_DAY, CALENDAR);
  private static final ZonedDateTime OPTION_EXP_DATE = ScheduleCalculator.getAdjustedDate(OPTION_PAY_DATE, -SETTLEMENT_DAYS, CALENDAR);
  private static final ForexDefinition FOREX_DEFINITION = new ForexDefinition(EUR, USD, OPTION_PAY_DATE, NOTIONAL, STRIKE);
  private static final ForexOptionDigitalDefinition FOREX_DIGITAL_CALL_DOM_DEFINITION = new ForexOptionDigitalDefinition(FOREX_DEFINITION, OPTION_EXP_DATE, IS_CALL, IS_LONG, true);
  private static final ForexOptionDigital FOREX_DIGITAL_CALL_DOM = FOREX_DIGITAL_CALL_DOM_DEFINITION.toDerivative(REFERENCE_DATE, CURVES_NAME);
  private static final ForexOptionDigitalDefinition FOREX_DIGITAL_CALL_FOR_DEFINITION = new ForexOptionDigitalDefinition(FOREX_DEFINITION, OPTION_EXP_DATE, IS_CALL, IS_LONG, false);
  private static final ForexOptionDigital FOREX_DIGITAL_CALL_FOR = FOREX_DIGITAL_CALL_FOR_DEFINITION.toDerivative(REFERENCE_DATE, CURVES_NAME);
  private static final double TOLERANCE_PRICE = 1.0E-2;
  private static final double TOLERANCE_DELTA = 1.0E+2; // 0.01 currency unit for 1 bp
  private static final double TOLERANCE_TIME = 1.0E-6;

  @Test
  /**
   * Tests the present value at a time grid point.
   */
  public void persentValueAtGridPoint() {
    final double strike = 1.45;
    final boolean isCall = true;
    final boolean isLong = true;
    final double notional = 100000000;
    ZonedDateTime expiryDate = ScheduleCalculator.getAdjustedDate(REFERENCE_DATE, Period.ofYears(1), BUSINESS_DAY, CALENDAR, true);
    final ForexDefinition forexUnderlyingDefinition = new ForexDefinition(EUR, USD, ScheduleCalculator.getAdjustedDate(expiryDate, SETTLEMENT_DAYS, CALENDAR), notional, strike);
    final ForexOptionDigitalDefinition forexOptionDefinition = new ForexOptionDigitalDefinition(forexUnderlyingDefinition, expiryDate, isCall, isLong);
    final ForexOptionDigital forexOption = forexOptionDefinition.toDerivative(REFERENCE_DATE, CURVES_NAME);
    final double dfDomestic = CURVES.getCurve(CURVES_NAME[1]).getDiscountFactor(forexOption.getUnderlyingForex().getPaymentTime());
    final double dfForeign = CURVES.getCurve(CURVES_NAME[0]).getDiscountFactor(forexOption.getUnderlyingForex().getPaymentTime());
    final double forward = SPOT * dfForeign / dfDomestic;
    final double volatility = SMILE_TERM.getVolatility(new Triple<Double, Double, Double>(TimeCalculator.getTimeBetween(REFERENCE_DATE, expiryDate), strike, forward));
    final double sigmaRootT = volatility * Math.sqrt(forexOption.getExpirationTime());
    final double dM = Math.log(forward / strike) / sigmaRootT - 0.5 * sigmaRootT;
    final int omega = isCall ? 1 : -1;
    final double pvExpected = Math.abs(forexOption.getUnderlyingForex().getPaymentCurrency2().getAmount()) * dfDomestic * NORMAL.getCDF(omega * dM) * (isLong ? 1.0 : -1.0);
    final MultipleCurrencyAmount pvComputed = METHOD_BLACK_DIGITAL.presentValue(forexOption, SMILE_BUNDLE);
    assertEquals("Forex Digital option: present value", pvExpected, pvComputed.getAmount(USD), TOLERANCE_PRICE);
  }

  @Test
  /**
   * Tests the present value against an explicit computation. The amount is paid in the domestic currency.
   */
  public void presentValueDomestic() {
    final double strike = 1.45;
    final boolean isCall = true;
    final boolean isLong = true;
    final double notional = 100000000;
    final ZonedDateTime payDate = ScheduleCalculator.getAdjustedDate(REFERENCE_DATE, Period.ofMonths(9), BUSINESS_DAY, CALENDAR);
    final ZonedDateTime expDate = ScheduleCalculator.getAdjustedDate(payDate, -SETTLEMENT_DAYS, CALENDAR);
    final double timeToExpiry = TimeCalculator.getTimeBetween(REFERENCE_DATE, expDate);
    final ForexDefinition forexUnderlyingDefinition = new ForexDefinition(EUR, USD, payDate, notional, strike);
    final ForexOptionDigitalDefinition forexOptionDefinition = new ForexOptionDigitalDefinition(forexUnderlyingDefinition, expDate, isCall, isLong);
    final ForexOptionDigital forexOption = forexOptionDefinition.toDerivative(REFERENCE_DATE, CURVES_NAME);
    final double dfDomestic = CURVES.getCurve(CURVES_NAME[1]).getDiscountFactor(forexOption.getUnderlyingForex().getPaymentTime());
    final double dfForeign = CURVES.getCurve(CURVES_NAME[0]).getDiscountFactor(forexOption.getUnderlyingForex().getPaymentTime());
    final double forward = SPOT * dfForeign / dfDomestic;
    final double volatility = SMILE_TERM.getVolatility(new Triple<Double, Double, Double>(timeToExpiry, strike, forward));
    final double sigmaRootT = volatility * Math.sqrt(forexOption.getExpirationTime());
    final double dM = Math.log(forward / strike) / sigmaRootT - 0.5 * sigmaRootT;
    final int omega = isCall ? 1 : -1;
    final double pvExpected = Math.abs(forexOption.getUnderlyingForex().getPaymentCurrency2().getAmount()) * dfDomestic * NORMAL.getCDF(omega * dM) * (isLong ? 1.0 : -1.0);
    final MultipleCurrencyAmount pvComputed = METHOD_BLACK_DIGITAL.presentValue(forexOption, SMILE_BUNDLE);
    assertEquals("Forex Digital option: present value", pvExpected, pvComputed.getAmount(USD), TOLERANCE_PRICE);
  }

  @Test(enabled = false)
  /**
   * Profile the present value against an explicit computation. The amount is paid in the domestic currency.
   */
  public void presentValueDomesticProfile() {
    final double strike = 1.45;
    final boolean isCall = true;
    final boolean isLong = true;
    final ZonedDateTime payDate = ScheduleCalculator.getAdjustedDate(REFERENCE_DATE, Period.ofMonths(9), BUSINESS_DAY, CALENDAR);
    final ZonedDateTime expDate = ScheduleCalculator.getAdjustedDate(payDate, -SETTLEMENT_DAYS, CALENDAR);
    final ForexDefinition forexUnderlyingDefinition = new ForexDefinition(EUR, USD, payDate, 1.0 / strike, strike);
    final ForexOptionDigitalDefinition forexOptionDefinition = new ForexOptionDigitalDefinition(forexUnderlyingDefinition, expDate, isCall, isLong);
    final ForexOptionDigital forexOption = forexOptionDefinition.toDerivative(REFERENCE_DATE, CURVES_NAME);
    int nbSpot = 50;
    double range = 0.75;
    double[] spot = new double[nbSpot + 1];
    double[] pv = new double[nbSpot + 1];
    for (int loopspot = 0; loopspot <= nbSpot; loopspot++) {
      spot[loopspot] = strike - range + 2.0d * range * loopspot / nbSpot;
      FXMatrix fxMatrix = new FXMatrix(EUR, USD, spot[loopspot]);
      SmileDeltaTermStructureDataBundle smileBundle = new SmileDeltaTermStructureDataBundle(fxMatrix, CURVE_CURRENCY, CURVES, SMILE_TERM, Pair.of(EUR, USD));
      pv[loopspot] = METHOD_BLACK_DIGITAL.presentValue(forexOption, smileBundle).getAmount(USD);
    }
    double test = 0.0;
    test++;
  }

  @Test
  /**
   * Tests the present value against an explicit computation. The amount is paid in the foreign currency.
   */
  public void presentValueForeign() {
    final double strike = 1.45;
    final boolean isCall = true;
    final boolean isLong = true;
    final boolean payDomestic = false;
    final double notional = 100000000;
    final ZonedDateTime payDate = ScheduleCalculator.getAdjustedDate(REFERENCE_DATE, Period.ofMonths(9), BUSINESS_DAY, CALENDAR);
    final ZonedDateTime expDate = ScheduleCalculator.getAdjustedDate(payDate, -SETTLEMENT_DAYS, CALENDAR);
    final double timeToExpiry = TimeCalculator.getTimeBetween(REFERENCE_DATE, expDate);
    final ForexDefinition forexUnderlyingDefinition = new ForexDefinition(EUR, USD, payDate, notional, strike);
    final ForexOptionDigitalDefinition forexOptionDefinition = new ForexOptionDigitalDefinition(forexUnderlyingDefinition, expDate, isCall, isLong, payDomestic);
    final ForexOptionDigital forexOption = forexOptionDefinition.toDerivative(REFERENCE_DATE, CURVES_NAME);
    final double dfDomestic = CURVES.getCurve(CURVES_NAME[0]).getDiscountFactor(forexOption.getUnderlyingForex().getPaymentTime());
    final double dfForeign = CURVES.getCurve(CURVES_NAME[1]).getDiscountFactor(forexOption.getUnderlyingForex().getPaymentTime());
    final double forward = 1 / SPOT * dfForeign / dfDomestic;
    final double volatility = SMILE_TERM.getVolatility(new Triple<Double, Double, Double>(timeToExpiry, strike, 1 / forward));
    final double sigmaRootT = volatility * Math.sqrt(forexOption.getExpirationTime());
    final double dM = Math.log(forward * strike) / sigmaRootT - 0.5 * sigmaRootT;
    final double omega = isCall ? -1.0 : 1.0;
    final double pvExpected = Math.abs(forexOption.getUnderlyingForex().getPaymentCurrency1().getAmount()) * dfDomestic * NORMAL.getCDF(omega * dM) * (isLong ? 1.0 : -1.0);
    final MultipleCurrencyAmount pvComputed = METHOD_BLACK_DIGITAL.presentValue(forexOption, SMILE_BUNDLE);
    assertEquals("Forex Digital option: present value", pvExpected, pvComputed.getAmount(EUR), TOLERANCE_PRICE);
  }

  @Test(enabled = false)
  /**
   * Profile the present value against an explicit computation. The amount is paid in the domestic currency.
   */
  public void presentValueDomesticForeign() {
    final double strike = 1.45;
    final boolean isCall = true;
    final boolean isLong = true;
    final ZonedDateTime payDate = ScheduleCalculator.getAdjustedDate(REFERENCE_DATE, Period.ofMonths(9), BUSINESS_DAY, CALENDAR);
    final ZonedDateTime expDate = ScheduleCalculator.getAdjustedDate(payDate, -SETTLEMENT_DAYS, CALENDAR);
    final ForexDefinition forexUnderlyingDefinition = new ForexDefinition(EUR, USD, payDate, 1.0, strike);
    final ForexOptionDigitalDefinition forexOptionDefinition = new ForexOptionDigitalDefinition(forexUnderlyingDefinition, expDate, isCall, isLong, false);
    final ForexOptionDigital forexOption = forexOptionDefinition.toDerivative(REFERENCE_DATE, CURVES_NAME);
    int nbSpot = 50;
    double range = 0.75;
    double[] spot = new double[nbSpot + 1];
    double[] pv = new double[nbSpot + 1];
    for (int loopspot = 0; loopspot <= nbSpot; loopspot++) {
      spot[loopspot] = strike - range + 2.0d * range * loopspot / nbSpot;
      FXMatrix fxMatrix = new FXMatrix(EUR, USD, spot[loopspot]);
      SmileDeltaTermStructureDataBundle smileBundle = new SmileDeltaTermStructureDataBundle(fxMatrix, CURVE_CURRENCY, CURVES, SMILE_TERM, Pair.of(EUR, USD));
      pv[loopspot] = METHOD_BLACK_DIGITAL.presentValue(forexOption, smileBundle).getAmount(EUR);
    }
    double test = 0.0;
    test++;
  }

  @Test
  /**
   * Tests put call parity.
   */
  public void presentValuePutCallParity() {
    final double strike = 1.45;
    final boolean isCall = true;
    final boolean isLong = true;
    final double notional = 100000000;
    final ZonedDateTime payDate = ScheduleCalculator.getAdjustedDate(REFERENCE_DATE, Period.ofMonths(9), BUSINESS_DAY, CALENDAR);
    final ZonedDateTime expDate = ScheduleCalculator.getAdjustedDate(payDate, -SETTLEMENT_DAYS, CALENDAR);
    final ForexDefinition forexUnderlyingDefinition = new ForexDefinition(EUR, USD, payDate, notional, strike);
    final ForexOptionDigitalDefinition callDefinition = new ForexOptionDigitalDefinition(forexUnderlyingDefinition, expDate, isCall, isLong);
    final ForexOptionDigitalDefinition putDefinition = new ForexOptionDigitalDefinition(forexUnderlyingDefinition, expDate, !isCall, isLong);
    final ForexOptionDigital call = callDefinition.toDerivative(REFERENCE_DATE, CURVES_NAME);
    final ForexOptionDigital put = putDefinition.toDerivative(REFERENCE_DATE, CURVES_NAME);
    final MultipleCurrencyAmount pvCall = METHOD_BLACK_DIGITAL.presentValue(call, SMILE_BUNDLE);
    final MultipleCurrencyAmount pvPut = METHOD_BLACK_DIGITAL.presentValue(put, SMILE_BUNDLE);
    final Double pvCash = PVC.visit(put.getUnderlyingForex().getPaymentCurrency2(), CURVES);
    assertEquals("Forex Digital option: present value", pvCall.getAmount(USD) + pvPut.getAmount(USD), Math.abs(pvCash), TOLERANCE_PRICE);
  }

  @Test
  /**
   * Tests the present value Method versus the Calculator.
   */
  public void presentValueMethodVsCalculator() {
    final double strike = 1.45;
    final boolean isCall = true;
    final boolean isLong = true;
    final double notional = 100000000;
    final ZonedDateTime payDate = ScheduleCalculator.getAdjustedDate(REFERENCE_DATE, Period.ofMonths(9), BUSINESS_DAY, CALENDAR);
    final ZonedDateTime expDate = ScheduleCalculator.getAdjustedDate(payDate, -SETTLEMENT_DAYS, CALENDAR);
    final ForexDefinition forexUnderlyingDefinition = new ForexDefinition(EUR, USD, payDate, notional, strike);
    final ForexOptionDigitalDefinition forexOptionDefinition = new ForexOptionDigitalDefinition(forexUnderlyingDefinition, expDate, isCall, isLong);
    final InstrumentDerivative forexOption = forexOptionDefinition.toDerivative(REFERENCE_DATE, CURVES_NAME);
    final MultipleCurrencyAmount pvMethod = METHOD_BLACK_DIGITAL.presentValue(forexOption, SMILE_BUNDLE);
    final MultipleCurrencyAmount pvCalculator = PVC_BLACK.visit(forexOption, SMILE_BUNDLE);
    assertEquals("Forex Digital option: present value Method vs Calculator", pvMethod.getAmount(USD), pvCalculator.getAmount(USD), 1E-2);
  }

  @Test
  /**
   * Tests the present value long/short parity.
   */
  public void presentValueLongShort() {
    final ForexOptionDigitalDefinition forexOptionShortDefinition = new ForexOptionDigitalDefinition(FOREX_DEFINITION, OPTION_EXP_DATE, IS_CALL, !IS_LONG);
    final InstrumentDerivative forexOptionShort = forexOptionShortDefinition.toDerivative(REFERENCE_DATE, CURVES_NAME);
    final MultipleCurrencyAmount pvShort = METHOD_BLACK_DIGITAL.presentValue(forexOptionShort, SMILE_BUNDLE);
    final MultipleCurrencyAmount pvLong = METHOD_BLACK_DIGITAL.presentValue(FOREX_DIGITAL_CALL_DOM, SMILE_BUNDLE);
    assertEquals("Forex Digital option: present value long/short parity", pvLong.getAmount(USD), -pvShort.getAmount(USD), 1E-2);
    final MultipleCurrencyAmount ceShort = METHOD_BLACK_DIGITAL.currencyExposure(forexOptionShort, SMILE_BUNDLE);
    final MultipleCurrencyAmount ceLong = METHOD_BLACK_DIGITAL.currencyExposure(FOREX_DIGITAL_CALL_DOM, SMILE_BUNDLE);
    assertEquals("Forex Digital option: currency exposure long/short parity", ceLong.getAmount(USD), -ceShort.getAmount(USD), 1E-2);
    assertEquals("Forex Digital option: currency exposure long/short parity", ceLong.getAmount(EUR), -ceShort.getAmount(EUR), 1E-2);
  }

  @Test
  /**
   * Tests the currency exposure against an explicit computation.
   */
  public void currencyExposure() {
    final double strike = 1.45;
    final boolean isCall = true;
    final boolean isLong = true;
    final double notional = 100000000;
    final ZonedDateTime payDate = ScheduleCalculator.getAdjustedDate(REFERENCE_DATE, Period.ofMonths(9), BUSINESS_DAY, CALENDAR);
    final ZonedDateTime expDate = ScheduleCalculator.getAdjustedDate(payDate, -SETTLEMENT_DAYS, CALENDAR);
    final double timeToExpiry = TimeCalculator.getTimeBetween(REFERENCE_DATE, expDate);
    final ForexDefinition forexUnderlyingDefinition = new ForexDefinition(EUR, USD, payDate, notional, strike);
    final ForexOptionDigitalDefinition forexOptionDefinitionCall = new ForexOptionDigitalDefinition(forexUnderlyingDefinition, expDate, isCall, isLong);
    final ForexOptionDigitalDefinition forexOptionDefinitionPut = new ForexOptionDigitalDefinition(forexUnderlyingDefinition, expDate, !isCall, isLong);
    final ForexOptionDigital forexOptionCall = forexOptionDefinitionCall.toDerivative(REFERENCE_DATE, CURVES_NAME);
    final ForexOptionDigital forexOptionPut = forexOptionDefinitionPut.toDerivative(REFERENCE_DATE, CURVES_NAME);
    final double dfDomestic = CURVES.getCurve(CURVES_NAME[1]).getDiscountFactor(TimeCalculator.getTimeBetween(REFERENCE_DATE, payDate)); // USD
    final double dfForeign = CURVES.getCurve(CURVES_NAME[0]).getDiscountFactor(TimeCalculator.getTimeBetween(REFERENCE_DATE, payDate)); // EUR
    final double forward = SPOT * dfForeign / dfDomestic;
    final double volatility = SMILE_TERM.getVolatility(new Triple<Double, Double, Double>(timeToExpiry, strike, forward));
    final double sigmaRootT = volatility * Math.sqrt(forexOptionCall.getExpirationTime());
    final double dM = Math.log(forward / strike) / sigmaRootT - 0.5 * sigmaRootT;
    final double deltaSpotCall = dfDomestic * notional * strike * NORMAL.getPDF(dM) / (sigmaRootT * SPOT);
    final double deltaSpotPut = -dfDomestic * notional * strike * NORMAL.getPDF(dM) / (sigmaRootT * SPOT);
    final MultipleCurrencyAmount priceComputedCall = METHOD_BLACK_DIGITAL.presentValue(forexOptionCall, SMILE_BUNDLE);
    final MultipleCurrencyAmount priceComputedPut = METHOD_BLACK_DIGITAL.presentValue(forexOptionPut, SMILE_BUNDLE);
    final MultipleCurrencyAmount currencyExposureCallComputed = METHOD_BLACK_DIGITAL.currencyExposure(forexOptionCall, SMILE_BUNDLE);
    assertEquals("Forex Digital option: currency exposure foreign - call", deltaSpotCall, currencyExposureCallComputed.getAmount(EUR), 1E-2);
    assertEquals("Forex Digital option: currency exposure domestic - call", -deltaSpotCall * SPOT + priceComputedCall.getAmount(USD), currencyExposureCallComputed.getAmount(USD), 1E-2);
    final MultipleCurrencyAmount currencyExposurePutComputed = METHOD_BLACK_DIGITAL.currencyExposure(forexOptionPut, SMILE_BUNDLE);
    assertEquals("Forex Digital option: currency exposure foreign- put", deltaSpotPut, currencyExposurePutComputed.getAmount(EUR), 1E-2);
    assertEquals("Forex Digital option: currency exposure domestic - put", -deltaSpotPut * SPOT + priceComputedPut.getAmount(USD), currencyExposurePutComputed.getAmount(USD), 1E-2);
  }

  @Test
  /**
   * Tests the currency exposure with a FD rate shift.
   */
  public void currencyExposureForeign2() {
    double shift = 0.000005;
    FXMatrix fxMatrix = new FXMatrix(EUR, USD, SPOT);
    FXMatrix fxMatrixP = new FXMatrix(EUR, USD, SPOT + shift);
    SmileDeltaTermStructureDataBundle smileBundle = new SmileDeltaTermStructureDataBundle(fxMatrix, CURVE_CURRENCY, CURVES, SMILE_TERM_FLAT, Pair.of(EUR, USD));
    SmileDeltaTermStructureDataBundle smileBundleP = new SmileDeltaTermStructureDataBundle(fxMatrixP, CURVE_CURRENCY, CURVES, SMILE_TERM_FLAT, Pair.of(EUR, USD));
    MultipleCurrencyAmount ce = METHOD_BLACK_DIGITAL.currencyExposure(FOREX_DIGITAL_CALL_FOR, smileBundle);
    MultipleCurrencyAmount pv = METHOD_BLACK_DIGITAL.presentValue(FOREX_DIGITAL_CALL_FOR, smileBundle);
    MultipleCurrencyAmount pvP = METHOD_BLACK_DIGITAL.presentValue(FOREX_DIGITAL_CALL_FOR, smileBundleP);
    assertEquals("Forex Digital option: call spread method - currency exposure - PL EUR", pvP.getAmount(EUR) - pv.getAmount(EUR), ce.getAmount(USD) * (1.0 / (SPOT + shift) - 1 / SPOT),
        TOLERANCE_PRICE);
    assertEquals("Forex Digital option: call spread method - currency exposure - PL USD", pvP.getAmount(EUR) * (SPOT + shift) - pv.getAmount(EUR) * SPOT, ce.getAmount(EUR) * (SPOT + shift - SPOT),
        TOLERANCE_PRICE);
  }

  @Test
  /**
   * Tests the currency exposure against the present value.
   */
  public void currencyExposureVsPresentValue() {
    MultipleCurrencyAmount pv = METHOD_BLACK_DIGITAL.presentValue(FOREX_DIGITAL_CALL_DOM, SMILE_BUNDLE);
    MultipleCurrencyAmount ce = METHOD_BLACK_DIGITAL.currencyExposure(FOREX_DIGITAL_CALL_DOM, SMILE_BUNDLE);
    assertEquals("Forex Digital option: currency exposure vs present value", ce.getAmount(USD) + ce.getAmount(EUR) * SPOT, pv.getAmount(USD), 1E-2);
  }

  @Test
  /**
   * Tests the put/call parity currency exposure.
   */
  public void currencyExposurePutCallParity() {
    final double strike = 1.45;
    final boolean isCall = true;
    final boolean isLong = true;
    final double notional = 100000000;
    final ZonedDateTime payDate = ScheduleCalculator.getAdjustedDate(REFERENCE_DATE, Period.ofMonths(9), BUSINESS_DAY, CALENDAR);
    final ZonedDateTime expDate = ScheduleCalculator.getAdjustedDate(payDate, -SETTLEMENT_DAYS, CALENDAR);
    final ForexDefinition forexUnderlyingDefinition = new ForexDefinition(EUR, USD, payDate, notional, strike);
    final ForexOptionDigitalDefinition forexOptionDefinitionCall = new ForexOptionDigitalDefinition(forexUnderlyingDefinition, expDate, isCall, isLong);
    final ForexOptionDigitalDefinition forexOptionDefinitionPut = new ForexOptionDigitalDefinition(forexUnderlyingDefinition, expDate, !isCall, isLong);
    final ForexOptionDigital forexOptionCall = forexOptionDefinitionCall.toDerivative(REFERENCE_DATE, CURVES_NAME);
    final ForexOptionDigital forexOptionPut = forexOptionDefinitionPut.toDerivative(REFERENCE_DATE, CURVES_NAME);
    final MultipleCurrencyAmount currencyExposureCall = METHOD_BLACK_DIGITAL.currencyExposure(forexOptionCall, SMILE_BUNDLE);
    final MultipleCurrencyAmount currencyExposurePut = METHOD_BLACK_DIGITAL.currencyExposure(forexOptionPut, SMILE_BUNDLE);
    final Double pvCash = PVC.visit(forexOptionPut.getUnderlyingForex().getPaymentCurrency2(), CURVES);
    assertEquals("Forex Digital option: currency exposure put/call parity foreign", 0, currencyExposureCall.getAmount(EUR) + currencyExposurePut.getAmount(EUR), TOLERANCE_PRICE);
    assertEquals("Forex Digital option: currency exposure put/call parity domestic", Math.abs(pvCash), currencyExposureCall.getAmount(USD) + currencyExposurePut.getAmount(USD), TOLERANCE_PRICE);
  }

  @Test
  /**
   * Tests currency exposure Method vs Calculator.
   */
  public void currencyExposureMethodVsCalculator() {
    final double strike = 1.45;
    final boolean isCall = true;
    final boolean isLong = true;
    final double notional = 100000000;
    final ZonedDateTime payDate = ScheduleCalculator.getAdjustedDate(REFERENCE_DATE, Period.ofMonths(9), BUSINESS_DAY, CALENDAR);
    final ZonedDateTime expDate = ScheduleCalculator.getAdjustedDate(payDate, -SETTLEMENT_DAYS, CALENDAR);
    final ForexDefinition forexUnderlyingDefinition = new ForexDefinition(EUR, USD, payDate, notional, strike);
    final ForexOptionDigitalDefinition forexOptionDefinition = new ForexOptionDigitalDefinition(forexUnderlyingDefinition, expDate, isCall, isLong);
    final InstrumentDerivative forexOption = forexOptionDefinition.toDerivative(REFERENCE_DATE, CURVES_NAME);
    final MultipleCurrencyAmount ceMethod = METHOD_BLACK_DIGITAL.currencyExposure(forexOption, SMILE_BUNDLE);
    final MultipleCurrencyAmount ceCalculator = CEC_BLACK.visit(forexOption, SMILE_BUNDLE);
    assertEquals("Forex Digital option: currency exposure Method vs Calculator", ceMethod.getAmount(EUR), ceCalculator.getAmount(EUR), 1E-2);
    assertEquals("Forex Digital option: currency exposure Method vs Calculator", ceMethod.getAmount(USD), ceCalculator.getAmount(USD), 1E-2);
  }

  @Test
  /**
   * Tests the present value curve sensitivity.
   */
  public void presentValueCurveSensitivity() {
    final double strike = 1.45;
    final boolean isCall = true;
    final boolean isLong = true;
    final double notional = 100000000;
    final ZonedDateTime payDate = ScheduleCalculator.getAdjustedDate(REFERENCE_DATE, Period.ofMonths(9), BUSINESS_DAY, CALENDAR);
    final ZonedDateTime expDate = ScheduleCalculator.getAdjustedDate(payDate, -SETTLEMENT_DAYS, CALENDAR);
    final ForexDefinition forexUnderlyingDefinition = new ForexDefinition(EUR, USD, payDate, notional, strike);
    final ForexOptionDigitalDefinition forexOptionDefinitionCall = new ForexOptionDigitalDefinition(forexUnderlyingDefinition, expDate, isCall, isLong);
    final ForexOptionDigital forexOptionCall = forexOptionDefinitionCall.toDerivative(REFERENCE_DATE, CURVES_NAME);
    final Forex forexForward = forexUnderlyingDefinition.toDerivative(REFERENCE_DATE, CURVES_NAME);
    final MultipleCurrencyInterestRateCurveSensitivity sensi = METHOD_BLACK_DIGITAL.presentValueCurveSensitivity(forexOptionCall, SMILE_BUNDLE);
    final double dfDomestic = CURVES.getCurve(CURVES_NAME[1]).getDiscountFactor(forexForward.getPaymentTime());
    final double dfForeign = CURVES.getCurve(CURVES_NAME[0]).getDiscountFactor(forexForward.getPaymentTime());
    final double forward = SPOT * dfForeign / dfDomestic;
    final double volatility = SMILE_TERM.getVolatility(new Triple<Double, Double, Double>(forexOptionCall.getExpirationTime(), strike, forward));
    final double sigmaRootT = volatility * Math.sqrt(forexOptionCall.getExpirationTime());
    final int omega = isCall ? 1 : -1;
    // Finite difference
    final YieldAndDiscountCurve curveDomestic = CURVES.getCurve(forexOptionCall.getUnderlyingForex().getPaymentCurrency2().getFundingCurveName());
    final YieldAndDiscountCurve curveForeign = CURVES.getCurve(forexOptionCall.getUnderlyingForex().getPaymentCurrency1().getFundingCurveName());
    double forwardBumped;
    double dfForeignBumped;
    double dfDomesticBumped;
    double dMBumped;
    final double deltaShift = 0.00001; // 0.1 bp
    final double[] nodeTimes = new double[2];
    nodeTimes[0] = 0.0;
    nodeTimes[1] = forexOptionCall.getUnderlyingForex().getPaymentTime();
    final double[] yields = new double[2];
    YieldAndDiscountCurve curveNode;
    YieldAndDiscountCurve curveBumpedPlus;
    YieldAndDiscountCurve curveBumpedMinus;
    final String bumpedCurveName = "Bumped";
    //Foreign
    yields[0] = curveForeign.getInterestRate(nodeTimes[0]);
    yields[1] = curveForeign.getInterestRate(nodeTimes[1]);
    curveNode = new YieldCurve(InterpolatedDoublesCurve.fromSorted(nodeTimes, yields, new LinearInterpolator1D()));
    curveBumpedPlus = curveNode.withSingleShift(nodeTimes[1], deltaShift);
    curveBumpedMinus = curveNode.withSingleShift(nodeTimes[1], -deltaShift);
    final YieldCurveBundle curvesForeign = new YieldCurveBundle();
    curvesForeign.setCurve(bumpedCurveName, curveBumpedPlus);
    curvesForeign.setCurve(CURVES_NAME[1], CURVES.getCurve(CURVES_NAME[1]));
    dfForeignBumped = curveBumpedPlus.getDiscountFactor(forexForward.getPaymentTime());
    forwardBumped = SPOT * dfForeignBumped / dfDomestic;
    dMBumped = Math.log(forwardBumped / strike) / sigmaRootT - 0.5 * sigmaRootT;
    final double bumpedPvForeignPlus = Math.abs(forexOptionCall.getUnderlyingForex().getPaymentCurrency2().getAmount()) * dfDomestic * NORMAL.getCDF(omega * dMBumped) * (isLong ? 1.0 : -1.0);
    dfForeignBumped = curveBumpedMinus.getDiscountFactor(forexForward.getPaymentTime());
    forwardBumped = SPOT * dfForeignBumped / dfDomestic;
    dMBumped = Math.log(forwardBumped / strike) / sigmaRootT - 0.5 * sigmaRootT;
    final double bumpedPvForeignMinus = Math.abs(forexOptionCall.getUnderlyingForex().getPaymentCurrency2().getAmount()) * dfDomestic * NORMAL.getCDF(omega * dMBumped) * (isLong ? 1.0 : -1.0);
    final double resultForeign = (bumpedPvForeignPlus - bumpedPvForeignMinus) / (2 * deltaShift);
    assertEquals("Forex Digital option: curve sensitivity", forexForward.getPaymentTime(), sensi.getSensitivity(USD).getSensitivities().get(CURVES_NAME[0]).get(0).first, TOLERANCE_TIME);
    assertEquals("Forex Digital option: curve sensitivity", resultForeign, sensi.getSensitivity(USD).getSensitivities().get(CURVES_NAME[0]).get(0).second, TOLERANCE_DELTA);
    //Domestic
    yields[0] = curveDomestic.getInterestRate(nodeTimes[0]);
    yields[1] = curveDomestic.getInterestRate(nodeTimes[1]);
    curveNode = new YieldCurve(InterpolatedDoublesCurve.fromSorted(nodeTimes, yields, new LinearInterpolator1D()));
    curveBumpedPlus = curveNode.withSingleShift(nodeTimes[1], deltaShift);
    curveBumpedMinus = curveNode.withSingleShift(nodeTimes[1], -deltaShift);
    final YieldCurveBundle curvesDomestic = new YieldCurveBundle();
    curvesDomestic.setCurve(CURVES_NAME[0], CURVES.getCurve(CURVES_NAME[0]));
    curvesDomestic.setCurve(bumpedCurveName, curveBumpedPlus);
    dfDomesticBumped = curveBumpedPlus.getDiscountFactor(forexForward.getPaymentTime());
    forwardBumped = SPOT * dfForeign / dfDomesticBumped;
    dMBumped = Math.log(forwardBumped / strike) / sigmaRootT - 0.5 * sigmaRootT;
    final double bumpedPvDomesticPlus = Math.abs(forexOptionCall.getUnderlyingForex().getPaymentCurrency2().getAmount()) * dfDomesticBumped * NORMAL.getCDF(omega * dMBumped) * (isLong ? 1.0 : -1.0);
    curvesForeign.replaceCurve(bumpedCurveName, curveBumpedMinus);
    dfDomesticBumped = curveBumpedMinus.getDiscountFactor(forexForward.getPaymentTime());
    forwardBumped = SPOT * dfForeign / dfDomesticBumped;
    dMBumped = Math.log(forwardBumped / strike) / sigmaRootT - 0.5 * sigmaRootT;
    final double bumpedPvDomesticMinus = Math.abs(forexOptionCall.getUnderlyingForex().getPaymentCurrency2().getAmount()) * dfDomesticBumped * NORMAL.getCDF(omega * dMBumped) * (isLong ? 1.0 : -1.0);
    final double resultDomestic = (bumpedPvDomesticPlus - bumpedPvDomesticMinus) / (2 * deltaShift);
    assertEquals("Forex Digital option: curve sensitivity", forexForward.getPaymentTime(), sensi.getSensitivity(USD).getSensitivities().get(CURVES_NAME[1]).get(0).first, TOLERANCE_TIME);
    assertEquals("Forex Digital option: curve sensitivity", resultDomestic, sensi.getSensitivity(USD).getSensitivities().get(CURVES_NAME[1]).get(0).second, TOLERANCE_DELTA);
  }

  @Test
  /**
   * Test the present value curve sensitivity through the method and through the calculator.
   */
  public void presentValueCurveSensitivityMethodVsCalculator() {
    final MultipleCurrencyInterestRateCurveSensitivity pvcsMethod = METHOD_BLACK_DIGITAL.presentValueCurveSensitivity(FOREX_DIGITAL_CALL_DOM, SMILE_BUNDLE);
    final MultipleCurrencyInterestRateCurveSensitivity pvcsCalculator = PVCSC_BLACK.visit(FOREX_DIGITAL_CALL_DOM, SMILE_BUNDLE);
    assertEquals("Forex present value curve sensitivity: Method vs Calculator", pvcsMethod, pvcsCalculator);
  }

  @Test
  /**
   * Tests present value volatility sensitivity.
   */
  public void volatilitySensitivity() {
    final double shift = 1.0E-6;
    final PresentValueForexBlackVolatilitySensitivity sensi = METHOD_BLACK_DIGITAL.presentValueBlackVolatilitySensitivity(FOREX_DIGITAL_CALL_DOM, SMILE_BUNDLE);
    final Pair<Currency, Currency> currencyPair = ObjectsPair.of(EUR, USD);
    final DoublesPair point = new DoublesPair(FOREX_DIGITAL_CALL_DOM.getExpirationTime(), STRIKE);
    assertEquals("Forex Digital option: vega", currencyPair, sensi.getCurrencyPair());
    assertEquals("Forex Digital option: vega size", 1, sensi.getVega().getMap().entrySet().size());
    assertTrue("Forex Digital option: vega", sensi.getVega().getMap().containsKey(point));
    final double strike = FOREX_DIGITAL_CALL_DOM.getStrike();
    final int omega = FOREX_DIGITAL_CALL_DOM.isCall() ? 1 : -1;
    final double dfDomestic = CURVES.getCurve(CURVES_NAME[1]).getDiscountFactor(FOREX_DIGITAL_CALL_DOM.getUnderlyingForex().getPaymentTime());
    final double dfForeign = CURVES.getCurve(CURVES_NAME[0]).getDiscountFactor(FOREX_DIGITAL_CALL_DOM.getUnderlyingForex().getPaymentTime());
    final double forward = SPOT * dfForeign / dfDomestic;
    final double volatility = SMILE_TERM.getVolatility(new Triple<Double, Double, Double>(FOREX_DIGITAL_CALL_DOM.getExpirationTime(), strike, forward));
    final double sigmaRootTPlus = (volatility + shift) * Math.sqrt(FOREX_DIGITAL_CALL_DOM.getExpirationTime());
    final double dMPlus = Math.log(forward / strike) / sigmaRootTPlus - 0.5 * sigmaRootTPlus;
    final double pvPlus = Math.abs(FOREX_DIGITAL_CALL_DOM.getUnderlyingForex().getPaymentCurrency2().getAmount()) * dfDomestic * NORMAL.getCDF(omega * dMPlus)
        * (FOREX_DIGITAL_CALL_DOM.isLong() ? 1.0 : -1.0);
    final double sigmaRootTMinus = (volatility - shift) * Math.sqrt(FOREX_DIGITAL_CALL_DOM.getExpirationTime());
    final double dMMinus = Math.log(forward / strike) / sigmaRootTMinus - 0.5 * sigmaRootTMinus;
    final double pvMinus = Math.abs(FOREX_DIGITAL_CALL_DOM.getUnderlyingForex().getPaymentCurrency2().getAmount()) * dfDomestic * NORMAL.getCDF(omega * dMMinus)
        * (FOREX_DIGITAL_CALL_DOM.isLong() ? 1.0 : -1.0);
    assertEquals("Forex Digital option: vega", (pvPlus - pvMinus) / (2 * shift), sensi.getVega().getMap().get(point), TOLERANCE_PRICE);
    final ForexOptionDigitalDefinition optionShortDefinition = new ForexOptionDigitalDefinition(FOREX_DEFINITION, OPTION_EXP_DATE, IS_CALL, !IS_LONG);
    final ForexOptionDigital optionShort = optionShortDefinition.toDerivative(REFERENCE_DATE, CURVES_NAME);
    final PresentValueForexBlackVolatilitySensitivity sensiShort = METHOD_BLACK_DIGITAL.presentValueBlackVolatilitySensitivity(optionShort, SMILE_BUNDLE);
    assertEquals("Forex Digital option: vega short", -sensi.getVega().getMap().get(point), sensiShort.getVega().getMap().get(point));
    // Put/call parity
    ForexOptionDigitalDefinition optionShortPutDefinition = new ForexOptionDigitalDefinition(FOREX_DEFINITION, OPTION_EXP_DATE, !IS_CALL, IS_LONG);
    ForexOptionDigital optionShortPut = optionShortPutDefinition.toDerivative(REFERENCE_DATE, CURVES_NAME);
    final PresentValueForexBlackVolatilitySensitivity sensiShortPut = METHOD_BLACK_DIGITAL.presentValueBlackVolatilitySensitivity(optionShortPut, SMILE_BUNDLE);
    assertEquals("Forex Digital option: vega short", sensiShortPut.getVega().getMap().get(point) + sensi.getVega().getMap().get(point), 0.0, TOLERANCE_PRICE);
  }

  @Test
  /**
   * Test the present value curve sensitivity through the method and through the calculator.
   */
  public void volatilitySensitivityMethodVsCalculator() {
    final PresentValueForexBlackVolatilitySensitivity pvvsMethod = METHOD_BLACK_DIGITAL.presentValueBlackVolatilitySensitivity(FOREX_DIGITAL_CALL_DOM, SMILE_BUNDLE);
    final PresentValueForexBlackVolatilitySensitivity pvvsCalculator = PVVSC_BLACK.visit(FOREX_DIGITAL_CALL_DOM, SMILE_BUNDLE);
    assertEquals("Forex present value curve sensitivity: Method vs Calculator", pvvsMethod, pvvsCalculator);
  }

  @Test
  /**
   * Tests present value volatility quote sensitivity.
   */
  public void volatilityQuoteSensitivity() {
    final PresentValueForexBlackVolatilityNodeSensitivityDataBundle sensiStrike = METHOD_BLACK_DIGITAL.presentValueBlackVolatilityNodeSensitivity(FOREX_DIGITAL_CALL_DOM, SMILE_BUNDLE);
    double[][] sensiQuote = METHOD_BLACK_DIGITAL.presentValueBlackVolatilityNodeSensitivity(FOREX_DIGITAL_CALL_DOM, SMILE_BUNDLE).quoteSensitivity().getVega();
    double[][] sensiStrikeData = sensiStrike.getVega().getData();
    double[] atm = new double[sensiQuote.length];
    int nbDelta = SMILE_TERM.getDelta().length;
    for (int loopexp = 0; loopexp < sensiQuote.length; loopexp++) {
      for (int loopdelta = 0; loopdelta < nbDelta; loopdelta++) {
        assertEquals("Forex Digital option: vega quote - RR", sensiQuote[loopexp][1 + loopdelta], -0.5 * sensiStrikeData[loopexp][loopdelta] + 0.5 * sensiStrikeData[loopexp][2 * nbDelta - loopdelta],
            1.0E-10);
        assertEquals("Forex Digital option: vega quote - Strangle", sensiQuote[loopexp][nbDelta + 1 + loopdelta], sensiStrikeData[loopexp][loopdelta]
            + sensiStrikeData[loopexp][2 * nbDelta - loopdelta], 1.0E-10);
        atm[loopexp] += sensiStrikeData[loopexp][loopdelta] + sensiStrikeData[loopexp][2 * nbDelta - loopdelta];
      }
      atm[loopexp] += sensiStrikeData[loopexp][nbDelta];
      assertEquals("Forex Digital option: vega quote", sensiQuote[loopexp][0], atm[loopexp], 1.0E-10); // ATM
    }
  }

  @Test
  /**
   * Tests present value volatility quote sensitivity: method vs calculator.
   */
  public void volatilityQuoteSensitivityMethodVsCalculator() {
    double[][] sensiMethod = METHOD_BLACK_DIGITAL.presentValueBlackVolatilityNodeSensitivity(FOREX_DIGITAL_CALL_DOM, SMILE_BUNDLE).quoteSensitivity().getVega();
    double[][] sensiCalculator = PresentValueBlackVolatilityQuoteSensitivityForexCalculator.getInstance().visit(FOREX_DIGITAL_CALL_DOM, SMILE_BUNDLE).getVega();
    for (int loopexp = 0; loopexp < SMILE_TERM.getNumberExpiration(); loopexp++) {
      ArrayAsserts.assertArrayEquals("Forex option - quote sensitivity", sensiMethod[loopexp], sensiCalculator[loopexp], 1.0E-10);
    }
  }

}
