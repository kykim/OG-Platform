/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.engine.marketdata;

import java.util.Set;

import com.opengamma.engine.value.ValueRequirement;
import com.opengamma.livedata.UserPrincipal;

/**
 * Permission provider that always returns a positive result.
 */
public class PermissiveMarketDataPermissionProvider implements MarketDataPermissionProvider {

  @Override
  public boolean canAccessMarketData(UserPrincipal user, Set<ValueRequirement> requirements) {
    return true;
  }

}
