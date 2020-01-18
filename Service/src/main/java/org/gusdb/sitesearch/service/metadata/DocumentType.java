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

  public DocumentType(String id, String displayName, String displayNamePlural, String wdkSearchUrlName) {
    _id = id;
    _displayName = displayName;
    _displayNamePlural = displayNamePlural;
    _wdkSearchUrlName = Optional.ofNullable(wdkSearchUrlName);
    _fields = new ArrayList<>();
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

  public JSONObject toJson() {
    JSONArray fields = new JSONArray();
    for (DocumentField field : _fields) {
      fields.put(field.toJson());
    }
    return new JSONObject()
      .put("id", _id)
      .put("displayName", _displayName)
      .put("displayNamePlural", _displayNamePlural)
      .put("isWdkRecordType", _wdkSearchUrlName.isPresent())
      .put("wdkSearchUrlName", _wdkSearchUrlName.orElse(null))
      .put("fields", fields);
  }
}
