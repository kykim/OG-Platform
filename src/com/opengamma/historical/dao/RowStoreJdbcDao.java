/**
 * Copyright (C) 2009 - 2010 by OpenGamma Inc.
 * 
 * Please see distribution for license.
 */
package com.opengamma.historical.dao;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.sql.DataSource;
import javax.time.calendar.ZonedDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.id.DomainSpecificIdentifier;
import com.opengamma.timeseries.ArrayDoubleTimeSeries;
import com.opengamma.timeseries.DoubleTimeSeries;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.time.DateUtil;

/**
 * 
 * 
 * @author yomi
 */
public abstract class RowStoreJdbcDao implements TimeSeriesDao {
  
  private static final Logger s_logger = LoggerFactory.getLogger(RowStoreJdbcDao.class);
  
  private static final String DESCRIPTION_COLUMN = "description";
  private static final String NAME_COLUMN = "name";
  private static final String ID_COLUMN = "id";
  private static final String IDENTIFIER_COLUMN = "identifier";
  private static final String DOMAIN_SPEC_IDENTIFIER_TABLE = "domain_spec_identifier"; 
  private static final String DATA_SOURCE_TABLE = "data_source";
  private static final String DATA_PROVIDER_TABLE = "data_provider";
  private static final String QUOTED_OBJECT_TABLE = "quoted_object";
  private static final String DATA_FIELD_TABLE = "data_field";
  private static final String OBSERVATION_TIME_TABLE = "observation_time";
  private static final String DOMAIN_TABLE = "domain";

  private DataSourceTransactionManager _transactionManager;
  private SimpleJdbcTemplate _simpleJdbcTemplate;
  private TransactionDefinition _transactionDefinition = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED);
    
  public RowStoreJdbcDao(DataSourceTransactionManager transactionManager) {
    ArgumentChecker.checkNotNull(transactionManager, "transactionManager");
    _transactionManager = transactionManager;
    DataSource dataSource = _transactionManager.getDataSource();
    _simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);   
  }

  private Date convertZonedDateTime(ZonedDateTime date) {
    ArgumentChecker.checkNotNull(date, "date");
    return new Date(date.toInstant().toEpochMillis());
  }

  @Override
  public void addTimeSeries(Set<DomainSpecificIdentifier> domainIdentifiers,
      String dataSource, String dataProvider, String field,
      String observationTime, final DoubleTimeSeries timeSeries) {
    ArgumentChecker.checkNotNull(domainIdentifiers, "domainIdentifiers");
    ArgumentChecker.checkNotNull(dataSource, "dataSource");
    ArgumentChecker.checkNotNull(dataProvider, "dataProvider");
    ArgumentChecker.checkNotNull(field, "field");
    ArgumentChecker.checkNotNull(observationTime, "observationTime");
    ArgumentChecker.checkNotNull(timeSeries, "timeSeries");

    s_logger.debug("adding timeseries for {} with dataSoruce={}, dataProvider={}, dataField={}, observationTime={}", 
        new Object[]{domainIdentifiers, dataSource, dataProvider, field, observationTime});
    String quotedObject = findQuotedObject(domainIdentifiers);
    if (quotedObject == null) {
      DomainSpecificIdentifier identifier = domainIdentifiers.iterator().next();
      quotedObject = identifier.getDomain().getDomainName() + "_" + identifier.getValue();
    }
    createDomainSpecIdentifiers(domainIdentifiers, quotedObject);
    createTimeSeriesKey(quotedObject, dataSource, dataProvider, field, observationTime);
    final int timeSeriesKeyID = getTimeSeriesKeyID(quotedObject, dataSource, dataProvider, field, observationTime);
    String sql = "INSERT into time_series_data (id, ts_date, value) VALUES (?, ?, ?)";

    JdbcOperations jdbcOperations = _simpleJdbcTemplate.getJdbcOperations();
    jdbcOperations.batchUpdate(sql, new BatchPreparedStatementSetter() {
      
      @Override
      public void setValues(PreparedStatement ps, int i) throws SQLException {
        ps.setInt(1, timeSeriesKeyID);
        ps.setDate(2, convertZonedDateTime(timeSeries.getTime(i)));
        ps.setDouble(3, timeSeries.getValue(i));
      }
      
      @Override
      public int getBatchSize() {
        return timeSeries.size();
      }
    });

  }

  @Override
  public String findDataFieldByID(int id) {
    return findNamedDimensionByID(DATA_FIELD_TABLE, id);
  }

  @Override
  public int createDataProvider(String dataProvider, String description) {
    insertNamedDimension(DATA_PROVIDER_TABLE, dataProvider, description);
    return getDataProviderID(dataProvider);
  }

  /**
   * @param dataProvider
   * @param description
   */
  private void insertNamedDimension(String tableName, String name, String description) {
    ArgumentChecker.checkNotNull(tableName, "table");
    ArgumentChecker.checkNotNull(name, "name");
    s_logger.debug("inserting into table={} values({}, {})", new Object[]{tableName, name, description});
    String sql = "INSERT INTO " + tableName + " (" + NAME_COLUMN + ", " + DESCRIPTION_COLUMN + ") VALUES (?, ?)";
    _simpleJdbcTemplate.update(sql, new Object[]{name, description});
  }

  @Override
  public String findDataProviderByID(int id) {
    return findNamedDimensionByID(DATA_PROVIDER_TABLE, id);
  }

  /**
   * @param id
   * @return
   */
  private String findNamedDimensionByID(String tableName, int id) {
    ArgumentChecker.checkNotNull(tableName, "table");
    s_logger.debug("looking up named dimension from table={} id={}", tableName, id);
    final StringBuffer sql = new StringBuffer("SELECT ").append(NAME_COLUMN).append(" FROM ").append(tableName).append(" WHERE ").append(ID_COLUMN).append(" = ?");
    String result = null;
    try {
      result = _simpleJdbcTemplate.queryForObject(sql.toString(), String.class, new Object[] { id });
    } catch (EmptyResultDataAccessException e) {
      s_logger.debug("Empty row return for id = {} from table = {}", id, tableName);
      result = null;
    }
    return result;
  }

  @Override
  public int createDataSource(String dataSource, String description) {
    insertNamedDimension(DATA_SOURCE_TABLE, dataSource, description);
    return getDataSourceID(dataSource);
  }

  @Override
  public String findDataSourceByID(int id) {
    return findNamedDimensionByID(DATA_SOURCE_TABLE, id);
  }
  
  protected String findQuotedObject(final Set<DomainSpecificIdentifier> domainIdentifiers) {
    String result = null;
    int size = domainIdentifiers.size();
    if (size < 1) {
      return result;
    }
    
    StringBuffer sqlBuffer = new StringBuffer();
    sqlBuffer.append("SELECT qo.name, count(qo.name) as count FROM ").append(QUOTED_OBJECT_TABLE).append(" qo, ")
    .append(DOMAIN_SPEC_IDENTIFIER_TABLE).append(" dsi, ").append(DOMAIN_TABLE).append(" d ")
    .append("WHERE d.id = dsi.domain_id AND qo.id = dsi.quoted_obj_id AND (");
    int orCounter = 1;
    Object[] parameters = new Object[size*2];
    int paramIndex = 0;
    for (DomainSpecificIdentifier domainSpecificIdentifier : domainIdentifiers) {
      sqlBuffer.append("(d.name = ? AND dsi.identifier = ?)");
      parameters[paramIndex++] = domainSpecificIdentifier.getDomain().getDomainName();
      parameters[paramIndex++] = domainSpecificIdentifier.getValue();
      if (orCounter++ != size) {
        sqlBuffer.append(" OR ");
      }
    }
    sqlBuffer.append(" ) GROUP BY qo.name");
    String findIdentifiersSql = sqlBuffer.toString(); 
    List<Map<String, Object>> queryForList = _simpleJdbcTemplate.queryForList(findIdentifiersSql, parameters);
    //should return just one quoted object
    if (queryForList.size() > 1) {
      s_logger.warn("{} has more than 1 quoted object associated to them", domainIdentifiers);
      throw new OpenGammaRuntimeException(domainIdentifiers + " has more than 1 quoted object associated to them");
    }
    if (queryForList.size() == 1) {
      Map<String, Object> row = queryForList.get(0);
      result = (String)row.get("name");
    }
    return result;
  }

  @Override
  public void createDomainSpecIdentifiers(final Set<DomainSpecificIdentifier> domainIdentifiers, final String quotedObj) {
    ArgumentChecker.checkNotNull(domainIdentifiers, "domainIdentifiers");
    //start transaction
    TransactionStatus transactionStatus = _transactionManager.getTransaction(_transactionDefinition);
    
    s_logger.debug("creating domainSpecIdentifiers {} for quotedObj={}", domainIdentifiers, quotedObj);
    try {
      //find existing identifiers
      Set<DomainSpecificIdentifier> resolvedIdentifiers = new HashSet<DomainSpecificIdentifier>(domainIdentifiers);
      int size = domainIdentifiers.size();
      if (size > 0) {
        StringBuffer sqlBuffer = new StringBuffer();
        sqlBuffer.append("SELECT qo.name, count(qo.name) as count FROM ").append(QUOTED_OBJECT_TABLE).append(" qo, ")
        .append(DOMAIN_SPEC_IDENTIFIER_TABLE).append(" dsi, ").append(DOMAIN_TABLE).append(" d ")
        .append("WHERE d.id = dsi.domain_id AND qo.id = dsi.quoted_obj_id AND (");
        int orCounter = 1;
        Object[] parameters = new Object[size*2];
        int paramIndex = 0;
        for (DomainSpecificIdentifier domainSpecificIdentifier : domainIdentifiers) {
          sqlBuffer.append("(d.name = ? AND dsi.identifier = ?)");
          parameters[paramIndex++] = domainSpecificIdentifier.getDomain().getDomainName();
          parameters[paramIndex++] = domainSpecificIdentifier.getValue();
          if (orCounter++ != size) {
            sqlBuffer.append(" OR ");
          }
        }
        sqlBuffer.append(" ) GROUP BY qo.name");
        String findIdentifiersSql = sqlBuffer.toString(); 
        List<Map<String, Object>> queryForList = _simpleJdbcTemplate.queryForList(findIdentifiersSql, parameters);
        //should return just one quoted object
        if (queryForList.size() > 1) {
          s_logger.warn("{} has more than 1 quoted object associated to them", domainIdentifiers);
          throw new OpenGammaRuntimeException(domainIdentifiers + " has more than 1 quoted object associated to them");
        }
        if (queryForList.size() == 1) {
          Map<String, Object> row = queryForList.get(0);
          String loadedQuotedObject = (String)row.get("name");
          if (!loadedQuotedObject.equals(quotedObj)) {
            s_logger.warn("{} has been associated already with {}", loadedQuotedObject, domainIdentifiers);
            throw new OpenGammaRuntimeException(loadedQuotedObject + " has been associated already with " + domainIdentifiers);
          }
          long identifiersCount = Long.valueOf(String.valueOf(row.get("count")));
          if (identifiersCount != domainIdentifiers.size()) {
            Set<DomainSpecificIdentifier> loadeIdentifiers = findDomainSpecIdentifiersByQuotedObject(quotedObj);
            resolvedIdentifiers.removeAll(loadeIdentifiers);
          }
        }
        
      }
  //    Set<DomainSpecificIdentifier> existingIdentifiers = findDomainSpecIdentifiersByQuotedObject(quotedObj);
      if (getQuotedObjectID(quotedObj) == -1) {
        createQuotedObject(quotedObj, quotedObj);
      }
      List<Object[]> batchArgs = new ArrayList<Object[]>();
      for (DomainSpecificIdentifier domainSpecificIdentifier : resolvedIdentifiers) {
        String domainName = domainSpecificIdentifier.getDomain().getDomainName();
        if (getDomainID(domainName) == -1) {
          createDomain(domainName, domainName);
        }
        Object[] values = new Object[] {quotedObj, domainName, domainSpecificIdentifier.getValue()};
        batchArgs.add(values);
      }
      String sql = "INSERT into " + DOMAIN_SPEC_IDENTIFIER_TABLE + " (quoted_obj_id, domain_id, identifier) VALUES ((SELECT id FROM " + QUOTED_OBJECT_TABLE + " WHERE name = ?) ,(SELECT id FROM " + DOMAIN_TABLE + " WHERE name = ?), ?)" ;
      _simpleJdbcTemplate.batchUpdate(sql, batchArgs);
      _transactionManager.commit(transactionStatus);
    } catch (Throwable t) {
      _transactionManager.rollback(transactionStatus);
      s_logger.warn("error trying to create domainSpecIdentifiers", t);
      throw new OpenGammaRuntimeException("Unable to create DomainSpecificIdentifiers", t);
    }
    
  }

  @Override
  public int createObservationTime(String observationTime, String description) {
    insertNamedDimension(OBSERVATION_TIME_TABLE, observationTime, description);
    return getObservationTimeID(observationTime);
  }

  @Override
  public int createQuotedObject(String name, String description) {
    insertNamedDimension(QUOTED_OBJECT_TABLE, name, description);
    return getQuotedObjectID(name);
  }

  @Override
  public int createDataField(String field, String description) {
    insertNamedDimension(DATA_FIELD_TABLE, field, description);
    return getDataFieldID(field);
  }

  @Override
  public Set<String> getAllDataProviders() {
    return getAllNamedDimensionNames(DATA_PROVIDER_TABLE);
  }

  @Override
  public Set<String> getAllDataSources() {
    return getAllNamedDimensionNames(DATA_SOURCE_TABLE);
  }

  /**
   * @return
   */
  private Set<String> getAllNamedDimensionNames(final String tableName) {
    ArgumentChecker.checkNotNull(tableName, "tableName");
    
    s_logger.debug("gettting all names from table = {}", tableName);
    
    final StringBuffer sql = new StringBuffer("SELECT ").append(NAME_COLUMN).append(" fROM ").append(tableName);

    List<Map<String, Object>> queryForList = _simpleJdbcTemplate.queryForList(
        sql.toString(), new Object[] {});
    Set<String> result = new TreeSet<String>();

    for (Map<String, Object> row : queryForList) {
      String name = (String) row.get(NAME_COLUMN);
      result.add(name);
    }

    return result;
  }
  
  @Override
  public String findQuotedObjectByID(int id) {
    return findNamedDimensionByID(QUOTED_OBJECT_TABLE, id);
  }
  
  @Override
  public String findObservationTimeByID(int id) {
    return findNamedDimensionByID(OBSERVATION_TIME_TABLE, id);
  }

  @Override
  public Set<String> getAllObservationTimes() {
    return getAllNamedDimensionNames(OBSERVATION_TIME_TABLE);
  }

  @Override
  public Set<String> getAllQuotedObjects() {
    return getAllNamedDimensionNames(QUOTED_OBJECT_TABLE);
  }

  @Override
  public Set<String> getAllTimeSeriesFields() {
    return getAllNamedDimensionNames(DATA_FIELD_TABLE);
  }

  @Override
  public int getDataProviderID(String name) {
    return getNamedDimensionID(DATA_PROVIDER_TABLE, name);
  }

  /**
   * @param name
   * @return
   */
  private int getNamedDimensionID(final String tableName, final String name) {
    ArgumentChecker.checkNotNull(tableName, "tableName");
    ArgumentChecker.checkNotNull(name, "name");
    s_logger.debug("looking up id from table={} with name={}", tableName, name);
    final StringBuffer sql = new StringBuffer("SELECT ").append(ID_COLUMN).append(" FROM ").append(tableName).append(" where ").append(NAME_COLUMN).append(" = ?");
    int result = -1;
    try {
      result = _simpleJdbcTemplate.queryForInt(sql.toString(), new Object[] { name });
    } catch(EmptyResultDataAccessException e) {
      s_logger.debug("Empty row return for name = {} from table = {}", name, tableName);
      result = -1;
    }
    return result;
  }

  @Override
  public int getDataSourceID(String name) {
    return getNamedDimensionID(DATA_SOURCE_TABLE, name);
  }

  @Override
  public int getDataFieldID(String name) {
    return getNamedDimensionID(DATA_FIELD_TABLE, name);
  }

  @Override
  public int getObservationTimeID(String name) {
    return getNamedDimensionID(OBSERVATION_TIME_TABLE, name);
  }

  @Override
  public int getQuotedObjectID(String name) {
    return getNamedDimensionID(QUOTED_OBJECT_TABLE, name);
  }

  @Override
  public DoubleTimeSeries getTimeSeries(DomainSpecificIdentifier domainSpecId,
      String dataSource, String dataProvider, String field,
      String observationTime) {
    ArgumentChecker.checkNotNull(domainSpecId, "identifier");
    ArgumentChecker.checkNotNull(dataSource, "dataSource");
    ArgumentChecker.checkNotNull(dataProvider, "dataProvider");
    ArgumentChecker.checkNotNull(field, "field");
    ArgumentChecker.checkNotNull(observationTime, "observationTime");
    
    s_logger.debug("getting timeseries for identifier={} dataSource={} dataProvider={} dataField={} observationTime={}", 
        new Object[]{domainSpecId, dataSource, dataProvider, field, observationTime});
    
    String sql = "SELECT ts_date, value FROM time_series_data " +
    		" WHERE id = (SELECT tsKey.id FROM time_series_key tsKey, quoted_object qo, domain_spec_identifier dsi," +
    		" domain d, data_source ds, data_provider dp, data_field df, observation_time ot " +
    		" WHERE dsi.domain_id = d.id AND dsi.quoted_obj_id = qo.id " +
    		" AND tsKey.qouted_obj_id = qo.id " +
    		" AND tsKey.data_soure_id = ds.id " +
    		" AND tsKey.data_provider_id = dp.id " +
    		" AND tsKey.data_field_id = df.id" +
    		" AND observation_time_id = ot.id " +
    		" AND dsi.identifier = ? " +
        " AND d.name = ? " +
        " AND ds.name = ? " +
        " AND dp.name = ? " +
        " AND df.name = ? " +
        " AND ot.name = ?)" +
        " ORDER BY ts_date";
    
    Object[] parameters = new Object[] {domainSpecId.getValue(), domainSpecId.getDomain().getDomainName(), dataSource, 
        dataProvider, field, observationTime};
    
    List<TimeSeriesData> queryResult = _simpleJdbcTemplate.query(sql, new ParameterizedRowMapper<TimeSeriesData>() {

      @Override
      public TimeSeriesData mapRow(ResultSet rs, int rowNum)
          throws SQLException {
        double tsValue = rs.getDouble("value");
        Date tsDate = rs.getDate("ts_date");
        return new TimeSeriesData(tsDate, tsValue);
      }
    }, parameters);
    
    List<ZonedDateTime> times = new ArrayList<ZonedDateTime>();
    List<Double> values = new ArrayList<Double>();
    for (TimeSeriesData tsData : queryResult) {
      double tsValue = tsData.getValue();
      values.add(tsValue);
      times.add(DateUtil.getUTCDate(tsData.getYear(), tsData.getMonth(), tsData.getDay()));
    }
    
    return new ArrayDoubleTimeSeries(times, values);
  }
  
  @Override
  public Set<DomainSpecificIdentifier> findDomainSpecIdentifiersByQuotedObject(String name) {
    ArgumentChecker.checkNotNull(name, "name");
    
    s_logger.debug("looking up domainSpecIdentifiers using quotedObj={}", name);
    
    String sql = "SELECT d.name, dsi.identifier " +
    		         "FROM domain_spec_identifier dsi, domain d, quoted_object qo " +
    		         "WHERE dsi.domain_id = d.id " +
    		         "AND qo.id = dsi.quoted_obj_id " +
    		         "AND qo.name = ? ORDER BY d.name";
    Set<DomainSpecificIdentifier> result = new HashSet<DomainSpecificIdentifier>();
    List<Map<String, Object>> queryForList = _simpleJdbcTemplate.queryForList(sql, new Object[]{name});
    for (Map<String, Object> row : queryForList) {
      DomainSpecificIdentifier identifier = new DomainSpecificIdentifier((String)row.get(NAME_COLUMN), (String)row.get(IDENTIFIER_COLUMN));
      result.add(identifier);
    }
    return result;
  }
  
  @Override
  public int createDomain(String domain, String description) {
    insertNamedDimension(DOMAIN_TABLE, domain, description);
    return getDomainID(domain);
  }

  @Override
  public String findDomainByID(int id) {
    return findNamedDimensionByID(DOMAIN_TABLE, id);
  }

  @Override
  public Set<String> getAllDomains() {
    return getAllNamedDimensionNames(DOMAIN_TABLE);
  }

  @Override
  public int getDomainID(String name) {
    return getNamedDimensionID(DOMAIN_TABLE, name);
  }
  
  @Override
  public void createTimeSeriesKey(String quotedObject, String dataSource,
      String dataProvider, String dataField, String observationTime) {
    ArgumentChecker.checkNotNull(quotedObject, "quotedObject");
    ArgumentChecker.checkNotNull(dataSource, "dataSource");
    ArgumentChecker.checkNotNull(dataProvider, "dataProvider");
    ArgumentChecker.checkNotNull(dataField, "dataField");
    ArgumentChecker.checkNotNull(observationTime, "observationTime");
    
    s_logger.debug("creating timeSeriesKey with quotedObj={}, dataSource={}, dataProvider={}, dataField={}, observationTime={}", 
        new Object[]{quotedObject, dataSource, dataProvider, dataField, observationTime});
    
    if (getDataSourceID(dataSource) == -1) {
      createDataSource(dataSource, null);
    }
    if (getDataProviderID(dataProvider) == -1) {
      createDataProvider(dataProvider, null);
    }
    if (getDataFieldID(dataField) == -1) {
      createDataField(dataField, null);
    }
    if (getObservationTimeID(observationTime) == -1) {
      createObservationTime(observationTime, null);
    }
    String sql = "INSERT into time_series_key " +
    		" (qouted_obj_id, data_soure_id, data_provider_id, data_field_id, observation_time_id)" +
    		" VALUES " +
    		"((SELECT id from quoted_object where name = ?)," +
    		" (SELECT id from data_source where name = ?)," +
    		" (SELECT id from data_provider where name = ?)," +
    		" (SELECT id from data_field where name = ?)," +
    		" (SELECT id from observation_time where name = ?))";
    _simpleJdbcTemplate.update(sql, new Object[]{quotedObject, dataSource, dataProvider, dataField, observationTime});
  }
  
  protected int getTimeSeriesKeyID(String quotedObject, String dataSource,
      String dataProvider, String dataField, String observationTime) {
    ArgumentChecker.checkNotNull(quotedObject, "quotedObject");
    ArgumentChecker.checkNotNull(dataSource, "dataSource");
    ArgumentChecker.checkNotNull(dataProvider, "dataProvider");
    ArgumentChecker.checkNotNull(dataField, "dataField");
    ArgumentChecker.checkNotNull(observationTime, "observationTime");
    
    s_logger.debug("looking up id for timeSeriesKey with quotedObj={}, dataSource={}, dataProvider={}, dataField={}, observationTime={}", 
        new Object[]{quotedObject, dataSource, dataProvider, dataField, observationTime});
    
    String sql = "SELECT tskey.id FROM " +
    		" time_series_key tskey, " +
    		" quoted_object qo,  " +
    		" data_source ds, " +
    		" data_provider dp, " +
    		" data_field df, " +
    		" observation_time ot" +
    		" WHERE " +
    		" tskey.qouted_obj_id = qo.id " +
    		" AND tskey.data_soure_id = ds.id " +
    		" AND tskey.data_provider_id = dp.id " +
    		" AND tskey.data_field_id = df.id " +
    		" AND tskey.observation_time_id = ot.id " +
    		" AND qo.name = ? " +
    		" AND ds.name = ? " +
    		" AND dp.name = ? " +
    		" AND df.name = ? " +
    		" AND ot.name = ?";
    return _simpleJdbcTemplate.queryForInt(sql, new Object[]{quotedObject, dataSource, dataProvider, dataField, observationTime});
  }
  
  private static class TimeSeriesData {
    private Date _date;
    private double _value;
    private SimpleDateFormat _yearFormat = new SimpleDateFormat("yyyy");
    private SimpleDateFormat _monthFormat = new SimpleDateFormat("MM");
    private SimpleDateFormat _dayFormat = new SimpleDateFormat("dd");
   
    public TimeSeriesData(Date date, double value) {
      ArgumentChecker.checkNotNull(date, "date");
      _date = date;
      _value = value;
    }
    
    public double getValue() {
      return _value;
    }
    
    public int getYear() {
      return Integer.parseInt(_yearFormat.format(_date));
    }
    
    public int getMonth() {
      return Integer.parseInt(_monthFormat.format(_date));
    }
    
    public int getDay() {
      return Integer.parseInt(_dayFormat.format(_date));
    }
    
  }
  
}
