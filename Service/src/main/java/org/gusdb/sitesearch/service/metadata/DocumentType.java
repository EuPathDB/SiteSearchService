package org.gusdb.sitesearch.service.metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONObject;

public class DocumentType {

  private final String _id;
  private final String _displayName;
  private final String _displayNamePlural;
  private final Optional<String> _wdkSearchUrlName;
  private final List<DocumentField> _fields;

  private Optional<Integer> _count;

  public DocumentType(String id, String displayName, String displayNamePlural, String wdkSearchUrlName) {
    _id = id;
    _displayName = displayName;
    _displayNamePlural = displayNamePlural;
    _wdkSearchUrlName = Optional.ofNullable(wdkSearchUrlName);
    _fields = new ArrayList<>();
    _count = Optional.empty();
  }

  public void addFields(List<DocumentField> newFields) {
    _fields.addAll(newFields);
  }

  public String getId() {
    return _id;
  }

  public String getDisplayName() {
    return _displayName;
  }

  public String getDisplayNamePlural() {
    return _displayNamePlural;
  }

  public Optional<String> getWdkSearchUrlName() {
    return _wdkSearchUrlName;
  }

  public List<DocumentField> getFields() {
    return _fields;
  }

  public Optional<Integer> getCount() {
    return _count;
  }

  public JSONObject toJson(JsonDestination dest) {
    JSONArray fields = new JSONArray();
    for (DocumentField field : _fields) {
      fields.put(field.toJson(dest));
    }
    return new JSONObject()
      .put("id", _id)
      .put("displayName", _displayName)
      .put("displayNamePlural", _displayNamePlural)
      .put("isWdkRecordType", _wdkSearchUrlName.isPresent())
      .put("wdkRecordTypeData", _wdkSearchUrlName
        .map(searchName ->
          new JSONObject()
            .put("searchUrlName", searchName)
            .put("fields", fields))
        .orElse(null));
  }

  public void setCount(int count) {
    _count = Optional.of(count);
  }
}
