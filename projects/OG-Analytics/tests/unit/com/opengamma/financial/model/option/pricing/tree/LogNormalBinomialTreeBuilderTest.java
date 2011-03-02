/**
 * Copyright (C) 2009 - 2011 by OpenGamma Inc.
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.model.option.pricing.tree;

import static org.junit.Assert.assertEquals;

import javax.time.calendar.ZonedDateTime;

import org.apache.commons.lang.Validate;
import org.junit.Test;

import com.opengamma.financial.model.interestrate.curve.YieldAndDiscountCurve;
import com.opengamma.financial.model.interestrate.curve.YieldCurve;
import com.opengamma.financial.model.option.definition.EuropeanVanillaOptionDefinition;
import com.opengamma.financial.model.option.definition.GeneralLogNormalOptionDataBundle;
import com.opengamma.financial.model.option.definition.OptionDefinition;
import com.opengamma.financial.model.option.pricing.analytic.formula.BlackImpliedVolFormula;
import com.opengamma.financial.model.option.pricing.analytic.formula.CEVFormula;
import com.opengamma.financial.model.option.pricing.analytic.formula.NormalFormula;
import com.opengamma.financial.model.tree.RecombiningBinomialTree;
import com.opengamma.financial.model.volatility.surface.DriftSurface;
import com.opengamma.financial.model.volatility.surface.VolatilitySurface;
import com.opengamma.math.curve.ConstantDoublesCurve;
import com.opengamma.math.function.Function;
import com.opengamma.math.surface.FunctionalDoublesSurface;
import com.opengamma.util.time.DateUtil;
import com.opengamma.util.time.Expiry;

/**
 * 
 */
public class LogNormalBinomialTreeBuilderTest {

  private static final double SPOT = 100;
  private static final double FORWARD;
  private static final double T = 5.0;
  private static final double BETA = 0.4;
  private static final YieldAndDiscountCurve YIELD_CURVE = new YieldCurve(ConstantDoublesCurve.from(0.05));
  private static final double ATM_VOL = 0.20;
  private static final double SIGMA_BETA;
  private static final ZonedDateTime DATE = DateUtil.getUTCDate(2010, 7, 1);
  private static final OptionDefinition OPTION;
  private static final BinomialTreeBuilder<GeneralLogNormalOptionDataBundle> BUILDER = new LogNormalBinomialTreeBuilder<GeneralLogNormalOptionDataBundle>();
  private static final DriftSurface DRIFTLESS;
  
  static {
    SIGMA_BETA = ATM_VOL * Math.pow(SPOT, 1 - BETA);
    FORWARD = SPOT / YIELD_CURVE.getDiscountFactor(T);
    OPTION = new EuropeanVanillaOptionDefinition(FORWARD, new Expiry(DateUtil.getDateOffsetWithYearFraction(DATE, T)), true);
 
    final Function<Double, Double> driftless = new Function<Double, Double>() {
      @Override
      public Double evaluate(final Double... tk) {
        Validate.isTrue(tk.length == 2);
        return 0.0;
      }
    };
    
    DRIFTLESS = new DriftSurface(FunctionalDoublesSurface.from(driftless)); 
  }

 

  private static final Function<Double, Double> FLAT_LOCAL_VOL = new Function<Double, Double>() {
    @Override
    public Double evaluate(final Double... tk) {
      Validate.isTrue(tk.length == 2);
      double f = tk[1];
      return ATM_VOL;
    }
  };

  private static final Function<Double, Double> TIME_DEPENDENT_LOCAL_VOL = new Function<Double, Double>() {
    @Override
    public Double evaluate(final Double... tk) {
      Validate.isTrue(tk.length == 2);
      double t = tk[0];
      double f = tk[1];
      return (2 * ATM_VOL - t * ATM_VOL / T);
    }
  };

  private static final Function<Double, Double> CEV_LOCAL_VOL = new Function<Double, Double>() {
    @SuppressWarnings("synthetic-access")
    @Override
    public Double evaluate(final Double... tk) {
      Validate.isTrue(tk.length == 2);

      double f = tk[1];
      double sigma = SIGMA_BETA * Math.pow(f, BETA - 1);
      // return Math.min(sigma,100*ATM_VOL);
      return sigma;
    }
  };

  private static final GeneralLogNormalOptionDataBundle DATA = new GeneralLogNormalOptionDataBundle(YIELD_CURVE, DRIFTLESS, new VolatilitySurface(
      FunctionalDoublesSurface.from(FLAT_LOCAL_VOL)), FORWARD, DATE);

  @Test
  public void testPriceFlat() {
    RecombiningBinomialTree<BinomialTreeNode<Double>> assetPriceTree = BUILDER.buildAssetTree(T, DATA, 200);
    RecombiningBinomialTree<BinomialTreeNode<Double>> optionPriceTree = BUILDER.buildOptionPriceTree(OPTION, DATA, assetPriceTree);

    double impVol = BlackImpliedVolFormula.impliedVol(optionPriceTree.getNode(0, 0).getValue(), FORWARD, FORWARD, YIELD_CURVE.getDiscountFactor(T), T, true);
    assertEquals(ATM_VOL, impVol, 1e-3);
    for (int i = 0; i < 10; i++) {
      double m = -1.5 + 3.0 * i / 10.0;
      double strike = FORWARD * Math.exp(ATM_VOL * Math.sqrt(T) * m);
      OptionDefinition option = new EuropeanVanillaOptionDefinition(strike, OPTION.getExpiry(), OPTION.isCall());
      optionPriceTree = BUILDER.buildOptionPriceTree(option, DATA, assetPriceTree);
      impVol = BlackImpliedVolFormula.impliedVol(optionPriceTree.getNode(0, 0).getValue(), FORWARD, strike, YIELD_CURVE.getDiscountFactor(T), T, true);
      // System.out.println(strike+"\t"+impVol);
      assertEquals(ATM_VOL, impVol, 1e-3);
    }
  }

  @Test
  public void testPriceTimeDependent() {
    final GeneralLogNormalOptionDataBundle data = new GeneralLogNormalOptionDataBundle(YIELD_CURVE, DRIFTLESS, new VolatilitySurface(FunctionalDoublesSurface
        .from(TIME_DEPENDENT_LOCAL_VOL)), FORWARD, DATE);
    RecombiningBinomialTree<BinomialTreeNode<Double>> assetPriceTree = BUILDER.buildAssetTree(T, data, 200);
    RecombiningBinomialTree<BinomialTreeNode<Double>> optionPriceTree = BUILDER.buildOptionPriceTree(OPTION, data, assetPriceTree);
    double df = YIELD_CURVE.getDiscountFactor(T);
    double vol = Math.sqrt(7.0 / 3.0) * ATM_VOL;
    double impVol = BlackImpliedVolFormula.impliedVol(optionPriceTree.getNode(0, 0).getValue(), FORWARD, FORWARD, df, T, true);
    assertEquals(vol, impVol, 1e-3);
    for (int i = 0; i < 10; i++) {
      double m = -1.5 + 3.0 * i / 10.0;
      double strike = FORWARD * Math.exp(ATM_VOL * Math.sqrt(T) * m);
      OptionDefinition option = new EuropeanVanillaOptionDefinition(strike, OPTION.getExpiry(), OPTION.isCall());
      optionPriceTree = BUILDER.buildOptionPriceTree(option, data, assetPriceTree);
      impVol = BlackImpliedVolFormula.impliedVol(optionPriceTree.getNode(0, 0).getValue(), FORWARD, strike, df, T, true);
      // System.out.println(strike+"\t"+impVol);
      assertEquals(vol, impVol, 1e-3);
    }
  }

  @Test
  public void testCEV() {
    final GeneralLogNormalOptionDataBundle data = new GeneralLogNormalOptionDataBundle(YIELD_CURVE,DRIFTLESS, new VolatilitySurface(FunctionalDoublesSurface
        .from(CEV_LOCAL_VOL)), FORWARD, DATE);
    RecombiningBinomialTree<BinomialTreeNode<Double>> assetPriceTree = BUILDER.buildAssetTree(T, data, 200);
    RecombiningBinomialTree<BinomialTreeNode<Double>> optionPriceTree = BUILDER.buildOptionPriceTree(OPTION, data, assetPriceTree);
    double df = YIELD_CURVE.getDiscountFactor(T);

    for (int i = 0; i < 10; i++) {
      double m = -1.5 + 3.0 * i / 10.0;
      double strike = FORWARD * Math.exp(ATM_VOL * Math.sqrt(T) * m);
      OptionDefinition option = new EuropeanVanillaOptionDefinition(strike, OPTION.getExpiry(), OPTION.isCall());
      optionPriceTree = BUILDER.buildOptionPriceTree(option, data, assetPriceTree);
      double cevPrice = CEVFormula.optionPrice(FORWARD, strike, BETA, df, SIGMA_BETA, T, true);
      double cevVol = BlackImpliedVolFormula.impliedVol(cevPrice, FORWARD, strike, df, T, true);
      double impVol = BlackImpliedVolFormula.impliedVol(optionPriceTree.getNode(0, 0).getValue(), FORWARD, strike, df, T, true);
     // System.out.println(strike + "\t" + cevVol  + "\t" + impVol);
      assertEquals(cevVol, impVol, 1e-3);
    }
  }

}
