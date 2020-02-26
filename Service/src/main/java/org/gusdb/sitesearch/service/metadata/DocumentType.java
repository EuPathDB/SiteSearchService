package org.gusdb.sitesearch.service.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

public class DocumentType {

  private final String _id;
  private final String _displayName;
  private final String _displayNamePlural;
  private final boolean _hasOrganismField;
  private final double _boost;
  private final Optional<String> _wdkSearchUrlName;
  private final List<DocumentField> _fields;

  private Optional<Integer> _count;

  public DocumentType(
      String id,
      String displayName,
      String displayNamePlural,
      boolean hasOrganismField,
      double boost,
      String wdkSearchUrlName) {
    _id = id;
    _displayName = displayName;
    _displayNamePlural = displayNamePlural;
    _hasOrganismField = hasOrganismField;
    _boost = boost;
    _wdkSearchUrlName = Optional.ofNullable(wdkSearchUrlName);
    _fields = new ArrayList<>();
    _count = Optional.empty();
  }

  public void addFields(List<DocumentField> newFields) {
    _fields.addAll(newFields);
    newFields.stream().forEach(f -> f.setBoostMultiplier(_boost));
    Collections.sort(_fields, (df1, df2) ->
      df1.getDisplayName().compareToIgnoreCase(df2.getDisplayName()));
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

  public boolean hasOrganismField() {
    return _hasOrganismField;
  }

  public double getBoost() {
    return _boost;
  }

  public Optional<String> getWdkSearchUrlName() {
    return _wdkSearchUrlName;
  }

  public List<DocumentField>  getSummaryFields(Optional<String> projectId) {
    return getFields(projectId, DocumentField::isSummary);
  }

  public List<DocumentField>  getSearchFields(Optional<String> projectId) {
    return getFields(projectId, DocumentField::isSearchable);
  }

  private List<DocumentField> getFields(Optional<String> projectId, Predicate<DocumentField> pred) {
    return _fields.stream()
      .filter(field -> projectId.isEmpty() || field.includeInProject(projectId.get()))
      .filter(pred)
      .collect(Collectors.toList());
  }

  private static JSONArray toJson(List<DocumentField> fields) {
    JSONArray array = new JSONArray();
    for (DocumentField field : fields) {
      array.put(field.toJson());
    }
    return array;
  }

  public JSONObject toJson(Optional<String> projectId) {
    return new JSONObject()
      .put("id", _id)
      .put("displayName", _displayName)
      .put("displayNamePlural", _displayNamePlural)
      .put("hasOrganismField", _hasOrganismField)
      .put("count", _count.orElse(0))
      .put("isWdkRecordType", _wdkSearchUrlName.isPresent())
      .put("summaryFields", toJson(getSummaryFields(projectId)))
      .put("wdkRecordTypeData", _wdkSearchUrlName
        .map(searchName ->
          new JSONObject()
            .put("searchName", searchName)
            .put("searchFields", toJson(getSearchFields(projectId))))
        .orElse(null));
  }

  public void setCount(int count) {
    _count = Optional.of(count);
  }
}
