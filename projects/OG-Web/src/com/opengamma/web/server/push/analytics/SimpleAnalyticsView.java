/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.web.server.push.analytics;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opengamma.engine.view.ViewComputationResultModel;
import com.opengamma.engine.view.calc.ViewCycle;
import com.opengamma.engine.view.compilation.CompiledViewDefinition;
import com.opengamma.util.ArgumentChecker;

/**
 * TODO synchronize every method or decorate with an AnalyticsView impl that does
 */
/* package */ class SimpleAnalyticsView implements AnalyticsView {

  private static final Logger s_logger = LoggerFactory.getLogger(SimpleAnalyticsView.class);

  private final AnalyticsHistory _history = new AnalyticsHistory();
  private final AnalyticsViewListener _listener;

  private MainAnalyticsGrid _portfolioGrid;
  private MainAnalyticsGrid _primitivesGrid;

  public SimpleAnalyticsView(AnalyticsViewListener listener, String portoflioGridId, String primitivesGridId) {
    ArgumentChecker.notNull(listener, "listener");
    ArgumentChecker.notNull(portoflioGridId, "portoflioGridId");
    ArgumentChecker.notNull(primitivesGridId, "primitivesGridId");
    _portfolioGrid = MainAnalyticsGrid.empty(GridType.PORTFORLIO, portoflioGridId);
    _primitivesGrid = MainAnalyticsGrid.empty(GridType.PRIMITIVES, primitivesGridId);
    _listener = listener;
  }

  @Override
  public void updateStructure(CompiledViewDefinition compiledViewDefinition) {
    // TODO this loses all dependency graphs. new grid needs to rebuild graphs from old grid. need stable row and col IDs to do that
    _portfolioGrid = MainAnalyticsGrid.portfolio(compiledViewDefinition, _portfolioGrid.getGridId());
    _primitivesGrid = MainAnalyticsGrid.primitives(compiledViewDefinition, _primitivesGrid.getGridId());
    List<String> gridIds = new ArrayList<String>();
    gridIds.add(_portfolioGrid.getGridId());
    gridIds.add(_primitivesGrid.getGridId());
    gridIds.addAll(_portfolioGrid.getDependencyGraphGridIds());
    gridIds.addAll(_primitivesGrid.getDependencyGraphGridIds());
    _listener.gridStructureChanged(gridIds);
  }

  @Override
  public void updateResults(ViewComputationResultModel fullResult) {
    _history.addResults(fullResult);
    _portfolioGrid.updateResults(fullResult, _history);
    _primitivesGrid.updateResults(fullResult, _history);
    List<String> dataIds = new ArrayList<String>();
    dataIds.addAll(_portfolioGrid.getViewportDataIds());
    dataIds.addAll(_primitivesGrid.getViewportDataIds());
    _listener.gridDataChanged(dataIds);
  }

  @Override
  public void updateResults(ViewComputationResultModel fullResult, ViewCycle viewCycle) {
    _history.addResults(fullResult);
    _portfolioGrid.updateResults(fullResult, _history, viewCycle);
    _primitivesGrid.updateResults(fullResult, _history, viewCycle);
    List<String> dataIds = new ArrayList<String>();
    dataIds.addAll(_portfolioGrid.getViewportDataIds());
    dataIds.addAll(_portfolioGrid.getDependencyGraphViewportDataIds());
    dataIds.addAll(_primitivesGrid.getViewportDataIds());
    dataIds.addAll(_primitivesGrid.getDependencyGraphViewportDataIds());
    _listener.gridDataChanged(dataIds);
  }

  @Override
  public boolean isViewCycleRequired() {
    return _portfolioGrid.dependencyGraphVisible() || _primitivesGrid.dependencyGraphVisible();
  }

  private MainAnalyticsGrid getGrid(GridType gridType) {
    switch (gridType) {
      case PORTFORLIO:
        return _portfolioGrid;
      case PRIMITIVES:
        return _primitivesGrid;
      default:
        throw new IllegalArgumentException("Unexpected grid type " + gridType);
    }
  }

  @Override
  public AnalyticsGridStructure getGridStructure(GridType gridType) {
    s_logger.debug("Getting grid structure for the {} grid", gridType);
    return getGrid(gridType).getGridStructure();
  }

  @Override
  public void createViewport(GridType gridType, String viewportId, String dataId, ViewportSpecification viewportSpec) {
    getGrid(gridType).createViewport(viewportId, dataId, viewportSpec, _history);
    s_logger.debug("Created viewport ID {} for the {} grid from {}", new Object[]{viewportId, gridType, viewportSpec});
  }

  @Override
  public void updateViewport(GridType gridType, String viewportId, ViewportSpecification viewportSpec) {
    s_logger.debug("Updating viewport {} for {} grid to {}", new Object[]{viewportId, gridType, viewportSpec});
    getGrid(gridType).updateViewport(viewportId, viewportSpec, _history);
    // TODO fire event - might have to return the viewport dataId. or just return the updated data?
  }

  @Override
  public void deleteViewport(GridType gridType, String viewportId) {
    s_logger.debug("Deleting viewport {} from the {} grid", viewportId, gridType);
    getGrid(gridType).deleteViewport(viewportId);
  }

  @Override
  public ViewportResults getData(GridType gridType, String viewportId) {
    s_logger.debug("Getting data for viewport {} of the {} grid", viewportId, gridType);
    return getGrid(gridType).getData(viewportId);
  }

  @Override
  public void openDependencyGraph(GridType gridType, String graphId, String gridId, int row, int col) {
    s_logger.debug("Opening dependency graph for cell ({}, {}) of the {} grid", new Object[]{row, col, gridType});
    getGrid(gridType).openDependencyGraph(graphId, gridId,row, col);
  }

  @Override
  public void closeDependencyGraph(GridType gridType, String graphId) {
    s_logger.debug("Closing dependency graph {} of the {} grid", graphId, gridType);
    getGrid(gridType).closeDependencyGraph(graphId);
  }

  @Override
  public AnalyticsGridStructure getGridStructure(GridType gridType, String graphId) {
    s_logger.debug("Getting grid structure for dependency graph {} of the {} grid", graphId, gridType);
    return getGrid(gridType).getGridStructure(graphId);
  }

  @Override
  public void createViewport(GridType gridType, String graphId, String viewportId, String dataId, ViewportSpecification viewportSpec) {
    getGrid(gridType).createViewport(graphId, viewportId, dataId, viewportSpec, _history);
    s_logger.debug("Created viewport ID {} for dependency graph {} of the {} grid using {}", new Object[]{viewportId, graphId, gridType, viewportSpec});
  }

  @Override
  public void updateViewport(GridType gridType, String graphId, String viewportId, ViewportSpecification viewportSpec) {
    s_logger.debug("Updating viewport for dependency graph {} of the {} grid using {}", new Object[]{graphId, gridType, viewportSpec});
    getGrid(gridType).updateViewport(graphId, viewportId, viewportSpec, _history);
    // TODO fire event - might have to return the viewport dataId. or just return the updated data?
  }

  @Override
  public void deleteViewport(GridType gridType, String graphId, String viewportId) {
    s_logger.debug("Deleting viewport {} from dependency graph {} of the {} grid", new Object[]{viewportId, graphId, gridType});
    getGrid(gridType).deleteViewport(graphId, viewportId);
  }

  @Override
  public ViewportResults getData(GridType gridType, String graphId, String viewportId) {
    s_logger.debug("Getting data for the viewport {} of the dependency graph {} of the {} grid", new Object[]{viewportId, graphId, gridType});
    return getGrid(gridType).getData(graphId, viewportId);
  }

}
