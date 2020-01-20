package org.gusdb.sitesearch.service.search;

import org.json.JSONObject;

public class Pagination {

  private final int _offset;
  private final int _numRecords;

  /**
   * {
   *   offset: integer,
   *   numRecords: integer
   * }
   */
  public Pagination(JSONObject json) {
    _offset = json.getInt("offset");
    _numRecords = json.getInt("numRecords");
  }

  public Pagination(int offset, int numRecords) {
    _offset = offset;
    _numRecords = numRecords;
  }

  public int getOffset() {
    return _offset;
  }

  public int getNumRecords() {
    return _numRecords;
  }
}
