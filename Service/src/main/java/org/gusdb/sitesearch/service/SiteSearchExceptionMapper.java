package org.gusdb.sitesearch.service;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.apache.log4j.Logger;
import org.gusdb.sitesearch.service.util.SiteSearchRuntimeException;

public class SiteSearchExceptionMapper implements ExceptionMapper<Exception> {

  private static final Logger LOG = Logger.getLogger(SiteSearchExceptionMapper.class);

  @Override
  public Response toResponse(Exception exception) {
    try { throw exception; }

    catch (SiteSearchRuntimeException e) {
      LOG.error("Server runtime exception occurred while processing request", e);
      return Response.serverError().build();
    }

    catch (Exception e) {
      // all other exceptions
      LOG.error("Unknown exception occurred while processing request", e);
      return Response.serverError().build();
    }

  }
}
