// Automatically created - do not modify
///CLOVER:OFF
// CSOFF: Generated File
package com.opengamma.financial.security.swap;
public class FloatingInterestRateLeg extends com.opengamma.financial.security.swap.InterestRateLeg implements java.io.Serializable {
  public <T> T accept (SwapLegVisitor<T> visitor) { return visitor.visitFloatingInterestRateLeg (this); }
  private static final long serialVersionUID = -844249679190015272l;
  private final com.opengamma.id.Identifier _floatingReferenceRateIdentifier;
  public static final String FLOATING_REFERENCE_RATE_IDENTIFIER_KEY = "floatingReferenceRateIdentifier";
  private final Double _initialFloatingRate;
  public static final String INITIAL_FLOATING_RATE_KEY = "initialFloatingRate";
  private final double _spread;
  public static final String SPREAD_KEY = "spread";
  private final boolean _isIBOR;
  public static final String IS_IBOR_KEY = "isIBOR";
  public static class Builder {
    private com.opengamma.financial.convention.daycount.DayCount _dayCount;
    private com.opengamma.financial.convention.frequency.Frequency _frequency;
    private com.opengamma.id.Identifier _regionIdentifier;
    private com.opengamma.financial.convention.businessday.BusinessDayConvention _businessDayConvention;
    private com.opengamma.financial.security.swap.Notional _notional;
    private org.fudgemsg.mapping.FudgeDeserializationContext _fudgeContext;
    protected org.fudgemsg.mapping.FudgeDeserializationContext getFudgeContext () {
      return _fudgeContext;
    }
    private org.fudgemsg.FudgeMsg _fudgeRoot;
    protected org.fudgemsg.FudgeMsg getFudgeRoot () {
      return _fudgeRoot;
    }
    private com.opengamma.id.Identifier _floatingReferenceRateIdentifier;
    private Double _initialFloatingRate;
    private double _spread;
    private boolean _isIBOR;
    public Builder (com.opengamma.financial.convention.daycount.DayCount dayCount, com.opengamma.financial.convention.frequency.Frequency frequency, com.opengamma.id.Identifier regionIdentifier, com.opengamma.financial.convention.businessday.BusinessDayConvention businessDayConvention, com.opengamma.financial.security.swap.Notional notional, com.opengamma.id.Identifier floatingReferenceRateIdentifier, double spread, boolean isIBOR) {
      _dayCount = dayCount;
      _frequency = frequency;
      _regionIdentifier = regionIdentifier;
      _businessDayConvention = businessDayConvention;
      _notional = notional;
      floatingReferenceRateIdentifier (floatingReferenceRateIdentifier);
      spread (spread);
      isIBOR (isIBOR);
    }
    protected Builder (final org.fudgemsg.mapping.FudgeDeserializationContext fudgeContext, final org.fudgemsg.FudgeMsg fudgeMsg) {
      _fudgeRoot = fudgeMsg;
      _fudgeContext = fudgeContext;
      org.fudgemsg.FudgeField fudgeField;
      fudgeField = fudgeMsg.getByName (FLOATING_REFERENCE_RATE_IDENTIFIER_KEY);
      if (fudgeField == null) throw new IllegalArgumentException ("Fudge message is not a FloatingInterestRateLeg - field 'floatingReferenceRateIdentifier' is not present");
      try {
        _floatingReferenceRateIdentifier = com.opengamma.id.Identifier.fromFudgeMsg (fudgeContext, fudgeMsg.getFieldValue (org.fudgemsg.FudgeMsg.class, fudgeField));
      }
      catch (IllegalArgumentException e) {
        throw new IllegalArgumentException ("Fudge message is not a FloatingInterestRateLeg - field 'floatingReferenceRateIdentifier' is not Identifier message", e);
      }
      fudgeField = fudgeMsg.getByName (SPREAD_KEY);
      if (fudgeField == null) throw new IllegalArgumentException ("Fudge message is not a FloatingInterestRateLeg - field 'spread' is not present");
      try {
        _spread = fudgeMsg.getFieldValue (Double.class, fudgeField);
      }
      catch (IllegalArgumentException e) {
        throw new IllegalArgumentException ("Fudge message is not a FloatingInterestRateLeg - field 'spread' is not double", e);
      }
      fudgeField = fudgeMsg.getByName (IS_IBOR_KEY);
      if (fudgeField == null) throw new IllegalArgumentException ("Fudge message is not a FloatingInterestRateLeg - field 'isIBOR' is not present");
      try {
        _isIBOR = fudgeMsg.getFieldValue (Boolean.class, fudgeField);
      }
      catch (IllegalArgumentException e) {
        throw new IllegalArgumentException ("Fudge message is not a FloatingInterestRateLeg - field 'isIBOR' is not boolean", e);
      }
      fudgeField = fudgeMsg.getByName (INITIAL_FLOATING_RATE_KEY);
      if (fudgeField != null)  {
        try {
          initialFloatingRate (fudgeMsg.getFieldValue (Double.class, fudgeField));
        }
        catch (IllegalArgumentException e) {
          throw new IllegalArgumentException ("Fudge message is not a FloatingInterestRateLeg - field 'initialFloatingRate' is not double", e);
        }
      }
    }
    public Builder floatingReferenceRateIdentifier (com.opengamma.id.Identifier floatingReferenceRateIdentifier) {
      if (floatingReferenceRateIdentifier == null) throw new NullPointerException ("'floatingReferenceRateIdentifier' cannot be null");
      else {
        _floatingReferenceRateIdentifier = floatingReferenceRateIdentifier;
      }
      return this;
    }
    public Builder initialFloatingRate (Double initialFloatingRate) {
      _initialFloatingRate = initialFloatingRate;
      return this;
    }
    public Builder spread (double spread) {
      _spread = spread;
      return this;
    }
    public Builder isIBOR (boolean isIBOR) {
      _isIBOR = isIBOR;
      return this;
    }
    public FloatingInterestRateLeg build () {
      return (getFudgeRoot () != null) ? new FloatingInterestRateLeg (getFudgeContext (), getFudgeRoot (), this) : new FloatingInterestRateLeg (this);
    }
  }
  protected FloatingInterestRateLeg (final Builder builder) {
    super (builder._dayCount, builder._frequency, builder._regionIdentifier, builder._businessDayConvention, builder._notional);
    if (builder._floatingReferenceRateIdentifier == null) _floatingReferenceRateIdentifier = null;
    else {
      _floatingReferenceRateIdentifier = builder._floatingReferenceRateIdentifier;
    }
    _initialFloatingRate = builder._initialFloatingRate;
    _spread = builder._spread;
    _isIBOR = builder._isIBOR;
  }
  protected FloatingInterestRateLeg (final org.fudgemsg.mapping.FudgeDeserializationContext fudgeContext, final org.fudgemsg.FudgeMsg fudgeMsg, final Builder builder) {
    super (fudgeContext, fudgeMsg);
    if (builder._floatingReferenceRateIdentifier == null) _floatingReferenceRateIdentifier = null;
    else {
      _floatingReferenceRateIdentifier = builder._floatingReferenceRateIdentifier;
    }
    _initialFloatingRate = builder._initialFloatingRate;
    _spread = builder._spread;
    _isIBOR = builder._isIBOR;
  }
  public FloatingInterestRateLeg (com.opengamma.financial.convention.daycount.DayCount dayCount, com.opengamma.financial.convention.frequency.Frequency frequency, com.opengamma.id.Identifier regionIdentifier, com.opengamma.financial.convention.businessday.BusinessDayConvention businessDayConvention, com.opengamma.financial.security.swap.Notional notional, com.opengamma.id.Identifier floatingReferenceRateIdentifier, Double initialFloatingRate, double spread, boolean isIBOR) {
    super (dayCount, frequency, regionIdentifier, businessDayConvention, notional);
    if (floatingReferenceRateIdentifier == null) throw new NullPointerException ("'floatingReferenceRateIdentifier' cannot be null");
    else {
      _floatingReferenceRateIdentifier = floatingReferenceRateIdentifier;
    }
    _initialFloatingRate = initialFloatingRate;
    _spread = spread;
    _isIBOR = isIBOR;
  }
  protected FloatingInterestRateLeg (final FloatingInterestRateLeg source) {
    super (source);
    if (source == null) throw new NullPointerException ("'source' must not be null");
    if (source._floatingReferenceRateIdentifier == null) _floatingReferenceRateIdentifier = null;
    else {
      _floatingReferenceRateIdentifier = source._floatingReferenceRateIdentifier;
    }
    _initialFloatingRate = source._initialFloatingRate;
    _spread = source._spread;
    _isIBOR = source._isIBOR;
  }
  public org.fudgemsg.FudgeMsg toFudgeMsg (final org.fudgemsg.mapping.FudgeSerializationContext fudgeContext) {
    if (fudgeContext == null) throw new NullPointerException ("fudgeContext must not be null");
    final org.fudgemsg.MutableFudgeMsg msg = fudgeContext.newMessage ();
    toFudgeMsg (fudgeContext, msg);
    return msg;
  }
  public void toFudgeMsg (final org.fudgemsg.mapping.FudgeSerializationContext fudgeContext, final org.fudgemsg.MutableFudgeMsg msg) {
    super.toFudgeMsg (fudgeContext, msg);
    if (_floatingReferenceRateIdentifier != null)  {
      final org.fudgemsg.MutableFudgeMsg fudge1 = org.fudgemsg.mapping.FudgeSerializationContext.addClassHeader (fudgeContext.newMessage (), _floatingReferenceRateIdentifier.getClass (), com.opengamma.id.Identifier.class);
      _floatingReferenceRateIdentifier.toFudgeMsg (fudgeContext, fudge1);
      msg.add (FLOATING_REFERENCE_RATE_IDENTIFIER_KEY, null, fudge1);
    }
    if (_initialFloatingRate != null)  {
      msg.add (INITIAL_FLOATING_RATE_KEY, null, _initialFloatingRate);
    }
    msg.add (SPREAD_KEY, null, _spread);
    msg.add (IS_IBOR_KEY, null, _isIBOR);
  }
  public static FloatingInterestRateLeg fromFudgeMsg (final org.fudgemsg.mapping.FudgeDeserializationContext fudgeContext, final org.fudgemsg.FudgeMsg fudgeMsg) {
    final java.util.List<org.fudgemsg.FudgeField> types = fudgeMsg.getAllByOrdinal (0);
    for (org.fudgemsg.FudgeField field : types) {
      final String className = (String)field.getValue ();
      if ("com.opengamma.financial.security.swap.FloatingInterestRateLeg".equals (className)) break;
      try {
        return (com.opengamma.financial.security.swap.FloatingInterestRateLeg)Class.forName (className).getDeclaredMethod ("fromFudgeMsg", org.fudgemsg.mapping.FudgeDeserializationContext.class, org.fudgemsg.FudgeMsg.class).invoke (null, fudgeContext, fudgeMsg);
      }
      catch (Throwable t) {
        // no-action
      }
    }
    return new Builder (fudgeContext, fudgeMsg).build ();
  }
  public com.opengamma.id.Identifier getFloatingReferenceRateIdentifier () {
    return _floatingReferenceRateIdentifier;
  }
  public Double getInitialFloatingRate () {
    return _initialFloatingRate;
  }
  public double getSpread () {
    return _spread;
  }
  public boolean getIsIBOR () {
    return _isIBOR;
  }
  public boolean equals (final Object o) {
    if (o == this) return true;
    if (!(o instanceof FloatingInterestRateLeg)) return false;
    FloatingInterestRateLeg msg = (FloatingInterestRateLeg)o;
    if (_floatingReferenceRateIdentifier != null) {
      if (msg._floatingReferenceRateIdentifier != null) {
        if (!_floatingReferenceRateIdentifier.equals (msg._floatingReferenceRateIdentifier)) return false;
      }
      else return false;
    }
    else if (msg._floatingReferenceRateIdentifier != null) return false;
    if (_initialFloatingRate != null) {
      if (msg._initialFloatingRate != null) {
        if (!_initialFloatingRate.equals (msg._initialFloatingRate)) return false;
      }
      else return false;
    }
    else if (msg._initialFloatingRate != null) return false;
    if (_spread != msg._spread) return false;
    if (_isIBOR != msg._isIBOR) return false;
    return super.equals (msg);
  }
  public int hashCode () {
    int hc = super.hashCode ();
    hc *= 31;
    if (_floatingReferenceRateIdentifier != null) hc += _floatingReferenceRateIdentifier.hashCode ();
    hc *= 31;
    if (_initialFloatingRate != null) hc += _initialFloatingRate.hashCode ();
    hc = (hc * 31) + (int)_spread;
    hc *= 31;
    if (_isIBOR) hc++;
    return hc;
  }
  public String toString () {
    return org.apache.commons.lang.builder.ToStringBuilder.reflectionToString(this, org.apache.commons.lang.builder.ToStringStyle.SHORT_PREFIX_STYLE);
  }
}
///CLOVER:ON
// CSON: Generated File
