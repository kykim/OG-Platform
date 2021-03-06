package com.opengamma.web.server.push.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.opengamma.core.marketdatasnapshot.impl.ManageableMarketDataSnapshot;
import com.opengamma.master.marketdatasnapshot.MarketDataSnapshotMaster;
import com.opengamma.master.marketdatasnapshot.MarketDataSnapshotSearchRequest;
import com.opengamma.master.marketdatasnapshot.MarketDataSnapshotSearchResult;

/**
 * REST resource that produces a JSON list of market data snapshots and their IDs.  This isn't a full REST
 * interface for market data snapshots, it's intended for populating data in the web client.
 */
@Path("marketdatasnapshots")
public class MarketDataSnapshotListResource {

  private static final Logger s_logger = LoggerFactory.getLogger(MarketDataSnapshotListResource.class);

  static final String BASIS_VIEW_NAME = "basisViewName";
  static final String SNAPSHOTS = "snapshots";
  static final String ID = "id";
  static final String NAME = "name";

  private static final Pattern s_guidPattern =
      Pattern.compile("(\\{?([0-9a-fA-F]){8}-([0-9a-fA-F]){4}-([0-9a-fA-F]){4}-([0-9a-fA-F]){4}-([0-9a-fA-F]){12}\\}?)");

  private final MarketDataSnapshotMaster _snapshotMaster;

  public MarketDataSnapshotListResource(MarketDataSnapshotMaster snapshotMaster) {
    _snapshotMaster = snapshotMaster;
  }

  /**
   * @return JSON {@code [{basisViewName: basisViewName1, snapshots: [{id: snapshot1Id, name: snapshot1Name}, ...]}, ...]}
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public String getMarketDataSnapshotList() {
    MarketDataSnapshotSearchRequest snapshotSearchRequest = new MarketDataSnapshotSearchRequest();
    snapshotSearchRequest.setIncludeData(false);
    MarketDataSnapshotSearchResult snapshotSearchResult = _snapshotMaster.search(snapshotSearchRequest);
    List<ManageableMarketDataSnapshot> snapshots = snapshotSearchResult.getSnapshots();
    Multimap<String, ManageableMarketDataSnapshot> snapshotsByBasisView = ArrayListMultimap.create();

    for (ManageableMarketDataSnapshot snapshot : snapshots) {
      if (snapshot.getUniqueId() == null) {
        s_logger.warn("Ignoring snapshot with null unique identifier {}", snapshot.getName());
        continue;
      }
      if (StringUtils.isBlank(snapshot.getName())) {
        s_logger.warn("Ignoring snapshot {} with no name", snapshot.getUniqueId());
        continue;
      }
      if (s_guidPattern.matcher(snapshot.getName()).find()) {
        s_logger.debug("Ignoring snapshot which appears to have an auto-generated name: {}", snapshot.getName());
        continue;
      }
      String basisViewName = snapshot.getBasisViewName() != null ? snapshot.getBasisViewName() : "unknown";
      snapshotsByBasisView.put(basisViewName, snapshot);
    }
    // list of maps for each basis view: {"basisViewName": basisViewName, "snapshots", [...]}
    List<Map<String, Object>> basisViewSnapshotList = new ArrayList<Map<String, Object>>();
    for (String basisViewName : snapshotsByBasisView.keySet()) {
      Collection<ManageableMarketDataSnapshot> viewSnapshots = snapshotsByBasisView.get(basisViewName);
      // list of maps containing snapshot IDs and names: {"id", snapshotId, "name", snapshotName}
      List<Map<String, Object>> snapshotsList = new ArrayList<Map<String, Object>>(viewSnapshots.size());
      for (ManageableMarketDataSnapshot viewSnapshot : viewSnapshots) {
        // map for a single snapshot: {"id", snapshotId, "name", snapshotName}
        Map<String, Object> snapshotMap =
            ImmutableMap.<String, Object>of(ID, viewSnapshot.getUniqueId(), NAME, viewSnapshot.getName());
        snapshotsList.add(snapshotMap);
      }
      basisViewSnapshotList.add(ImmutableMap.of(BASIS_VIEW_NAME, basisViewName, SNAPSHOTS, snapshotsList));
    }
    return new JSONArray(basisViewSnapshotList).toString();
  }
}
