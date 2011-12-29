/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.core.holiday.impl;

import java.net.URI;
import java.util.List;

import javax.time.calendar.LocalDate;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.opengamma.core.holiday.Holiday;
import com.opengamma.core.holiday.HolidaySource;
import com.opengamma.core.holiday.HolidayType;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.id.ObjectId;
import com.opengamma.id.UniqueId;
import com.opengamma.id.VersionCorrection;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.fudgemsg.FudgeBooleanWrapper;
import com.opengamma.util.money.Currency;
import com.opengamma.util.rest.AbstractDataResource;

/**
 * RESTful resource for holidays.
 * <p>
 * The holidays resource receives and processes RESTful calls to the holiday source.
 */
@Path("/holSource")
public class DataHolidaySourceResource extends AbstractDataResource {

  /**
   * The holiday source.
   */
  private final HolidaySource _holSource;

  /**
   * Creates the resource, exposing the underlying source over REST.
   * 
   * @param holidaySource  the underlying holiday source, not null
   */
  public DataHolidaySourceResource(final HolidaySource holidaySource) {
    ArgumentChecker.notNull(holidaySource, "holidaySource");
    _holSource = holidaySource;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the holiday source.
   * 
   * @return the holiday source, not null
   */
  public HolidaySource getHolidaySource() {
    return _holSource;
  }

  //-------------------------------------------------------------------------
  @GET
  @Path("holidays/{holidayId}")
  public Response get(
      @PathParam("holidayId") String idStr,
      @QueryParam("version") String version,
      @QueryParam("versionAsOf") String versionAsOf,
      @QueryParam("correctedTo") String correctedTo) {
    
    final ObjectId objectId = ObjectId.parse(idStr);
    if (version != null) {
      final Holiday result = getHolidaySource().getHoliday(objectId.atVersion(version));
      return Response.ok(result).build();
    } else {
      final VersionCorrection vc = VersionCorrection.parse(versionAsOf, correctedTo);
      Holiday result = getHolidaySource().getHoliday(objectId, vc);
      return Response.ok(result).build();
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Builds a URI.
   * 
   * @param baseUri  the base URI, not null
   * @param uniqueId  the unique identifier, may be null
   * @return the URI, not null
   */
  public static URI uriGet(URI baseUri, UniqueId uniqueId) {
    UriBuilder bld = UriBuilder.fromUri(baseUri).path("/holidays/{holidayId}");
    if (uniqueId.getVersion() != null) {
      bld.queryParam("version", uniqueId.getVersion());
    }
    return bld.build(uniqueId.getObjectId());
  }

  /**
   * Builds a URI.
   * 
   * @param baseUri  the base URI, not null
   * @param objectId  the object identifier, may be null
   * @param vc  the version-correction, null means latest
   * @return the URI, not null
   */
  public static URI uriGet(URI baseUri, ObjectId objectId, VersionCorrection vc) {
    UriBuilder bld = UriBuilder.fromUri(baseUri).path("/holidays/{holidayId}");
    if (vc != null) {
      bld.queryParam("versionAsOf", vc.getVersionAsOfString());
      bld.queryParam("correctedTo", vc.getCorrectedToString());
    }
    return bld.build(objectId);
  }

  // deprecated
  //-------------------------------------------------------------------------
  @GET
  @Path("holidays/searchCheck")
  public Response check(
      @QueryParam("date") String localDateStr,
      @QueryParam("holidayType") HolidayType holidayType,
      @QueryParam("currency") String currencyCode,
      @QueryParam("id") List<String> externalIdStrs) {
    
    LocalDate date = LocalDate.parse(localDateStr);
    if (holidayType == HolidayType.CURRENCY) {
      boolean result = getHolidaySource().isHoliday(date, Currency.of(currencyCode));
      return Response.ok(FudgeBooleanWrapper.of(result)).build();
    } else {
      final ExternalIdBundle bundle = ExternalIdBundle.parse(externalIdStrs);
      boolean result = getHolidaySource().isHoliday(date, holidayType, bundle);
      return Response.ok(FudgeBooleanWrapper.of(result)).build();
    }
  }

  /**
   * Builds a URI.
   * 
   * @param baseUri  the base URI, not null
   * @param date  the date, not null
   * @param holidayType  the holiday type, not null
   * @param currency  the currency, may be null
   * @param regionOrExchangeIds  the ids, may be null
   * @return the URI, not null
   */
  public static URI uriSearchCheck(URI baseUri, LocalDate date, HolidayType holidayType, Currency currency, ExternalIdBundle regionOrExchangeIds) {
    UriBuilder bld = UriBuilder.fromUri(baseUri).path("/holidays/searchCheck");
    bld.queryParam("date", date.toString());
    bld.queryParam("holidayType", holidayType.name());
    if (currency != null) {
      bld.queryParam("currency", currency.getCode());
    }
    if (regionOrExchangeIds != null) {
      bld.queryParam("id", regionOrExchangeIds.toStringList().toArray());
    }
    return bld.build();
  }

}