/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

package com.opengamma.language.view;

import java.util.Arrays;
import java.util.List;

import javax.time.Instant;

import com.opengamma.id.UniqueId;
import com.opengamma.language.context.SessionContext;
import com.opengamma.language.definition.DefinitionAnnotater;
import com.opengamma.language.definition.JavaTypeInfo;
import com.opengamma.language.definition.MetaParameter;
import com.opengamma.language.function.AbstractFunctionInvoker;
import com.opengamma.language.function.MetaFunction;
import com.opengamma.language.function.PublishedFunction;

/**
 * Constructs a {@link ViewClientDescriptor} string from the component parameters
 */
public abstract class ViewClientDescriptorFunction extends AbstractFunctionInvoker implements PublishedFunction {

  private static final MetaParameter VIEW_PARAMETER = new MetaParameter("view", JavaTypeInfo.builder(UniqueId.class).get());
  private static final MetaParameter VALUATION_TIME_PARAMETER = new MetaParameter("valuationTime", JavaTypeInfo.builder(Instant.class).get());
  private static final MetaParameter FIRST_VALUATION_TIME_PARAMETER = new MetaParameter("firstValuationTime", JavaTypeInfo.builder(Instant.class).get());
  private static final MetaParameter LAST_VALUATION_TIME_PARAMETER = new MetaParameter("lastValuationTime", JavaTypeInfo.builder(Instant.class).get());
  private static final MetaParameter SAMPLE_PERIOD_PARAMETER = new MetaParameter("samplePeriod", JavaTypeInfo.builder(Integer.class).defaultValue(ViewClientDescriptor.DEFAULT_SAMPLE_PERIOD).get());
  private static final MetaParameter SNAPSHOT_PARAMETER = new MetaParameter("snapshot", JavaTypeInfo.builder(UniqueId.class).get());

  private final MetaFunction _meta;

  private ViewClientDescriptorFunction(final DefinitionAnnotater info, final String name, final List<MetaParameter> parameters) {
    super(info.annotate(parameters));
    _meta = info.annotate(new MetaFunction(name, getParameters(), this));
  }

  protected ViewClientDescriptorFunction(final String name, final List<MetaParameter> parameters) {
    this(new DefinitionAnnotater(ViewClientDescriptorFunction.class), name, parameters);
  }

  protected abstract ViewClientDescriptor invokeImpl(final Object[] parameters);

  private static final class TickingMarketData extends ViewClientDescriptorFunction {

    private TickingMarketData() {
      super("TickingMarketDataViewClient", Arrays.asList(VIEW_PARAMETER));
    }

    @Override
    protected ViewClientDescriptor invokeImpl(final Object[] parameters) {
      return ViewClientDescriptor.tickingMarketData((UniqueId) parameters[0]);
    }

  }

  /**
   * tickingMarketData instance
   */
  public static final ViewClientDescriptorFunction TICKING_MARKET_DATA = new TickingMarketData();

  private static final class StaticMarketData extends ViewClientDescriptorFunction {
    
    private StaticMarketData() {
      super("StaticMarketDataViewClient", Arrays.asList(VIEW_PARAMETER, VALUATION_TIME_PARAMETER));
    }

    @Override
    protected ViewClientDescriptor invokeImpl(final Object[] parameters) {
      return ViewClientDescriptor.staticMarketData((UniqueId) parameters[0], (Instant) parameters[1]);
    }

  }

  /**
   * staticMarketData instance
   */
  public static final ViewClientDescriptorFunction STATIC_MARKET_DATA = new StaticMarketData();

  private static final class SampleMarketData extends ViewClientDescriptorFunction {

    private SampleMarketData() {
      super("SampleMarketDataViewClient", Arrays.asList(VIEW_PARAMETER, FIRST_VALUATION_TIME_PARAMETER, LAST_VALUATION_TIME_PARAMETER, SAMPLE_PERIOD_PARAMETER));
    }

    @Override
    protected ViewClientDescriptor invokeImpl(final Object[] parameters) {
      return ViewClientDescriptor.sampleMarketData((UniqueId) parameters[0], (Instant) parameters[1], (Instant) parameters[2], (Integer) parameters[3]);
    }

  }

  /**
   * sampleMarketData instance
   */
  public static final ViewClientDescriptorFunction SAMPLE_MARKET_DATA = new SampleMarketData();

  private static final class TickingSnapshot extends ViewClientDescriptorFunction {

    private TickingSnapshot() {
      super("TickingSnapshotViewClient", Arrays.asList(VIEW_PARAMETER, SNAPSHOT_PARAMETER));
    }

    @Override
    protected ViewClientDescriptor invokeImpl(final Object[] parameters) {
      return ViewClientDescriptor.tickingSnapshot((UniqueId) parameters[0], (UniqueId) parameters[1]);
    }

  }

  /**
   * tickingSnapshot instance
   */
  public static final ViewClientDescriptorFunction TICKING_SNAPSHOT = new TickingSnapshot();

  private static final class StaticSnapshot extends ViewClientDescriptorFunction {

    private StaticSnapshot() {
      super("StaticSnapshotViewClient", Arrays.asList(VIEW_PARAMETER, SNAPSHOT_PARAMETER));
    }

    @Override
    protected ViewClientDescriptor invokeImpl(final Object[] parameters) {
      return ViewClientDescriptor.staticSnapshot((UniqueId) parameters[0], (UniqueId) parameters[1]);
    }

  }

  /**
   * staticSnapshot instance
   */
  public static final ViewClientDescriptorFunction STATIC_SNAPSHOT = new StaticSnapshot();

  // AbstractFunctionInvoker

  @Override
  protected final Object invokeImpl(final SessionContext sessionContext, final Object[] parameters) {
    return invokeImpl(parameters).encode();
  }

  // PublishedFunction

  @Override
  public final MetaFunction getMetaFunction() {
    return _meta;
  }

}