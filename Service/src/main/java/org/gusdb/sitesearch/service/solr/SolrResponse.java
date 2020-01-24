package org.gusdb.sitesearch.service.solr;

import java.util.List;
import java.util.Map;

import org.json.JSONObject;

public class SolrResponse {

  private final int _totalCount;
  private final List<JSONObject> _documents;
  private final Map<String,Map<String,Integer>> _facetCounts;
  private final Map<String,List<String>> _highlighting;

  public SolrResponse(
      int totalCount,
      List<JSONObject> documents,
      Map<String,Map<String, Integer>> facetCounts,
      Map<String, List<String>> highlighting) {
    _totalCount = totalCount;
    _documents = documents;
    _facetCounts = facetCounts;
    _highlighting = highlighting;
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

  public Map<String, List<String>> getHighlighting() {
    return _highlighting;
  }
}
