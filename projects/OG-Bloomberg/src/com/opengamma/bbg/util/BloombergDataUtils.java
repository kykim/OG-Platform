/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.bbg.util;

import static com.opengamma.bbg.BloombergConstants.FIELD_ID_BBG_UNIQUE;
import static com.opengamma.bbg.BloombergConstants.FIELD_ID_CUSIP;
import static com.opengamma.bbg.BloombergConstants.FIELD_ID_ISIN;
import static com.opengamma.bbg.BloombergConstants.FIELD_ID_SEDOL1;
import static com.opengamma.bbg.BloombergConstants.FIELD_OPT_CHAIN;
import static com.opengamma.bbg.BloombergConstants.FIELD_PARSEKYABLE_DES;
import static com.opengamma.bbg.BloombergConstants.ON_OFF_FIELDS;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.time.calendar.LocalDate;
import javax.time.calendar.MonthOfYear;
import javax.time.calendar.format.CalendricalParseException;
import javax.time.calendar.format.DateTimeFormatter;
import javax.time.calendar.format.DateTimeFormatters;

import net.sf.ehcache.CacheManager;

import org.apache.commons.lang.StringUtils;
import org.fudgemsg.FudgeContext;
import org.fudgemsg.FudgeField;
import org.fudgemsg.FudgeMsg;
import org.fudgemsg.MutableFudgeMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bloomberglp.blpapi.Datetime;
import com.bloomberglp.blpapi.Element;
import com.bloomberglp.blpapi.Schema.Datatype;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.bbg.BloombergConstants;
import com.opengamma.bbg.PerSecurityReferenceDataResult;
import com.opengamma.bbg.ReferenceDataProvider;
import com.opengamma.bbg.ReferenceDataResult;
import com.opengamma.bbg.historical.normalization.BloombergRateHistoricalTimeSeriesNormalizer;
import com.opengamma.bbg.livedata.normalization.BloombergRateRuleProvider;
import com.opengamma.bbg.normalization.BloombergRateClassifier;
import com.opengamma.core.id.ExternalSchemes;
import com.opengamma.core.security.SecuritySource;
import com.opengamma.core.value.MarketDataRequirementNames;
import com.opengamma.core.value.MarketDataRequirementNamesHelper;
import com.opengamma.engine.marketdata.availability.DomainMarketDataAvailabilityProvider;
import com.opengamma.engine.marketdata.availability.MarketDataAvailabilityProvider;
import com.opengamma.financial.security.option.OptionType;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.id.ExternalIdBundleWithDates;
import com.opengamma.id.ExternalIdWithDates;
import com.opengamma.id.ExternalScheme;
import com.opengamma.livedata.normalization.FieldFilter;
import com.opengamma.livedata.normalization.FieldHistoryUpdater;
import com.opengamma.livedata.normalization.FieldNameChange;
import com.opengamma.livedata.normalization.ImpliedVolatilityCalculator;
import com.opengamma.livedata.normalization.MarketValueCalculator;
import com.opengamma.livedata.normalization.NormalizationRule;
import com.opengamma.livedata.normalization.NormalizationRuleSet;
import com.opengamma.livedata.normalization.RequiredFieldFilter;
import com.opengamma.livedata.normalization.SecurityRuleApplier;
import com.opengamma.livedata.normalization.SecurityRuleProvider;
import com.opengamma.livedata.normalization.StandardRules;
import com.opengamma.master.historicaltimeseries.HistoricalTimeSeriesAdjuster;
import com.opengamma.master.historicaltimeseries.impl.HistoricalTimeSeriesFieldAdjustmentMap;
import com.opengamma.master.position.PositionDocument;
import com.opengamma.master.position.PositionMaster;
import com.opengamma.master.position.PositionSearchRequest;
import com.opengamma.master.position.PositionSearchResult;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.time.Expiry;
import com.opengamma.util.tuple.Pair;

/**
 * Utilities for working with data in the Bloomberg schema.
 * <p>
 * This is a thread-safe static utility class.
 */
public final class BloombergDataUtils {

  /** Logger. */
  private static final Logger s_logger = LoggerFactory.getLogger(BloombergDataUtils.class);
  
  private static final Pattern s_bloombergTickerPattern = buildPattern();

  /**
   * The standard fields required for Bloomberg data, as a list.
   */
  public static final List<String> STANDARD_FIELDS_LIST = ImmutableList.of("BID",
      "ASK",
      "LAST_PRICE",
      "PX_SETTLE",
      "VOLUME",
      "OPT_IMPLIED_VOLATILITY_BID_RT",
      "OPT_IMPLIED_VOLATILITY_ASK_RT",
      "OPT_IMPLIED_VOLATILITY_LAST_RT",
      "OPT_IMPLIED_VOLATILITY_MID_RT",
      "YLD_CNV_MID", //TODO BBG-96
      "YLD_YTM_MID", //TODO BBG-96
      "PX_DIRTY_MID"); //TODO BBG-96
  
  /**
   * The standard fields required for Bloomberg data, as a set.
   */
  public static final Set<String> STANDARD_FIELDS_SET = new ImmutableSet.Builder<String>().addAll(STANDARD_FIELDS_LIST).build();

  /** 
   * Map from RIC to BBG prefixes, for exceptions only 
   */
  private static final Map<String, String> s_ricToBbgPrefixMap;
  static {
    s_ricToBbgPrefixMap = new HashMap<String, String>();
    s_ricToBbgPrefixMap.put("EDD", "FD");
    s_ricToBbgPrefixMap.put("EDM", "0D");
    s_ricToBbgPrefixMap.put("EDD", "FD");
    s_ricToBbgPrefixMap.put("FEI", "ER");
    s_ricToBbgPrefixMap.put("FME", "0R");
    s_ricToBbgPrefixMap.put("2FME", "2R");
    s_ricToBbgPrefixMap.put("FSS", "L ");
    s_ricToBbgPrefixMap.put("FMS", "0L");
    s_ricToBbgPrefixMap.put("2FMS", "2L");
    s_ricToBbgPrefixMap.put("FES", "ES");
  }
  
  /**
   * Number format to use in BBC strike price
   */
  private static final DecimalFormat THREE_DECIMAL_PLACES = new DecimalFormat("#0.000");

  /**
   * Map from month to BBG month code
   */
  private static final BiMap<MonthOfYear, String> s_monthCode;
  static {
    s_monthCode = HashBiMap.create();
    s_monthCode.put(MonthOfYear.JANUARY, "F");
    s_monthCode.put(MonthOfYear.FEBRUARY, "G");
    s_monthCode.put(MonthOfYear.MARCH, "H");
    s_monthCode.put(MonthOfYear.APRIL, "J");
    s_monthCode.put(MonthOfYear.MAY, "K");
    s_monthCode.put(MonthOfYear.JUNE, "M");
    s_monthCode.put(MonthOfYear.JULY, "N");
    s_monthCode.put(MonthOfYear.AUGUST, "Q");
    s_monthCode.put(MonthOfYear.SEPTEMBER, "U");
    s_monthCode.put(MonthOfYear.OCTOBER, "V");
    s_monthCode.put(MonthOfYear.NOVEMBER, "X");
    s_monthCode.put(MonthOfYear.DECEMBER, "Z");
  }

  /**
   * Restricted constructor.
   */
  private BloombergDataUtils() {
  }

  private static Pattern buildPattern() {
    if (BloombergConstants.MARKET_SECTORS.isEmpty()) {
      throw new OpenGammaRuntimeException("Bloomberg market sectors can not be empty");
    }
    List<String> marketSectorList = Lists.newArrayList(BloombergConstants.MARKET_SECTORS);
    String sectorPattern = marketSectorList.get(0);
    for (int i = 1; i < marketSectorList.size(); i++) {
      sectorPattern += "|" + marketSectorList.get(i);
    }
    Pattern bloombergTickerPattern = Pattern.compile(String.format("^(.+)(\\s+)((%s))$", sectorPattern), Pattern.CASE_INSENSITIVE);
    return bloombergTickerPattern;
  }

  public static Collection<NormalizationRuleSet> getDefaultNormalizationRules(ReferenceDataProvider referenceDataProvider, CacheManager cacheManager) {
    ArgumentChecker.notNull(cacheManager, "cacheManager");
    
    Collection<NormalizationRuleSet> returnValue = new ArrayList<NormalizationRuleSet>();
    returnValue.add(StandardRules.getNoNormalization());

    List<NormalizationRule> openGammaRules = new ArrayList<NormalizationRule>();

    // Filter out non-price updates
    openGammaRules.add(new FieldFilter(STANDARD_FIELDS_LIST));

    // Standardize field names.
    openGammaRules.add(new FieldNameChange("BID", MarketDataRequirementNames.BID));
    openGammaRules.add(new FieldNameChange("ASK", MarketDataRequirementNames.ASK));
    openGammaRules.add(new FieldNameChange("LAST_PRICE", MarketDataRequirementNames.LAST));
    openGammaRules.add(new FieldNameChange("PX_SETTLE", MarketDataRequirementNames.SETTLE_PRICE));
    openGammaRules.add(new FieldNameChange("VOLUME", MarketDataRequirementNames.VOLUME));
    openGammaRules.add(new FieldNameChange("OPT_IMPLIED_VOLATILITY_BID_RT", MarketDataRequirementNames.BID_IMPLIED_VOLATILITY));
    openGammaRules.add(new FieldNameChange("OPT_IMPLIED_VOLATILITY_ASK_RT", MarketDataRequirementNames.ASK_IMPLIED_VOLATILITY));
    openGammaRules.add(new FieldNameChange("OPT_IMPLIED_VOLATILITY_LAST_RT", MarketDataRequirementNames.LAST_IMPLIED_VOLATILITY));
    openGammaRules.add(new FieldNameChange("OPT_IMPLIED_VOLATILITY_MID_RT", MarketDataRequirementNames.MID_IMPLIED_VOLATILITY));
    openGammaRules.add(new FieldNameChange("YLD_CNV_MID", MarketDataRequirementNames.YIELD_CONVENTION_MID));
    openGammaRules.add(new FieldNameChange("YLD_YTM_MID", MarketDataRequirementNames.YIELD_YIELD_TO_MATURITY_MID));
    openGammaRules.add(new FieldNameChange("PX_DIRTY_MID", MarketDataRequirementNames.DIRTY_PRICE_MID));
    
    // Calculate market value
    openGammaRules.add(new MarketValueCalculator());
    
    // Normalize the market value
    if (referenceDataProvider != null) {
      BloombergRateClassifier rateClassifier = new BloombergRateClassifier(referenceDataProvider, cacheManager);
      SecurityRuleProvider quoteRuleProvider = new BloombergRateRuleProvider(rateClassifier);
      openGammaRules.add(new SecurityRuleApplier(quoteRuleProvider));
    }

    // Calculate implied vol value
    openGammaRules.add(new ImpliedVolatilityCalculator());

    // At this point, BID, ASK, LAST, MARKET_VALUE, Volume and various Bloomberg implied vol fields are stored in the history. 
    openGammaRules.add(new FieldHistoryUpdater());

    // Filter out non-OpenGamma fields (i.e., BID, ASK, various Bloomberg implied vol fields)
    openGammaRules.add(new FieldFilter(
        MarketDataRequirementNames.MARKET_VALUE,
        MarketDataRequirementNames.SETTLE_PRICE,
        MarketDataRequirementNames.VOLUME,
        MarketDataRequirementNames.IMPLIED_VOLATILITY,
        MarketDataRequirementNames.YIELD_CONVENTION_MID,
        MarketDataRequirementNames.YIELD_YIELD_TO_MATURITY_MID,
        MarketDataRequirementNames.DIRTY_PRICE_MID));
    openGammaRules.add(new RequiredFieldFilter(MarketDataRequirementNames.MARKET_VALUE));

    NormalizationRuleSet openGammaRuleSet = new NormalizationRuleSet(
        StandardRules.getOpenGammaRuleSetId(),
        "",
        openGammaRules);
    returnValue.add(openGammaRuleSet);

    return returnValue;
  }
  
  public static HistoricalTimeSeriesFieldAdjustmentMap createFieldAdjustmentMap(ReferenceDataProvider referenceDataProvider, CacheManager cacheManager) {
    HistoricalTimeSeriesFieldAdjustmentMap fieldAdjustmentMap = new HistoricalTimeSeriesFieldAdjustmentMap(BloombergConstants.BLOOMBERG_DATA_SOURCE_NAME);
    BloombergRateClassifier rateClassifier = new BloombergRateClassifier(referenceDataProvider, cacheManager);
    HistoricalTimeSeriesAdjuster rateNormalizer = new BloombergRateHistoricalTimeSeriesNormalizer(rateClassifier);
    fieldAdjustmentMap.addFieldAdjustment(MarketDataRequirementNames.SETTLE_PRICE, null, BloombergConstants.BBG_FIELD_SETTLE_PRICE, rateNormalizer);
    fieldAdjustmentMap.addFieldAdjustment(MarketDataRequirementNames.MARKET_VALUE, null, BloombergConstants.BBG_FIELD_LAST_PRICE, rateNormalizer);
    fieldAdjustmentMap.addFieldAdjustment(MarketDataRequirementNames.VOLUME, null, BloombergConstants.BBG_FIELD_VOLUME, null);
    fieldAdjustmentMap.addFieldAdjustment(MarketDataRequirementNames.YIELD_YIELD_TO_MATURITY_MID, null, BloombergConstants.BBG_FIELD_YIELD_TO_MATURITY_MID, null);
    return fieldAdjustmentMap;
  }
  
  public static MarketDataAvailabilityProvider createAvailabilityProvider(SecuritySource securitySource) {
    Set<ExternalScheme> acceptableSchemes = ImmutableSet.of(
        ExternalSchemes.BLOOMBERG_BUID_WEAK,
        ExternalSchemes.BLOOMBERG_BUID,
        ExternalSchemes.BLOOMBERG_TICKER_WEAK,
        ExternalSchemes.BLOOMBERG_TICKER);
    Collection<String> validMarketDataRequirementNames = MarketDataRequirementNamesHelper.constructValidRequirementNames();
    return new DomainMarketDataAvailabilityProvider(securitySource, acceptableSchemes, validMarketDataRequirementNames);
  }

  public static FudgeMsg parseElement(Element element) {
    MutableFudgeMsg fieldData = FudgeContext.GLOBAL_DEFAULT.newMessage();
    for (int iSubElement = 0; iSubElement < element.numElements(); iSubElement++) {
      Element subElement = element.getElement(iSubElement);
      if (subElement.numValues() == 0) {
        continue;
      }
      String name = subElement.elementDefinition().name().toString();
      Object value = parseValue(subElement);
      if (value instanceof List<?>) {
        for (Object obj : (List<?>) value) {
          fieldData.add(name, obj);
        }
      } else if (value != null) {
        fieldData.add(name, value);
      } else {
        s_logger.warn("Unable to extract value named {} from element {}", name, subElement);
      }
    }
    return fieldData;
  }

  /**
   * @param valueElement the value element
   * @return the parsed value
   */
  public static Object parseValue(Element valueElement) {
    Datatype datatype = valueElement.datatype();
    if (datatype == Datatype.STRING) {
      return valueElement.getValueAsString();
    } else if (datatype == Datatype.BOOL) {
      return valueElement.getValueAsBool();
    } else if (datatype == Datatype.BYTEARRAY) {
      // REVIEW kirk 2009-10-22 -- How do we extract this? Intentionally fall through.
    } else if (datatype == Datatype.CHAR) {
      char c = valueElement.getValueAsChar();
      return new String("" + c);
    } else if (datatype == Datatype.CHOICE) {
      // REVIEW kirk 2009-10-22 -- How do we extract this? Intentionally fall through.
    } else if (datatype == Datatype.DATE) {
      Datetime date = valueElement.getValueAsDate();
      return date.toString();
    } else if (datatype == Datatype.DATETIME) {
      // REVIEW kirk 2009-10-22 -- This is clearly wrong.
      Datetime date = valueElement.getValueAsDatetime();
      return date.toString();
      //return date.calendar().getTime();
    } else if (datatype == Datatype.ENUMERATION) {
      return valueElement.getValueAsString();
    } else if (datatype == Datatype.FLOAT32) {
      return valueElement.getValueAsFloat32();
    } else if (datatype == Datatype.FLOAT64) {
      return valueElement.getValueAsFloat64();
    } else if (datatype == Datatype.INT32) {
      return valueElement.getValueAsInt32();
    } else if (datatype == Datatype.INT64) {
      return valueElement.getValueAsInt64();
    } else if (datatype == Datatype.TIME) {
      // REVIEW kirk 2009-10-22 -- This is clearly wrong.
      Datetime date = valueElement.getValueAsDate();
      return date.toString();
      //return date.calendar().getTime();
    } else if (datatype == Datatype.SEQUENCE) {
      int numValues = valueElement.numValues();
      List<FudgeMsg> valueAsList = new ArrayList<FudgeMsg>(numValues);
      for (int i = 0; i < numValues; i++) {
        Element sequenceElem = valueElement.getValueAsElement(i);
        FudgeMsg sequenceElemAsMsg = parseElement(sequenceElem);
        valueAsList.add(sequenceElemAsMsg);
      }
      return valueAsList;
    }
    s_logger.warn("Unhandled Datatype of {}, data {}", datatype, valueElement);
    return null;
  }

  public static String getSingleBUID(ReferenceDataProvider refDataProvider, String securityDes) {
    Map<String, String> ticker2buid = getBUID(refDataProvider, Collections.singleton(securityDes));
    return ticker2buid.get(securityDes);
  }

  public static Map<String, String> getBUID(ReferenceDataProvider refDataProvider, Set<String> bloombergKeys) {
    Map<String, String> result = Maps.newHashMap();

    ReferenceDataResult refDataResult = refDataProvider.getFields(bloombergKeys, Collections.singleton(FIELD_ID_BBG_UNIQUE));
    if (refDataResult == null) {
      s_logger.warn("Bloomberg Reference Data Provider returned NULL result for {}", bloombergKeys);
      return result;
    }

    for (String securityDes : bloombergKeys) {
      PerSecurityReferenceDataResult secResult = refDataResult.getResult(securityDes);
      if (secResult == null) {
        s_logger.warn("PerSecurityReferenceDataResult for {} cannot be null", securityDes);
        continue;
      }
      //check no exceptions
      List<String> exceptions = secResult.getExceptions();

      if (exceptions != null && exceptions.size() > 0) {
        for (String msg : exceptions) {
          s_logger.warn("Exception looking up {}/{} - {}",
              new Object[] {securityDes, FIELD_ID_BBG_UNIQUE, msg });
        }
        continue;
      }
      // check same security was returned
      String refSec = secResult.getSecurity();
      if (!securityDes.equals(refSec)) {
        s_logger.warn("Returned security {} not the same as searched security {}", refSec, securityDes);
        continue;
      }
      //get field data
      FudgeMsg fieldData = secResult.getFieldData();
      if (fieldData != null) {
        if (fieldData.getString(FIELD_ID_BBG_UNIQUE) != null) {
          result.put(securityDes, fieldData.getString(FIELD_ID_BBG_UNIQUE));
        }
      }
    }
    return result;
  }

  public static Set<String> getIndexMembers(ReferenceDataProvider refDataProvider, String indexTicker) {
    Set<String> result = new TreeSet<String>();
    Set<String> bbgFields = new HashSet<String>();
    bbgFields.add("INDX_MEMBERS");
    bbgFields.add("INDX_MEMBERS2");
    bbgFields.add("INDX_MEMBERS3");
    ReferenceDataResult dataResult = refDataProvider.getFields(Collections.singleton(indexTicker), bbgFields);
    PerSecurityReferenceDataResult perSecResult = dataResult.getResult(indexTicker);
    List<String> exceptions = perSecResult.getExceptions();
    if (!exceptions.isEmpty()) {
      s_logger.warn("Unable to lookup Index {} members because of exceptions {}", indexTicker, exceptions.toString());
      throw new OpenGammaRuntimeException("Unable to lookup Index members because of exceptions " + exceptions.toString());
    }
    addIndexMembers(result, perSecResult, "INDX_MEMBERS");
    addIndexMembers(result, perSecResult, "INDX_MEMBERS2");
    addIndexMembers(result, perSecResult, "INDX_MEMBERS3");
    return result;
  }

  /**
   * @param result
   * @param perSecResult
   * @return
   */
  private static void addIndexMembers(Set<String> result,
      PerSecurityReferenceDataResult perSecResult, String fieldName) {
    FudgeMsg fieldData = perSecResult.getFieldData();
    List<FudgeField> fields = fieldData.getAllByName(fieldName);
    for (FudgeField fudgeField : fields) {
      FudgeMsg msg = (FudgeMsg) fudgeField.getValue();
      String memberTicker = msg.getString("Member Ticker and Exchange Code");
      result.add(memberTicker);
    }
  }

  public static Set<ExternalId> getOptionChain(ReferenceDataProvider refDataProvider, String securityID) {
    ArgumentChecker.notNull(securityID, "security name");
    Set<ExternalId> result = new TreeSet<ExternalId>();
    ReferenceDataResult refDataResult = refDataProvider.getFields(Collections.singleton(securityID), Collections.singleton(FIELD_OPT_CHAIN));
    if (refDataResult == null) {
      s_logger.info("Bloomberg Reference Data Provider returned NULL result");
      return null;
    }
    PerSecurityReferenceDataResult secResult = refDataResult.getResult(securityID);
    if (secResult == null) {
      s_logger.info("PerSecurityReferenceDataResult for " + securityID + " cannot be null");
      return null;
    }

    //check no exceptions
    List<String> exceptions = secResult.getExceptions();
    if (exceptions != null && exceptions.size() > 0) {
      for (String msg : exceptions) {
        s_logger.warn(msg);
      }
      return null;
    }

    // check same security was returned
    String refSec = secResult.getSecurity();
    if (!securityID.equals(refSec)) {
      s_logger.info("Returned security {} not the same as searched security {}", refSec, securityID);
      return null;
    }

    //get field data
    FudgeMsg fieldData = secResult.getFieldData();
    if (fieldData == null) {
      s_logger.info("FudgeFieldContainer for security {} cannot be null", securityID);
      return null;
    }

    for (FudgeField field : fieldData.getAllByName(FIELD_OPT_CHAIN)) {
      FudgeMsg chainContainer = (FudgeMsg) field.getValue();
      String identifier =  StringUtils.trimToNull(chainContainer.getString("Security Description"));
      if (identifier != null) {
        ExternalId ticker = ExternalSchemes.bloombergTickerSecurityId(BloombergDataUtils.removeDuplicateWhiteSpace(identifier, " "));
        result.add(ticker);
      }
    }
    return result;
  }

  /**
   * Checks if the specified field contains valid data.
   * @param name  the field name, not null
   * @return true if the field is valid
   */
  public static boolean isValidField(String name) {
    return ON_OFF_FIELDS.contains(name) || (StringUtils.isNotBlank(name) && !name.equalsIgnoreCase("N.A"));
  }

  public static ExternalIdBundleWithDates parseIdentifiers(FudgeMsg fieldData, String firstTradeDateField, String lastTradeDateField) {
    ArgumentChecker.notNull(fieldData, "fieldData");
    final String bbgUnique = fieldData.getString(FIELD_ID_BBG_UNIQUE);
    final String cusip = fieldData.getString(FIELD_ID_CUSIP);
    final String isin = fieldData.getString(FIELD_ID_ISIN);
    final String sedol1 = fieldData.getString(FIELD_ID_SEDOL1);
    final String securityIdentifier = fieldData.getString(FIELD_PARSEKYABLE_DES);
    final String validFromStr = firstTradeDateField != null ? fieldData.getString(firstTradeDateField) : null;
    final String validToStr = lastTradeDateField != null ? fieldData.getString(lastTradeDateField) : null;

    final Set<ExternalIdWithDates> identifiers = new HashSet<ExternalIdWithDates>();
    if (isValidField(bbgUnique)) {
      ExternalId buid = ExternalSchemes.bloombergBuidSecurityId(bbgUnique);
      identifiers.add(ExternalIdWithDates.of(buid, null, null));
    }
    if (isValidField(cusip)) {
      ExternalId cusipId = ExternalSchemes.cusipSecurityId(cusip);
      identifiers.add(ExternalIdWithDates.of(cusipId, null, null));
    }
    if (isValidField(sedol1)) {
      ExternalId sedol1Id = ExternalSchemes.sedol1SecurityId(sedol1);
      identifiers.add(ExternalIdWithDates.of(sedol1Id, null, null));
    }
    if (isValidField(isin)) {
      ExternalId isinId = ExternalSchemes.isinSecurityId(isin);
      identifiers.add(ExternalIdWithDates.of(isinId, null, null));
    }
    if (isValidField(securityIdentifier)) {
      ExternalId tickerId = ExternalSchemes.bloombergTickerSecurityId(securityIdentifier);
      LocalDate validFrom = null;
      if (isValidField(validFromStr)) {
        try {
          validFrom = LocalDate.parse(validFromStr);
        } catch (CalendricalParseException ex) {
          s_logger.warn("valid from date not in yyyy-mm-dd format - {}", validFromStr);
        }
      }
      LocalDate validTo = null;
      if (isValidField(validToStr)) {
        try {
          validTo = LocalDate.parse(validToStr);
        } catch (CalendricalParseException ex) {
          s_logger.warn("valid to date not in yyyy-mm-dd format - {}", validToStr);
        }
      }
      identifiers.add(ExternalIdWithDates.of(tickerId, validFrom, validTo));
    }
    return new ExternalIdBundleWithDates(identifiers);
  }

  /**
   * @param bundleWithDates the identifier bundle with dates
   * @return {@link ExternalIdBundleWithDates} with single and 2 digit year codes
   */
  public static ExternalIdBundleWithDates addTwoDigitYearCode(ExternalIdBundleWithDates bundleWithDates) {
    ArgumentChecker.notNull(bundleWithDates, "bundleWithDates");
    Set<ExternalIdWithDates> identifiers = new HashSet<ExternalIdWithDates>();
    for (ExternalIdWithDates identifierWithDates : bundleWithDates) {
      ExternalId identifier = identifierWithDates.toExternalId();
      String identifierValue = identifier.getValue();
      if (identifierValue.contains(BloombergConstants.MARKET_SECTOR_COMDTY) && identifierValue.indexOf(' ') == identifierValue.lastIndexOf(' ')) {
        //found a future code
        int splitIndex = identifierValue.lastIndexOf(' ');
        String secCode = identifierValue.substring(0, splitIndex);
        
        //get two year digit code
        int length = secCode.length();
        String yearStr = secCode.substring(length - 2);
        try {
          Integer.parseInt(yearStr);
          identifiers.add(ExternalIdWithDates.of(identifierWithDates.toExternalId(), identifierWithDates.getValidTo().plusDays(1), null));

          //add single digit as well
          StringBuilder buf = new StringBuilder(secCode.substring(0, secCode.indexOf(yearStr)));
          buf.append(yearStr.charAt(yearStr.length() - 1));
          buf.append(identifierValue.substring(splitIndex));
          ExternalId singleYearId = ExternalSchemes.bloombergTickerSecurityId(buf.toString());
          identifiers.add(ExternalIdWithDates.of(singleYearId, identifierWithDates.getValidFrom(), identifierWithDates.getValidTo()));
        } catch (NumberFormatException ex) {
          // try the single digit
          yearStr = secCode.substring(length - 1);
          try {
            Integer.parseInt(yearStr);
            identifiers.add(identifierWithDates);
            //add double digit as well
            LocalDate validTo = identifierWithDates.getValidTo();
            String endYear = String.valueOf(validTo.getYear());
            StringBuilder buf = new StringBuilder(secCode.substring(0, secCode.indexOf(yearStr)));
            buf.append(endYear.substring(endYear.length() - 2));
            buf.append(identifierValue.substring(splitIndex));
            ExternalId doubleDigitYearId = ExternalSchemes.bloombergTickerSecurityId(buf.toString());
            identifiers.add(ExternalIdWithDates.of(doubleDigitYearId, identifierWithDates.getValidTo().plusDays(1), null));
          } catch (NumberFormatException ex2) {
            s_logger.warn("cannot make out year code for {}", identifier);
          }
        }
      } else {
        identifiers.add(identifierWithDates);
      }
    }
    return new ExternalIdBundleWithDates(identifiers);
  }

  /**
   * Given a position master, it pulls the current positions identifier bundles.
   * @param positionMaster  the position master, not-null
   * @return a set of bundles of current positions
   */
  public static Set<ExternalIdBundle> getCurrentIdentifiers(PositionMaster positionMaster) {
    ArgumentChecker.notNull(positionMaster, "positionMaster");
    PositionSearchRequest searchRequest = new PositionSearchRequest();
    PositionSearchResult searchResult = positionMaster.search(searchRequest);
    Set<ExternalIdBundle> securities = new HashSet<ExternalIdBundle>();
    for (PositionDocument doc : searchResult.getDocuments()) {
      securities.add(doc.getPosition().getSecurityLink().getExternalId());  // TODO: doesn't work if linked by object id
    }
    return securities;
  }

  /**
   * Removes duplicate whitespace from the specified field.
   * @param field  the field name, not null
   * @param replacement the replacement string, not null
   * @return the stripped field, not null
   */
  public static String removeDuplicateWhiteSpace(final String field, final String replacement) {
    if (field != null) {
      ArgumentChecker.notNull(field, "field");
      ArgumentChecker.notNull(replacement, "replacement");
      return field.replaceAll("\\s+", replacement);
    } else {
      return null;
    }
  }

  public static Set<ExternalId> identifierLoader(Reader reader) {
    ArgumentChecker.notNull(reader, "reader");
    Set<ExternalId> result = Sets.newHashSet();
    BufferedReader inputReader = new BufferedReader(reader);
    try {
      String line;
      while ((line = inputReader.readLine()) != null) {
        if (StringUtils.isBlank(line) || line.charAt(0) == '#') {
          continue;
        }
        result.add(ExternalId.parse(line));
      }
    } catch (IOException ex) {
      throw new OpenGammaRuntimeException("cannot read from reader", ex);
    } finally {
      try {
        inputReader.close();
      } catch (IOException ex) {
        s_logger.warn("cannot close reader ", ex);
      }
    }
    return result;
  }

  /**
   * Convert bundles to preferred bloomberg keys.
   * Where possible these keys will be BUIDs [BBG-87]
   * 
   * @param identifiers the collection of bundles, not null
   * @param refDataProvider the ReferenceDataProvider to use to resolve bundles not containing BUIDs, not null
   * @return BiMap  of bloomberg key (hopefully a buid key) to bundle, not null
   */
  public static BiMap<String, ExternalIdBundle> convertToBloombergBuidKeys(Collection<ExternalIdBundle> identifiers, ReferenceDataProvider refDataProvider) {
    ArgumentChecker.notNull(identifiers, "identifiers");
    ArgumentChecker.notNull(refDataProvider, "refDataProvider");
    
    Set<String> nonBuids = new HashSet<String>();
    final BiMap<String, ExternalIdBundle> bundle2Bbgkey = HashBiMap.create();
    for (ExternalIdBundle identifierBundle : identifiers) {
      ExternalId preferredIdentifier = BloombergDomainIdentifierResolver.resolvePreferredIdentifier(identifierBundle);
      String bloombergKey = BloombergDomainIdentifierResolver.toBloombergKey(preferredIdentifier);
      if (bloombergKey == null) {
        s_logger.warn("bundle {} resolves to null bloomberg key", identifierBundle);
        continue;
      }
      //REVIEW simon : is it ok that we discard duplicates here 
      bundle2Bbgkey.put(bloombergKey, identifierBundle);
      if (!preferredIdentifier.getScheme().equals(ExternalSchemes.BLOOMBERG_BUID)) {
        nonBuids.add(bloombergKey);
      }
    }
    
    if (!nonBuids.isEmpty()) {
      //BBG-87 Map everything to BUIDs
      Map<String, String> remaps = getBUID(refDataProvider, nonBuids);
      for (Entry<String, String> entry : remaps.entrySet()) {
        String nonBuid = entry.getKey();
        String buid = entry.getValue();
        ExternalId buidExternalId = ExternalId.of(ExternalSchemes.BLOOMBERG_BUID, buid);
        String buidKey = BloombergDomainIdentifierResolver.toBloombergKey(buidExternalId);
        changeKey(nonBuid, buidKey, bundle2Bbgkey);
      }
    }
    return bundle2Bbgkey;
  }
  
  private static <TKey, TValue>  void changeKey(TKey oldKey, TKey newKey, BiMap<TKey, TValue> map) {
    //REVIEW simon : is it ok that we discard duplicates here 
    TValue oldValue = map.remove(oldKey);
    map.put(newKey, oldValue);
  }

  /**
   * Splits a ticker at the market sector, returning a pair of the ticker excluding the market sector and the market
   * sector itself.
   * 
   * @param ticker  the ticker, not null
   * @return a pair of the ticker excluding the market sector, and the market sector, not null
   */
  public static Pair<String, String> splitTickerAtMarketSector(String ticker) {
    ArgumentChecker.notNull(ticker, "ticker");
    int splitIdx = ticker.lastIndexOf(' ');
    if (splitIdx > 0) {
      return Pair.of(ticker.substring(0, splitIdx), ticker.substring(splitIdx + 1));
    } else {
      return null;
    }
  }
  
  public static boolean isValidBloombergTicker(String ticker) {
    ArgumentChecker.notNull(ticker, "ticker");
    Matcher matcher = s_bloombergTickerPattern.matcher(ticker);
    if (matcher.matches()) {
      String marketSector = matcher.group(3);
      s_logger.debug("market sector {} extracted from ticker {}", marketSector, ticker);
      return true;
    } else {
      return false;
    }
  }

  /**
   * Generates an equity option ticker from details about the option.
   * 
   * @param underlyingTicker  the ticker of the underlying equity, not null
   * @param expiry  the option expiry, not null
   * @param optionType  the option type, not null
   * @param strike  the strike rate
   * @return the equity option ticker, not null
   */
  public static ExternalId generateEquityOptionTicker(String underlyingTicker, Expiry expiry, OptionType optionType, double strike) {
    ArgumentChecker.notNull(underlyingTicker, "underlyingTicker");
    ArgumentChecker.notNull(expiry, "expiry");
    ArgumentChecker.notNull(optionType, "optionType");
    Pair<String, String> tickerMarketSectorPair = splitTickerAtMarketSector(underlyingTicker);
    DateTimeFormatter expiryFormatter = DateTimeFormatters.pattern("MM/dd/yy");
    DecimalFormat strikeFormat = new DecimalFormat("0.###");
    String strikeString = strikeFormat.format(strike);
    StringBuilder sb = new StringBuilder();
    sb.append(tickerMarketSectorPair.getFirst())
      .append(' ')
      .append(expiry.getExpiry().toString(expiryFormatter))
      .append(' ')
      .append(optionType == OptionType.PUT ? 'P' : 'C')
      .append(strikeString)
      .append(' ')
      .append(tickerMarketSectorPair.getSecond());
    return ExternalId.of(ExternalSchemes.BLOOMBERG_TICKER, sb.toString());
  }

  public static String dateToBbgCode(LocalDate date) {
    String y = Integer.toString(date.getYear());
    return s_monthCode.get(date.getMonthOfYear()) + y.charAt(y.length() - 1);
  }
  
  public static ExternalId ricToBbgFuture(String ric) {
    return ExternalId.of(ExternalSchemes.BLOOMBERG_TICKER, 
        ricToBbgFuturePrefix(ric) + ric.substring(ric.length() - 2, ric.length()) + " Comdty");
  }
  
  public static ExternalId ricToBbgFutureOption(String ric, boolean isCall, double strike, LocalDate expiry) {
    String result = ricToBbgFuturePrefix(ric) + dateToBbgCode(expiry) + (isCall ? "C" : "P") + " " + THREE_DECIMAL_PLACES.format(strike) + " Comdty";
    return ExternalId.of(ExternalSchemes.BLOOMBERG_TICKER, result);
  }
 
  private static String ricToBbgFuturePrefix(String ric) {
    if (ric.length() < 4) {
      return null;
    }
    ric = ric.trim().toUpperCase();
    String front = ric.substring(0, (Character.isDigit(ric.charAt(0)) ? 4 : 3));
    if (s_ricToBbgPrefixMap.containsKey(front)) {
      return s_ricToBbgPrefixMap.get(front);
    } else {
      throw new OpenGammaRuntimeException("Could not map RIC onto BBG code");
    }
  }
   
}
