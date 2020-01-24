package org.gusdb.sitesearch.service.metadata;

import org.json.JSONObject;

public class DocumentField {

  private final String _name;
  private final String _displayName;
  private final boolean _isMultiText;
  private final boolean _isSummary;
  private final double _boost;

  public DocumentField(JSONObject fieldJson) {
    _name = fieldJson.getString("name");
    _displayName = fieldJson.optString("displayName", getRawName(_name));
    _isMultiText = _name.indexOf("MULTITEXT") > 0;
    _isSummary = fieldJson.optBoolean("isSummary", true);
    _boost = fieldJson.optDouble("boost", 1);
  }

  private static String getRawName(String name) {
    // trim solr type
    name = name.substring(name.indexOf("MULTITEXT") < 0 ? "TEXT__".length() : "MULTITEXT__".length());
    // trim document type
    return name.substring(name.indexOf('_') + 1);
  }

  public String getName() {
    return _name;
  }

  public String getDisplayName() {
    return _displayName;
  }

  public boolean isMultiText() {
    return _isMultiText;
  }

  public boolean isSummary() {
    return _isSummary;
  }

  public double getBoost() {
    return _boost;
  }

  public JSONObject toJson(JsonDestination dest) {
    JSONObject json = new JSONObject()
      .put("name", _name)
      .put("displayName", _displayName);
    return dest.equals(JsonDestination.OUTPUT) ? json : json
      .put("isSummary", _isSummary)
      .put("boost", _boost);
  }

}
