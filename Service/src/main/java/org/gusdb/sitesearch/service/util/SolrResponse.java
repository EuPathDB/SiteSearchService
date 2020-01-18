package org.gusdb.sitesearch.service.util;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.json.JSONObject;

public class SolrResponse {

  private final List<JSONObject> _documents;
  private Map<String,Integer> _facetCounts;

  public SolrResponse(List<JSONObject> documents) {
    _documents = documents;
  }

  public List<JSONObject> getDocuments() {
    return _documents;
  }

  public void setFacetCounts(Map<String, Integer> facetCounts) {
    _facetCounts = facetCounts;
  }

  public Optional<Map<String,Integer>> getFacetCounts() {
    return Optional.ofNullable(_facetCounts);
  }
}
