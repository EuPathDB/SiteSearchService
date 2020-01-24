package org.gusdb.sitesearch.service.request;

import static java.util.Arrays.asList;
import static org.gusdb.fgputil.json.JsonUtil.toStringArray;

import java.util.List;
import java.util.Optional;

import org.gusdb.sitesearch.service.exception.InvalidRequestException;
import org.json.JSONObject;

/**
 * inputSchema = {
 *   searchText: string,
 *   pagination: {
 *     offset: integer,
 *     numRecords: integer (max 50)
 *   },
 *   restrictToProject?: string,
 *   restrictMetadataToOrganisms?: string[],
 *   restrictSearchToOrganisms?: string[], (must be subset of metadata orgs)
 *   documentTypeFilter?: {
 *     documentType: string,
 *     foundOnlyInFields?: string[]
 *   }
 * }
 */
public class SearchRequest {

  private final String _searchText;
  private final Pagination _pagination;
  private final String _restrictToProject;
  private final List<String> _restrictMetadataToOrganisms;
  private final List<String> _restrictSearchToOrganisms;
  private final DocTypeFilter _filter;

  public SearchRequest(JSONObject requestJson) {
    _searchText = requestJson.getString("searchText");
    _pagination = new Pagination(requestJson.getJSONObject("pagination"));
    if (_pagination.getNumRecords() > 50)
      throw new InvalidRequestException("numRecords must be <= 50");
    _restrictToProject = requestJson.optString("restrictToProject", null);
    _restrictMetadataToOrganisms = getArrayValues(requestJson, "restrictMetadataToOrganisms");
    _restrictSearchToOrganisms = getArrayValues(requestJson, "restrictSearchToOrganisms");
    ensureSubset(_restrictSearchToOrganisms, _restrictMetadataToOrganisms);
    _filter = !requestJson.has("documentTypeFilter") ? null :
      new DocTypeFilter(requestJson.getJSONObject("documentTypeFilter"));
  }

  private static void ensureSubset(
      List<String> restrictSearchToOrganisms,
      List<String> restrictMetadataToOrganisms) {
    if (restrictSearchToOrganisms == null || restrictMetadataToOrganisms == null) return;
    for (String org : restrictSearchToOrganisms) {
      if (!restrictMetadataToOrganisms.contains(org)) {
        throw new InvalidRequestException("All organisms in search must exist in organism metadata list");
      }
    }
  }

  public static List<String> getArrayValues(JSONObject requestJson, String key) {
    List<String> values = !requestJson.has(key) ? null :
      asList(toStringArray(requestJson.getJSONArray(key)));
    return values != null && values.isEmpty() ? null : values;
  }

  public SearchRequest(String searchText, int offset, int numRecords) {
    _searchText = searchText;
    _pagination = new Pagination(offset, numRecords);
    _restrictToProject = null;
    _restrictMetadataToOrganisms = null;
    _restrictSearchToOrganisms = null;
    _filter = null;
  }

  public String getSearchText() {
    return _searchText;
  }

  public Pagination getPagination() {
    return _pagination;
  }

  public Optional<String> getRestrictToProject() {
    return Optional.ofNullable(_restrictToProject);
  }

  public Optional<List<String>> getRestrictMetadataToOrganisms() {
    return Optional.ofNullable(_restrictMetadataToOrganisms);
  }

  public Optional<List<String>> getRestrictSearchToOrganisms() {
    return Optional.ofNullable(_restrictSearchToOrganisms);
  }

  public Optional<DocTypeFilter> getFilter() {
    return Optional.ofNullable(_filter);
  }

  public boolean hasOrganismFilter() {
    return getRestrictSearchToOrganisms().isPresent();
  }

}
