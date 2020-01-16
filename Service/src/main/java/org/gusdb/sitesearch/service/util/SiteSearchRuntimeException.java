package org.gusdb.sitesearch.service.util;

public class SiteSearchRuntimeException extends RuntimeException {

  public SiteSearchRuntimeException(String message) {
    super(message);
  }

  public SiteSearchRuntimeException(String message, Exception cause) {
    super(message, cause);
  }

}
