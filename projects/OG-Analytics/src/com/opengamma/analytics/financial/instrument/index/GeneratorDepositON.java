/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.analytics.financial.instrument.index;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.Validate;

import com.opengamma.financial.convention.calendar.Calendar;
import com.opengamma.financial.convention.daycount.DayCount;
import com.opengamma.util.money.Currency;

/**
 * Class with the description of overnight deposit characteristics (conventions, calendar, ...).
 */
public class GeneratorDepositON extends Generator {

  /**
   * The index currency. Not null.
   */
  private final Currency _currency;
  /**
   * The calendar associated to the index. Not null.
   */
  private final Calendar _calendar;
  /**
   * The day count convention associated to the generator. Not null.
   */
  private final DayCount _dayCount;

  /**
   * Deposit generator from all the financial details.
   * @param name The generator name. Not null.
   * @param currency The index currency. Not null.
   * @param calendar The calendar associated to the index. Not null.
   * @param dayCount The day count convention associated to the index.
   */
  public GeneratorDepositON(final String name, final Currency currency, final Calendar calendar, final DayCount dayCount) {
    super(name);
    Validate.notNull(currency, "Currency");
    Validate.notNull(calendar, "Calendar");
    Validate.notNull(dayCount, "Day count");
    _currency = currency;
    _calendar = calendar;
    _dayCount = dayCount;
  }

  /**
   * Gets the index currency.
   * @return The currency.
   */
  public Currency getCurrency() {
    return _currency;
  }

  /**
   * Gets the calendar associated to the index.
   * @return The calendar.
   */
  public Calendar getCalendar() {
    return _calendar;
  }

  /**
   * Gets the day count convention associated to the index.
   * @return The day count convention.
   */
  public DayCount getDayCount() {
    return _dayCount;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + _calendar.hashCode();
    result = prime * result + _currency.hashCode();
    result = prime * result + _dayCount.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    GeneratorDepositON other = (GeneratorDepositON) obj;
    if (!ObjectUtils.equals(_calendar, other._calendar)) {
      return false;
    }
    if (!ObjectUtils.equals(_currency, other._currency)) {
      return false;
    }
    if (!ObjectUtils.equals(_dayCount, other._dayCount)) {
      return false;
    }
    return true;
  }

}
