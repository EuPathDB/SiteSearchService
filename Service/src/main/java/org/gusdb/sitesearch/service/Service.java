package org.gusdb.sitesearch.service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.gusdb.fgputil.runtime.BuildStatus;
import org.gusdb.fgputil.server.RESTServer;
import org.gusdb.fgputil.solr.Solr;
import org.gusdb.fgputil.solr.SolrResponse;
import org.gusdb.fgputil.web.MimeTypes;
import org.gusdb.sitesearch.service.metadata.Metadata;
import org.gusdb.sitesearch.service.request.SearchRequest;
import org.gusdb.sitesearch.service.server.Context;
import org.json.JSONObject;

@Path("/")
public class Service {

  private static Solr getSolr() {
    return new Solr((String)RESTServer.getApplicationContext().get(Context.SOLR_URL));
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response runSearch(
      @QueryParam("searchText") @DefaultValue("*") String searchText,
      @QueryParam("offset") @DefaultValue("0") int offset,
      @QueryParam("numRecords") @DefaultValue("20") int numRecords,
      @QueryParam("projectId") String projectId,
      @QueryParam("docType") String docType) {
    return handleSearchRequest(getSolr(), new SearchRequest(searchText,
        offset, numRecords, Optional.ofNullable(docType), Optional.ofNullable(projectId)));
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response runSearch(String body) {
    return handleSearchRequest(getSolr(), new SearchRequest(new JSONObject(body), true, false, false));
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MimeTypes.ND_JSON)
  public Response getStreamingResults(String body) {
    return handleStreamRequest(getSolr(), new SearchRequest(new JSONObject(body), false, true, false));
  }

  @GET
  @Path("/categories-metadata")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getCategoriesJson(@QueryParam("projectId") String projectId) {
    Metadata meta = SolrCalls.initializeMetadata(getSolr());
    return Response.ok(
      new JSONObject()
        .put("categories", meta.getCategoriesJson())
        .put("documentTypes", meta.getDocumentTypesJson(Optional.ofNullable(projectId)))
        .toString(2)
    ).build();
  }

  @GET
  @Path("/build-status")
  @Produces(MediaType.TEXT_PLAIN)
  public Response getBuildStatus() {
    return Response.ok(BuildStatus.getLatestBuildStatus()).build();
  }

  private static Response handleSearchRequest(Solr solr, SearchRequest request) {

    // initialize metadata (2 SOLR calls for docTypes and fields)
    Metadata meta = SolrCalls.initializeMetadata(solr);
    meta.validateRequest(request);

    // get response with all filters in request applied (will produce results to deliver)
    boolean fieldFacetsRequested = request.hasDocTypeFilter();
    SolrResponse searchResults = SolrCalls.getSearchResponse(solr, request, meta, false, true, true, fieldFacetsRequested);

    // apply facets
    meta.applyDocTypeFacetCounts(searchResults.getFacetCounts());
    meta.setOrganismFacetCounts(request.getRestrictMetadataToOrganisms(), searchResults.getFacetCounts());
    if (fieldFacetsRequested) {
      meta.setFieldFacetCounts(request.getDocTypeFilter(), searchResults.getFacetQueryResults());
    }

    // At this point:
    //  - doc type facets are correct because: if doc type filter applied, we only need a count for that type
    //  - organism facets are wrong if org filter present; recalculate with org filter off
    //  - field facets are wrong if field filter present; recalculate with field filter off
    
    if (request.hasOrganismFilter()) {
      // need another call; one without organism filter applied to get org facets
      SolrResponse facetResponse = SolrCalls.getSearchResponse(solr, request, meta, true, false, true, false);
      meta.setOrganismFacetCounts(request.getRestrictMetadataToOrganisms(), facetResponse.getFacetCounts());
    }

    if (request.hasDocTypeFilterAndFields()) {
      // need another call; one without fields filtering applied to get field facets
      SolrResponse facetResponse = SolrCalls.getSearchResponse(solr, request, meta, true, true, false, true);
      meta.setFieldFacetCounts(request.getDocTypeFilter(), facetResponse.getFacetQueryResults());
    }

    return Response.ok(ResultsFormatter.formatResults(meta, searchResults, request.getRestrictToProject()).toString(2)).build();
  }

  private static Response handleStreamRequest(Solr solr, SearchRequest request) {

    // initialize metadata (2 SOLR calls for docTypes and fields)
    Metadata meta = SolrCalls.initializeMetadata(solr);
    meta.validateRequest(request);

    return Response.ok(new StreamingOutput() {
      @Override
      public void write(OutputStream output) throws IOException, WebApplicationException {
        // make the search request and stream primary keys to the client
        SolrCalls.writeSearchResponse(solr, request, meta, output);
      }
    }).build();
  }
}
