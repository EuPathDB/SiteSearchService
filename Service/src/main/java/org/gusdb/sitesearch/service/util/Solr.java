package org.gusdb.sitesearch.service.util;

import static org.gusdb.fgputil.json.JsonIterators.arrayStream;
import static org.gusdb.sitesearch.service.server.Context.SOLR_URL;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.FormatUtil;
import org.gusdb.fgputil.IoUtil;
import org.gusdb.fgputil.functional.FunctionalInterfaces.FunctionWithException;
import org.gusdb.fgputil.server.RESTServer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Solr {

  private static final Logger LOG = Logger.getLogger(Solr.class);

  public static String getSolrUrl() {
    return (String)RESTServer.getApplicationContext().get(SOLR_URL);
  }

  public static <T> T executeQuery(String urlSubpath, boolean closeResponseOnExit, FunctionWithException<Response, T> handler) {
    Response response = null;
    try {
      Client client = ClientBuilder.newClient();
      String finalUrl = getSolrUrl() + urlSubpath;
      LOG.info("Querying SOLR with: " + finalUrl);
      WebTarget webTarget = client.target(finalUrl);
      Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON);
      response = invocationBuilder.get();
      if (response.getStatusInfo().getFamily().equals(Family.SUCCESSFUL)) {
        try {
          return handler.apply(response);
        }
        catch(Exception e) {
          throw handleError("Unable to process SOLR response", urlSubpath, e);
        }
      }
      else {
        throw handleError("SOLR request failed with code: " + response.getStatus(), urlSubpath, null);
      }
    }
    catch (JSONException e) {
      throw handleError("SOLR response not valid JSON", urlSubpath, e);
    }
    finally {
      if (response != null && closeResponseOnExit) response.close();
    }
  }

  private static SiteSearchRuntimeException handleError(String message, String urlSubpath, Exception e) {
    String runtimeMsg = "Error: " + message + FormatUtil.NL + "SOLR_REQUEST_URL: " + urlSubpath;
    return e == null ? new SiteSearchRuntimeException(runtimeMsg) : new SiteSearchRuntimeException(runtimeMsg, e);
  }

  /*
   * "facet_counts": {
      "facet_intervals": {},
      "facet_queries": {},
      "facet_fields": {"document-type": [
        "batch-meta",
        0,
        "document-categories",
        0,
        "pathway",
        0,
        "organism",
        0,
        "genomic-sequence",
        0,
        "est",
        0,
        "gene",
        0,
        "dataset",
        0,
        "compound",
        0,
        "search",
        0
      ]},
      "facet_heatmaps": {},
      "facet_ranges": {}
    }
   */
  public static SolrResponse parseResponse(String requestSubpath, Response response) throws IOException {
    String data = IoUtil.readAllChars(new InputStreamReader((InputStream)response.getEntity()));
    JSONObject responseBody = new JSONObject(data);
    int responseStatus = responseBody.getJSONObject("responseHeader").getInt("status");
    if (responseStatus != 0) {
      throw handleError("SOLR response had non-zero embedded status (" + responseStatus + ")", requestSubpath, null);
    }
    JSONObject responseJson = responseBody.getJSONObject("response");
    List<JSONObject> documents = arrayStream(responseJson.getJSONArray("docs"))
        .map(val -> val.getJSONObject())
        .collect(Collectors.toList());
    SolrResponse respObj = new SolrResponse(documents);
    if (responseBody.has("facet_counts")) {
      LOG.info("Facet counts found with value: " + responseBody.getJSONObject("facet_counts").toString());
      // for now we only request facet counts on document-type
      JSONArray facets = responseBody.getJSONObject("facet_counts").getJSONObject("facet_fields").getJSONArray("document-type");
      Map<String,Integer> facetCounts = new HashMap<>();
      for (int i = 0; i < facets.length(); i+=2) {
        facetCounts.put(facets.getString(i), facets.getInt(i+1));
      }
      respObj.setFacetCounts(facetCounts);
    }
    return respObj;
  }

  // Leaving here as a reference in case we go with SOLRJ in a future iteration; for now sticking with JSON
  /*
  @SuppressWarnings("unused")
  private Response runSearchWithSolrj() {
    try {
      SolrClient client = new HttpSolrClient.Builder(Solr.getSolrUrl()).build();
      SolrQuery query = (SolrQuery) new SolrQuery(IDENTITY_QUERY)
          .setFacet(true)
          .addFacetField(DOCUMENT_TYPE_FIELD)
          .setFields(ID_FIELD, DOCUMENT_TYPE_FIELD)
          .setMoreLikeThisQF(QUERY_FIELDS)
          .setRows(10)
          .addFilterQuery("document-type:(gene)")
          .set("defType", "edismax");
      QueryResponse response = client.query(query);
      // TODO parse response
      //response.getFacetFields().get(0).getName() or .getValueCount()
      return Response.ok(response.toString()).build();
    }
    catch (Exception e) {
      throw handleError("solrj processing failed", "<unknown>", e);
    }
  }*/
}
