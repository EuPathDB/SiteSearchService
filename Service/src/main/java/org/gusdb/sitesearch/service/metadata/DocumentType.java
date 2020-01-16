package org.gusdb.sitesearch.service.metadata;

import java.util.Optional;

public class DocumentType {

  private final String _id;
  private final String _displayName;
  private final String _displayNamePlural;
  private final Optional<String> _wdkSearchUrlName;

  public DocumentType(String id, String displayName, String displayNamePlural, String wdkSearchUrlName) {
    _id = id;
    _displayName = displayName;
    _displayNamePlural = displayNamePlural;
    _wdkSearchUrlName = Optional.ofNullable(wdkSearchUrlName);
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

}
