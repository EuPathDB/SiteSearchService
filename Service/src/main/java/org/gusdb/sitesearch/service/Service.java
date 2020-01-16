package org.gusdb.sitesearch.service;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.FormatUtil;
import org.gusdb.sitesearch.service.metadata.CategoriesMetadata;
import org.gusdb.sitesearch.service.util.Solr;
import org.json.JSONObject;

/*
 * Playing with Solr queries

query to see what batches are loaded

curl -s "https://solr.local.apidb.org:8443/solr/site_search/select?q=*&fq=document-type:(batch-meta)"

queries for left panel counts:

curl -s "https://solr.local.apidb.org:8443/solr/site_search/select?q=kinase&fl=document-type&rows=1000000&wt=csv"
curl -s https://solr.local.apidb.org:8443/solr/test_core/select?q=identity&rows=100000&defType=edismax&fl=document-type&wt=csv"

queries for field counts

curl -s "https://solr.local.apidb.org:8443/solr/site_search/select?q=identity&qf=TABLE__gene_PdbSimilarities%20TABLE__gene_BlastP&rows=1&hl=true&hl.fl=*&hl.method=unified&defType=edismax&fl=document-type,id"

queries to performance test highlighting

time curl -s "https://solr.local.apidb.org:8443/solr/site_search/select?q=identity&qf=TABLE__gene_PdbSimilarities%20TABLE__gene_BlastP&rows=100000&hl=false&hl.fl=*&hl.method=unified&defType=edismax&fl=document-type,id" | grep document-type | wc
time curl -s "https://solr.local.apidb.org:8443/solr/site_search/select?q=identity&qf=TABLE__gene_PdbSimilarities%20TABLE__gene_BlastP&rows=100000&hl=true&hl.fl=*&hl.method=unified&defType=edismax&fl=document-type,id" | grep document-type | wc

query for json blob

curl -s "https://solr.local.apidb.org:8443/solr/site_search/select?q=*&fq=document-type:(document-categories)&fl=json-blob:[json]&wt=json"

TODO: remove SOLRJ from poms (this and base-pom) if decided finally not to use
 */

@Path("/")
public class Service {

  private static final Logger LOG = Logger.getLogger(Service.class);

  private static final String CATAGORIES_METADOC_REQUEST = "/select?q=*&fq=document-type:(document-categories)&fl=json-blob:[json]&wt=json";
  private static final String DOCUMENT_TYPE_FIELD = "document-type";
  private static final String ID_FIELD = "id";

  // dummy values for testing
  private static final String IDENTITY_QUERY = "identity";
  private static final String QUERY_FIELDS = "MULTITEXT__PdbSimilarities MULTITEXT__BlastP";

  private static final String TEST_REQUEST =
      "/select" +
      "?q=" + IDENTITY_QUERY + // search term
      "&qf=" + FormatUtil.urlEncodeUtf8(QUERY_FIELDS) +  // which fields to query (space or comma delimited)
      "&rows=10" +
      "&facet=true" +
      "&facet.field=" + DOCUMENT_TYPE_FIELD +
      "&defType=edismax" +     // query parser
      "&fl=document-type,id"; // fields we want back

  /**
inputSchema = {
  searchText: string,
  restrictToOrganisms: string[],
  pagination: {
    offset: integer,
    numRecords: integer
  }
}
   */
  @POST
  @Path("/search")
  @Produces(MediaType.APPLICATION_JSON)
  public Response runUnfilteredSearch(String body) {
    return Response.ok().build();
  }

  /**
inputSchema = {
  searchText: string,
  restrictToOrganisms: string[],
  pagination: {
    offset: integer,
    numRecords: integer
  },
  filter: {
    documentType: string,
    foundInFields?: string[]
  }
}
   */
  @POST
  @Path("/filtered-search")
  @Produces(MediaType.APPLICATION_JSON)
  public Response runFilteredSearch(String body) {
    return Response.ok().build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response runSiteSearch() {
    CategoriesMetadata meta = loadCategories();
    JSONObject results = Solr.executeQuery(TEST_REQUEST, true, response -> {
      return Solr.readToJson(TEST_REQUEST, response);
    });
    return Response.ok(new JSONObject()
        .put("metadata", meta)
        .put("results", results)
        .toString(2)).build();
  }

  @GET
  @Path("/categories")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getCategoriesJson() {
    return Response.ok(loadCategories().toString()).build();
  }

  private CategoriesMetadata loadCategories() {
    return Solr.executeQuery(CATAGORIES_METADOC_REQUEST, true, response -> {
      return new CategoriesMetadata(Solr.readToJson(CATAGORIES_METADOC_REQUEST, response));
    });
  }

}
