package org.gusdb.sitesearch.service.metadata;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.gusdb.fgputil.json.JsonUtil;
import org.json.JSONObject;

public class DocumentField {

  private final String _name;
  private final String _term;
  private final String _displayName;
  private final boolean _isMultiText;
  private final boolean _isSummary;
  private final boolean _isSearchable;
  private final boolean _isSubtitle;
  private final double _boost;
  private final Optional<List<String>> _includeProjects;
  private double _boostMultiplier = 1;

  public DocumentField(JSONObject fieldJson) {
    _name = fieldJson.getString("name");
    _term = getTerm(_name);
    _displayName = fieldJson.optString("displayName", getDisplay(_term));
    _isMultiText = _name.indexOf("MULTITEXT") > 0;
    _isSummary = fieldJson.optBoolean("isSummary", true);
    _isSearchable = fieldJson.optBoolean("isSearchable", true);
    _isSubtitle = fieldJson.optBoolean("isSubtitle", false);
    _boost = fieldJson.optDouble("boost", 1);
    _includeProjects = Optional
      .ofNullable(fieldJson.optJSONArray("includeProjects"))
      .map(json -> Arrays.asList(JsonUtil.toStringArray(json)));
  }

  private String getDisplay(String term) {
    return Arrays
        .stream(term.split("_"))
        .map(part -> part.isEmpty() ? part :
          (part.substring(0,1).toUpperCase() + part.substring(1,part.length())))
        .collect(Collectors.joining(" "));
  }

  private static String getTerm(String name) {
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

  public boolean isSearchable() {
    return _isSearchable;
  }

  public boolean isSubtitle() {
    return _isSubtitle;
  }

  public double getBoost() {
    return _boost * _boostMultiplier;
  }

  public boolean includeInProject(String projectId) {
    return _includeProjects
      .map(list -> list.contains(projectId))
      .orElse(true);
  }

  public JSONObject toJson() {
    return new JSONObject()
      .put("name", _name)
      .put("displayName", _displayName)
      .put("term", _term)
      .put("isSubtitle", _isSubtitle);
  }

  public void setBoostMultiplier(double d) {
    _boostMultiplier = d;
  }
}
