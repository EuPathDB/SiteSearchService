package org.gusdb.sitesearch.service.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.gusdb.fgputil.solr.SolrRuntimeException;

public class SiteSearchExceptionMapper implements ExceptionMapper<Exception> {

  private static final Logger LOG = LogManager.getLogger(SiteSearchExceptionMapper.class);

  @Override
  public Response toResponse(Exception exception) {
    try { throw exception; }

    catch (SolrRuntimeException | SiteSearchRuntimeException e) {
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
