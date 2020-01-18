package org.gusdb.sitesearch.service.metadata;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;


public class Category implements Iterable<DocumentType> {

  private final String _name;
  private final List<DocumentType> _docTypes;

  public Category(String name) {
    _name = name;
    _docTypes = new ArrayList<>();
  }

  public String getName() {
    return _name;
  }

  public Category addDocumentTypes(List<DocumentType> values) {
    _docTypes.addAll(values);
    return this;
  }

  @Override
  public Iterator<DocumentType> iterator() {
    return _docTypes.iterator();
  }

  public JSONObject toJson() {
    JSONArray docTypes = new JSONArray();
    for (DocumentType docType : _docTypes) {
      docTypes.put(docType.toJson());
    }
    return new JSONObject()
      .put("name", _name)
      .put("docTypes", docTypes);
  }

}
