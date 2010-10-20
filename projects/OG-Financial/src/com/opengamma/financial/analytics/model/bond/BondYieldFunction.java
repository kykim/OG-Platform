/**
 * Copyright (C) 2009 - 2010 by OpenGamma Inc.
 *
 * Please see distribution for license.
 */
package com.opengamma.financial.analytics.model.bond;

import java.util.Set;

import com.google.common.collect.Sets;
import com.opengamma.engine.ComputationTarget;
import com.opengamma.engine.ComputationTargetType;
import com.opengamma.engine.function.FunctionCompilationContext;
import com.opengamma.engine.position.Position;
import com.opengamma.engine.value.ComputedValue;
import com.opengamma.engine.value.ValueRequirement;
import com.opengamma.engine.value.ValueRequirementNames;
import com.opengamma.engine.value.ValueSpecification;
import com.opengamma.financial.interestrate.bond.BondPriceCalculator;
import com.opengamma.financial.interestrate.bond.BondYieldCalculator;
import com.opengamma.financial.interestrate.bond.definition.Bond;
import com.opengamma.livedata.normalization.MarketDataRequirementNames;

/**
 * 
 */
public class BondYieldFunction extends BondFunction {
  
  public BondYieldFunction() {
    _requirementName = MarketDataRequirementNames.MARKET_VALUE;
  }

  @Override
  protected Set<ComputedValue> getComputedValues(Position position, Bond bond, double cleanPrice) {
    double dirtyPrice = BondPriceCalculator.dirtyPrice(bond, cleanPrice / 100.0);
    double yield = new BondYieldCalculator().calculate(bond, dirtyPrice);
    final ValueSpecification specification = new ValueSpecification(new ValueRequirement(ValueRequirementNames.YTM, position), getUniqueIdentifier());
  
    yield = 2.0 * (Math.exp(yield / 2.0) - 1.0); //TODO YTM is quoted with compounding frequency depending on the bond's coupon frequency 
    return Sets.newHashSet(new ComputedValue(specification, yield * 100.));
  }


  @Override
  public Set<ValueSpecification> getResults(FunctionCompilationContext context, ComputationTarget target) {
    if (canApplyTo(context, target)) {
      return Sets.newHashSet(new ValueSpecification(new ValueRequirement(ValueRequirementNames.YTM, target.getPosition()), getUniqueIdentifier()));
    }
    return null;
  }

  @Override
  public String getShortName() {
    return "BondYieldFunction";
  }

}
