package org.gusdb.sitesearch.service.util;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.json.JSONObject;

public class SolrResponse {

  private final int _totalCount;
  private final List<JSONObject> _documents;
  private Map<String,Integer> _docTypeFacetCounts;
  private Map<String,Integer> _organismFacetCounts;

  public SolrResponse(int totalCount, List<JSONObject> documents) {
    _totalCount = totalCount;
    _documents = documents;
  }

  public List<JSONObject> getDocuments() {
    return _documents;
  }

  public void setDocTypeFacetCounts(Map<String, Integer> facetCounts) {
    _docTypeFacetCounts = facetCounts;
  }

  public Optional<Map<String,Integer>> getDocTypeFacetCounts() {
    return Optional.ofNullable(_docTypeFacetCounts);
  }

  public void setOrganismFacetCounts(Map<String, Integer> facetCounts) {
    _organismFacetCounts = facetCounts;
  }

  public Optional<Map<String,Integer>> getOrganismFacetCounts() {
    return Optional.ofNullable(_organismFacetCounts);
  }

  public int getTotalCount() {
    return _totalCount;
  }
}
