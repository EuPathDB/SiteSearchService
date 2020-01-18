package org.gusdb.sitesearch.service.search;

import static java.util.Arrays.asList;
import static org.gusdb.fgputil.json.JsonUtil.toStringArray;

import java.util.List;
import java.util.Optional;

import org.json.JSONObject;

/**
 * inputSchema = {
 *   searchText: string,
 *   pagination: {
 *     offset: integer,
 *     numRecords: integer
 *   },
 *   restrictToOrganisms?: string[],
 *   filter?: {
 *     documentType: string,
 *     foundOnlyInFields?: string[]
 *   }
 * }
 */
public class SearchRequest {

  private final String _searchText;
  private final Pagination _pagination;
  private final List<String> _restrictToOrganisms;
  private final DocTypeFilter _filter;

  public SearchRequest(JSONObject requestJson) {
    _searchText = requestJson.getString("searchText");
    _pagination = new Pagination(requestJson.getJSONObject("pagination"));
    _restrictToOrganisms = !requestJson.has("restrictToOrganisms") ? null :
      asList(toStringArray(requestJson.getJSONArray("restrictToOrganisms")));
    _filter = !requestJson.has("filter") ? null :
      new DocTypeFilter(requestJson.getJSONObject("filter"));
  }

  public String getSearchText() {
    return _searchText;
  }

  public Pagination getPagination() {
    return _pagination;
  }

  public Optional<List<String>> getRestrictToOrganisms() {
    return Optional.ofNullable(_restrictToOrganisms);
  }

  public Optional<DocTypeFilter> getFilter() {
    return Optional.ofNullable(_filter);
  }

}
