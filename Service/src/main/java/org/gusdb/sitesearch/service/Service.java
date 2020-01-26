package org.gusdb.sitesearch.service;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.gusdb.fgputil.runtime.BuildStatus;
import org.gusdb.fgputil.server.RESTServer;
import org.gusdb.fgputil.solr.Solr;
import org.gusdb.fgputil.solr.SolrResponse;
import org.gusdb.sitesearch.service.metadata.JsonDestination;
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
      @QueryParam("searchText") String searchText,
      @QueryParam("offset") int offset,
      @QueryParam("numRecords") int numRecords) {
    return handleSearchRequest(getSolr(), new SearchRequest(searchText, offset, numRecords));
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  public Response runSearch(String body) {
    return handleSearchRequest(getSolr(), new SearchRequest(new JSONObject(body)));
  }

  private static Response handleSearchRequest(Solr solr, SearchRequest request) {

    // initialize metadata (2 SOLR calls for docTypes and fields)
    Metadata meta = SolrCalls.initializeMetadata(solr);

    // get response with all filters in request applied
    SolrResponse searchResults = SolrCalls.getSearchResponse(solr, request, meta, false);

    // apply facets
    meta.applyDocTypeFacetCounts(searchResults.getFacetCounts());
    meta.setOrganismFacetCounts(request.getRestrictMetadataToOrganisms(), searchResults.getFacetCounts());

    if (request.hasOrganismFilter()) {
      // need a second call; one without organism filter applied to get org facets
      SolrResponse facetResponse = SolrCalls.getSearchResponse(solr, request, meta, true);
      meta.setOrganismFacetCounts(request.getRestrictMetadataToOrganisms(), facetResponse.getFacetCounts());
    }

    return Response.ok(ResultsFormatter.formatResults(meta, searchResults).toString(2)).build();
  }

  @GET
  @Path("/categories-metadata")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getCategoriesJson() {
    Metadata meta = SolrCalls.initializeMetadata(getSolr());
    return Response.ok(
      new JSONObject()
        .put("categories", meta.getCategoriesJson())
        .put("documentTypes", meta.getDocumentTypesJson(JsonDestination.OUTPUT))
        .toString(2)
    ).build();
  }

  @GET
  @Path("/build-status")
  @Produces(MediaType.TEXT_PLAIN)
  public Response getBuildStatus() {
    return Response.ok(BuildStatus.getLatestBuildStatus()).build();
  }
}
