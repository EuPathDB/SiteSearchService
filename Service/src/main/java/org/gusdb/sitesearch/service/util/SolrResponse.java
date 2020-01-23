package org.gusdb.sitesearch.service.util;

import java.util.List;
import java.util.Map;

import org.json.JSONObject;

public class SolrResponse {

  private final int _totalCount;
  private final List<JSONObject> _documents;
  private final Map<String,Map<String,Integer>> _facetCounts;

  public SolrResponse(
      int totalCount,
      List<JSONObject> documents,
      Map<String,Map<String, Integer>> facetCounts) {
    _totalCount = totalCount;
    _documents = documents;
    _facetCounts = facetCounts;
  }

  public int getTotalCount() {
    return _totalCount;
  }

  public List<JSONObject> getDocuments() {
    return _documents;
  }

  public Map<String,Map<String,Integer>> getFacetCounts() {
    return _facetCounts;
  }

}
