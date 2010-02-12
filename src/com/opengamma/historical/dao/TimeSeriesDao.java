/**
 * Copyright (C) 2009 - 2010 by OpenGamma Inc.
 *
 * Please see distribution for license.
 */
package com.opengamma.historical.dao;

import java.util.Set;

import com.opengamma.id.DomainSpecificIdentifier;
import com.opengamma.timeseries.DoubleTimeSeries;

/**
 * 
 *
 * @author yomi
 */
public interface TimeSeriesDao {
  
  public int createDomain(String domain, String description);
  
  public String findDomainByID(int id);
  
  public int getDomainID(String name);
  
  public Set<String> getAllDomains();
  
  public int createDataSource(String dataSource, String description);
  
  public String findDataSourceByID(int id);
  
  public int getDataSourceID(String name);
  
  public Set<String> getAllDataSources();
  
  public int createDataProvider(String dataProvider, String description);
  
  public String findDataProviderByID(int id);
  
  public int getDataProviderID(String name);
  
  public Set<String> getAllDataProviders();
  
  public int createDataField(String field, String description);
  
  public String findDataFieldByID(int id);
  
  public int getDataFieldID(String name);
  
  public Set<String> getAllTimeSeriesFields();
  
  public int createObservationTime(String observationTime, String description);
  
  public int getObservationTimeID(String name);
  
  public String findObservationTimeByID(int id);
  
  public Set<String> getAllObservationTimes();
  
  public int createQuotedObject(String name, String description);
  
  public int getQuotedObjectID(String name);
  
  public String findQuotedObjectByID(int id);
  
  public Set<String> getAllQuotedObjects();
  
  public void createDomainSpecIdentifiers(Set<DomainSpecificIdentifier> domainIdentifiers, String quotedObj);
  
  public void createTimeSeriesKey(String quotedObject, String dataSource, String dataProvider, String dataField, String observationTime);
  
  public Set<DomainSpecificIdentifier> findDomainSpecIdentifiersByQuotedObject(String name);

  public void addTimeSeries(Set<DomainSpecificIdentifier> domainIdentifiers, String dataSource, String dataProvider, String field,  String observationTime, DoubleTimeSeries timeSeries);
  
  public DoubleTimeSeries getTimeSeries(DomainSpecificIdentifier domainSpecId, String dataSource, String dataProvider, String field,  String observationTime);

  /*
  public List<DoubleTimeSeries> getTimeSeriesByDataSource(String secDes, String dataSource);
  
  public List<DoubleTimeSeries> getTimeSeriesByDataProvider(String secDes, String dataProvider);
  
  public List<DoubleTimeSeries> getTimeSeriesByField(String secDes, String field);
  
  public List<DoubleTimeSeries> getTimeSeriesByObservationTime(String secDes, String observationTime);
  */
  
}
