package org.gusdb.sitesearch.service.request;

import org.gusdb.sitesearch.service.exception.InvalidRequestException;
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
    validate();
  }

  public Pagination(int offset, int numRecords) {
    _offset = offset;
    _numRecords = numRecords;
    validate();
  }

  private void validate() {
    if (_offset < 0)
      throw new InvalidRequestException("offset must be >= 0");
    if (_numRecords < 0) {
      throw new InvalidRequestException("numRecords must be >= 0");
    }
  }

  public int getOffset() {
    return _offset;
  }

  public int getNumRecords() {
    return _numRecords;
  }
}
