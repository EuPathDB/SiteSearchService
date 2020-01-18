package org.gusdb.sitesearch.service.search;

import static java.util.Arrays.asList;
import static org.gusdb.fgputil.json.JsonUtil.toStringArray;

import java.util.List;
import java.util.Optional;

import org.json.JSONObject;

public class DocTypeFilter {

  private final String _docType;
  private final List<String> _foundOnlyInFields;

  /**
   * {
   *   documentType: string,
   *   foundOnlyInFields?: string[]
   * }
   */
  public DocTypeFilter(JSONObject json) {
    _docType = json.getString("documentType");
    _foundOnlyInFields = !json.has("foundOnlyInFields") ? null :
        asList(toStringArray(json.getJSONArray("foundOnlyInFields")));
  }

  public String getDocType() {
    return _docType;
  }

  public Optional<List<String>> getFoundOnlyInFields() {
    return Optional.ofNullable(_foundOnlyInFields);
  }

}
