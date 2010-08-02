/**
 * Copyright (C) 2009 - 2010 by OpenGamma Inc.
 *
 * Please see distribution for license.
 */
package com.opengamma.financial.position.master.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.time.Instant;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import com.google.common.base.Objects;
import com.opengamma.DataNotFoundException;
import com.opengamma.engine.position.PortfolioImpl;
import com.opengamma.engine.position.PortfolioNodeImpl;
import com.opengamma.financial.position.master.PortfolioTreeDocument;
import com.opengamma.financial.position.master.PortfolioTreeSearchHistoricRequest;
import com.opengamma.financial.position.master.PortfolioTreeSearchHistoricResult;
import com.opengamma.financial.position.master.PortfolioTreeSearchRequest;
import com.opengamma.financial.position.master.PortfolioTreeSearchResult;
import com.opengamma.id.UniqueIdentifier;
import com.opengamma.util.db.DbMapSqlParameterSource;
import com.opengamma.util.db.Paging;
import com.opengamma.util.time.DateUtil;
import com.opengamma.util.tuple.LongObjectPair;

/**
 * Position master worker to get the portfolio tree.
 */
public class QueryPortfolioTreeDbPositionMasterWorker extends DbPositionMasterWorker {

  /** Logger. */
  private static final Logger s_logger = LoggerFactory.getLogger(QueryPortfolioTreeDbPositionMasterWorker.class);
  /**
   * SQL select.
   */
  protected static final String SELECT =
      "SELECT " +
        "f.id AS portfolio_id, " +
        "f.oid AS portfolio_oid, " +
        "f.ver_from_instant AS ver_from_instant, " +
        "f.ver_to_instant AS ver_to_instant, " +
        "f.corr_from_instant AS corr_from_instant, " +
        "f.corr_to_instant AS corr_to_instant, " +
        "f.name AS portfolio_name, " +
        "n.id AS node_id," +
        "n.oid AS node_oid," +
        "n.tree_left AS tree_left," +
        "n.tree_right AS tree_right," +
        "n.name AS node_name";
//        "(COUNT (*) FROM pos_position p WHERE p.portfolio_oid = f.oid " +
//        "AND p.ver_from_instant <= :version_as_of_instant AND p.ver_to_instant > :version_as_of_instant " +
//        "AND p.corr_from_instant <= :corrected_to_instant AND p.corr_to_instant > :corrected_to_instant " +
//        ") AS position_count";
  /**
   * SQL from.
   */
  protected static final String FROM =
      "FROM pos_portfolio f LEFT JOIN pos_node n ON (n.portfolio_id = f.id) ";

  /**
   * Creates an instance.
   */
  public QueryPortfolioTreeDbPositionMasterWorker() {
    super();
  }

  //-------------------------------------------------------------------------
  @Override
  protected PortfolioTreeDocument getPortfolioTree(final UniqueIdentifier uid) {
    if (uid.isVersioned()) {
      return getPortfolioTreeById(uid);
    } else {
      return getPortfolioTreeByLatest(uid);
    }
  }

  /**
   * Gets a portfolio by searching for the latest version of an object identifier.
   * @param uid  the unique identifier
   * @return the portfolio document, null if not found
   */
  protected PortfolioTreeDocument getPortfolioTreeByLatest(final UniqueIdentifier uid) {
    s_logger.debug("getPortfolioTreeByLatest: {}", uid);
    final Instant now = Instant.now(getTimeSource());
    final PortfolioTreeSearchHistoricRequest request = new PortfolioTreeSearchHistoricRequest(uid, now, now);
    final PortfolioTreeSearchHistoricResult result = getMaster().searchPortfolioTreeHistoric(request);
    if (result.getDocuments().size() != 1) {
      throw new DataNotFoundException("PortfolioTree not found: " + uid);
    }
    return result.getFirstDocument();
  }

  /**
   * Gets a portfolio by identifier.
   * @param uid  the unique identifier
   * @return the portfolio document, null if not found
   */
  @SuppressWarnings("unchecked")
  protected PortfolioTreeDocument getPortfolioTreeById(final UniqueIdentifier uid) {
    s_logger.debug("getPortfolioTreeById {}", uid);
    final DbMapSqlParameterSource args = new DbMapSqlParameterSource()
      .addValue("portfolio_id", uid.getVersion());
    final PortfolioTreeDocumentExtractor extractor = new PortfolioTreeDocumentExtractor();
    final NamedParameterJdbcOperations namedJdbc = getTemplate().getNamedParameterJdbcOperations();
    final List<PortfolioTreeDocument> docs = (List<PortfolioTreeDocument>) namedJdbc.query(sqlGetPortfolioTreeById(), args, extractor);
    if (docs.isEmpty()) {
      throw new DataNotFoundException("PortfolioTree not found: " + uid);
    }
    return docs.get(0);
  }

  /**
   * Gets the SQL for getting a portfolio by unique row identifier.
   * @return the SQL, not null
   */
  protected String sqlGetPortfolioTreeById() {
    return SELECT + FROM + "WHERE f.id = :portfolio_id ";
  }

  //-------------------------------------------------------------------------
  @SuppressWarnings("unchecked")
  @Override
  protected PortfolioTreeSearchResult searchPortfolioTrees(PortfolioTreeSearchRequest request) {
    s_logger.debug("searchPortfolioTrees: {}", request);
    final Instant now = Instant.now(getTimeSource());
    final DbMapSqlParameterSource args = new DbMapSqlParameterSource()
      .addTimestamp("version_as_of_instant", Objects.firstNonNull(request.getVersionAsOfInstant(), now))
      .addTimestamp("corrected_to_instant", Objects.firstNonNull(request.getCorrectedToInstant(), now))
      .addValue("name", getDbHelper().sqlWildcardAdjustValue(request.getName()))
      .addValue("depth", request.getDepth());
    final String[] sql = sqlSearchPortfolioTrees(request);
    final NamedParameterJdbcOperations namedJdbc = getTemplate().getNamedParameterJdbcOperations();
    final int count = namedJdbc.queryForInt(sql[1], args);
    final PortfolioTreeSearchResult result = new PortfolioTreeSearchResult();
    result.setPaging(new Paging(request.getPagingRequest(), count));
    if (count > 0) {
      final PortfolioTreeDocumentExtractor extractor = new PortfolioTreeDocumentExtractor();
      result.getDocuments().addAll((List<PortfolioTreeDocument>) namedJdbc.query(sql[0], args, extractor));
      System.err.println(sql[0]);
    }
    return result;
  }

  /**
   * Gets the SQL to search for portfolios.
   * @param request  the request, not null
   * @return the SQL search and count, not null
   */
  protected String[] sqlSearchPortfolioTrees(final PortfolioTreeSearchRequest request) {
    String where = "WHERE (ver_from_instant <= :version_as_of_instant AND ver_to_instant > :version_as_of_instant) " +
                "AND (corr_from_instant <= :corrected_to_instant AND corr_to_instant > :corrected_to_instant) ";
    if (request.getName() != null) {
      where += getDbHelper().sqlWildcardQuery("AND name ", ":name", request.getName());
    }
    String selectFromWhereInner = "SELECT id FROM pos_portfolio " + where;
    String inner = getDbHelper().sqlApplyPaging(selectFromWhereInner, "ORDER BY id ", request.getPagingRequest());
    String search = SELECT + FROM + "WHERE f.id IN (" + inner + ") ";
    if (request.getDepth() >= 0) {
      search += "AND n.depth <= :depth ";
    }
    search += "ORDER BY f.id, n.tree_left";
    String count = "SELECT COUNT(*) FROM pos_portfolio " + where;
    return new String[] {search, count};
  }

  //-------------------------------------------------------------------------
  @SuppressWarnings("unchecked")
  @Override
  protected PortfolioTreeSearchHistoricResult searchPortfolioTreeHistoric(final PortfolioTreeSearchHistoricRequest request) {
    s_logger.debug("searchPortfolioTreeHistoric: {}", request);
    final DbMapSqlParameterSource args = new DbMapSqlParameterSource()
      .addValue("portfolio_oid", request.getPortfolioId().getValue())
      .addTimestampNullIgnored("versions_from_instant", request.getVersionsFromInstant())
      .addTimestampNullIgnored("versions_to_instant", request.getVersionsToInstant())
      .addTimestampNullIgnored("corrections_from_instant", request.getCorrectionsFromInstant())
      .addTimestampNullIgnored("corrections_to_instant", request.getCorrectionsToInstant())
      .addValue("depth", request.getDepth());
    final String[] sql = sqlSearchPortfolioTreeHistoric(request);
    final NamedParameterJdbcOperations namedJdbc = getTemplate().getNamedParameterJdbcOperations();
    final int count = namedJdbc.queryForInt(sql[1], args);
    final PortfolioTreeSearchHistoricResult result = new PortfolioTreeSearchHistoricResult();
    result.setPaging(new Paging(request.getPagingRequest(), count));
    if (count > 0) {
      final PortfolioTreeDocumentExtractor extractor = new PortfolioTreeDocumentExtractor();
      result.getDocuments().addAll((List<PortfolioTreeDocument>) namedJdbc.query(sql[0], args, extractor));
    }
    return result;
  }

  /**
   * Gets the SQL for searching the history of a portfolio.
   * @param request  the request, not null
   * @return the SQL search and count, not null
   */
  protected String[] sqlSearchPortfolioTreeHistoric(final PortfolioTreeSearchHistoricRequest request) {
    String where = "WHERE oid = :portfolio_oid ";
    if (request.getVersionsFromInstant() != null && request.getVersionsFromInstant().equals(request.getVersionsToInstant())) {
      where += "AND (ver_from_instant <= :versions_from_instant AND ver_to_instant > :versions_from_instant) ";
    } else {
      if (request.getVersionsFromInstant() != null) {
        where += "AND ((ver_from_instant <= :versions_from_instant AND ver_to_instant > :versions_from_instant) " +
                            "OR ver_from_instant >= :versions_from_instant) ";
      }
      if (request.getVersionsToInstant() != null) {
        where += "AND ((ver_from_instant <= :versions_to_instant AND ver_to_instant > :versions_to_instant) " +
                            "OR ver_to_instant < :versions_to_instant) ";
      }
    }
    if (request.getCorrectionsFromInstant() != null && request.getCorrectionsFromInstant().equals(request.getCorrectionsToInstant())) {
      where += "AND (corr_from_instant <= :corrections_from_instant AND corr_to_instant > :corrections_from_instant) ";
    } else {
      if (request.getCorrectionsFromInstant() != null) {
        where += "AND ((corr_from_instant <= :corrections_from_instant AND corr_to_instant > :corrections_from_instant) " +
                            "OR corr_from_instant >= :corrections_from_instant) ";
      }
      if (request.getCorrectionsToInstant() != null) {
        where += "AND ((corr_from_instant <= :corrections_to_instant AND ver_to_instant > :corrections_to_instant) " +
                            "OR corr_to_instant < :corrections_to_instant) ";
      }
    }
    String selectFromWhereInner = "SELECT id FROM pos_portfolio " + where;
    String inner = getDbHelper().sqlApplyPaging(selectFromWhereInner, "ORDER BY ver_from_instant DESC, corr_from_instant DESC ", request.getPagingRequest());
    String search = SELECT + FROM + "WHERE f.id IN (" + inner + ") ";
    if (request.getDepth() >= 0) {
      search += "AND n.depth <= :depth ";
    }
    search += "ORDER BY f.ver_from_instant DESC, f.corr_from_instant DESC, n.tree_left";
    String count = "SELECT COUNT(*) FROM pos_portfolio " + where;
    return new String[] {search, count};
  }

  //-------------------------------------------------------------------------
  /**
   * Mapper from SQL rows to a PortfolioTreeDocument.
   */
  protected final class PortfolioTreeDocumentExtractor implements ResultSetExtractor {
    private long _lastPortfolioId = -1;
    private PortfolioImpl _portfolio;
    private List<PortfolioTreeDocument> _documents = new ArrayList<PortfolioTreeDocument>();
    private final Stack<LongObjectPair<PortfolioNodeImpl>> _nodes = new Stack<LongObjectPair<PortfolioNodeImpl>>();

    @Override
    public Object extractData(ResultSet rs) throws SQLException, DataAccessException {
      while (rs.next()) {
        final long portfolioId = rs.getLong("PORTFOLIO_ID");
        if (_lastPortfolioId != portfolioId) {
          _lastPortfolioId = portfolioId;
          buildPortfolioTree(rs, portfolioId);
        }
        buildNode(rs);
      }
      return _documents;
    }

    private void buildPortfolioTree(final ResultSet rs, final long portfolioId) throws SQLException {
      final long portfolioOid = rs.getLong("PORTFOLIO_OID");
      final Timestamp versionFrom = rs.getTimestamp("VER_FROM_INSTANT");
      final Timestamp versionTo = rs.getTimestamp("VER_TO_INSTANT");
      final Timestamp correctionFrom = rs.getTimestamp("CORR_FROM_INSTANT");
      final Timestamp correctionTo = rs.getTimestamp("CORR_TO_INSTANT");
      final String name = StringUtils.defaultString(rs.getString("PORTFOLIO_NAME"));
      final UniqueIdentifier uid = createUniqueIdentifier(portfolioOid, portfolioId, null);
      _portfolio = new PortfolioImpl(uid, name);
      final PortfolioTreeDocument doc = new PortfolioTreeDocument(_portfolio);
      doc.setVersionFromInstant(DateUtil.fromSqlTimestamp(versionFrom));
      doc.setVersionToInstant(DateUtil.fromSqlTimestamp(versionTo));
      doc.setCorrectionFromInstant(DateUtil.fromSqlTimestamp(correctionFrom));
      doc.setCorrectionToInstant(DateUtil.fromSqlTimestamp(correctionTo));
      _documents.add(doc);
      _nodes.clear();
    }

    private void buildNode(ResultSet rs) throws SQLException {
      final long nodeId = rs.getLong("NODE_ID");
      final long nodeOid = rs.getLong("NODE_OID");
      final long treeLeft = rs.getLong("TREE_LEFT");
      final long treeRight = rs.getLong("TREE_RIGHT");
      final String name = StringUtils.defaultString(rs.getString("NODE_NAME"));
      final UniqueIdentifier uid = createUniqueIdentifier(nodeOid, nodeId, null);
      final PortfolioNodeImpl node = new PortfolioNodeImpl(uid, name);
      if (_nodes.size() == 0) {
        _portfolio.setRootNode(node);
      } else {
        while (treeLeft > _nodes.peek().first) {
          _nodes.pop();
        }
        final PortfolioNodeImpl parent = _nodes.peek().second;
        parent.addChildNode(node);
      }
      // add to stack
      _nodes.push(new LongObjectPair<PortfolioNodeImpl>(treeRight, node));
    }
  }

}
