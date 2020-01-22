package org.gusdb.sitesearch.service;

import static org.gusdb.fgputil.FormatUtil.urlEncodeUtf8;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.FormatUtil;
import org.gusdb.fgputil.FormatUtil.Style;
import org.gusdb.fgputil.runtime.GusHome;
import org.gusdb.sitesearch.service.metadata.CategoriesMetadata;
import org.gusdb.sitesearch.service.metadata.DocumentField;
import org.gusdb.sitesearch.service.metadata.DocumentType;
import org.gusdb.sitesearch.service.metadata.JsonDestination;
import org.gusdb.sitesearch.service.search.SearchRequest;
import org.gusdb.sitesearch.service.util.SiteSearchRuntimeException;
import org.gusdb.sitesearch.service.util.Solr;
import org.gusdb.sitesearch.service.util.SolrResponse;
import org.json.JSONArray;
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

  private static final Function<String,String> METADOC_REQUEST = docType ->
    "/select?q=*&fq=document-type:(" + docType + ")&fl=json-blob:[json]&wt=json";

  private static final String CATAGORIES_METADOC_REQUEST = METADOC_REQUEST.apply("document-categories");
  private static final String FIELDS_METADOC_REQUEST = METADOC_REQUEST.apply("document-fields");

  private static final String DOCUMENT_TYPE_FIELD = "document-type";
  private static final String ORGANISM_FIELD = "organism";
  private static final String ID_FIELD = "id";

  private static final String TEST_REQUEST =
      "/select" +
      "?q=exon" + // search term
      "&qf=" + FormatUtil.urlEncodeUtf8("MULTITEXT__PdbSimilarities MULTITEXT__BlastP") +  // which fields to query (space or comma delimited)

      "&rows=10" +
      "&facet=true" +
      "&facet.field=" + DOCUMENT_TYPE_FIELD +
      "&defType=edismax" +     // query parser
      "&fl=document-type,id"; // fields we want back

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response runSearch(
      @QueryParam("searchText") String searchText,
      @QueryParam("offset") int offset,
      @QueryParam("numRecords") int numRecords) {
    return handleSearchRequest(new SearchRequest(searchText, offset, numRecords));
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  public Response runSearch(String body) {
    return handleSearchRequest(new SearchRequest(new JSONObject(body)));
  }

  private Response handleSearchRequest(SearchRequest request) {
    CategoriesMetadata meta = loadCategories();
    // first get all document-types for facet information (will always do regardless of filter)
    String allDocsRequest =
        "/select" +
        "?q=" + urlEncodeUtf8(request.getSearchText()) +
        "&qf=" + urlEncodeUtf8(formatFieldsForRequest(meta.getFields(Optional.empty()))) +
        "&start=" + request.getPagination().getOffset() +
        "&rows=" + request.getPagination().getNumRecords() +
        "&facet=true" +
        "&facet.field=" + DOCUMENT_TYPE_FIELD +
        "&facet.field=" + ORGANISM_FIELD +
        //"&fl=document-type,id" + // temporarily only get back two fields
        "&defType=edismax"; // query parser
    SolrResponse response = Solr.executeQuery(allDocsRequest, true, resp -> {
      return Solr.parseResponse(allDocsRequest, resp);
    });
    if (response.getFacetCounts().isPresent()) {
      meta.applyFacetCounts(response.getFacetCounts().get());
    }
    JSONObject resultJson = new JSONObject()
      .put("categories", meta.toJson())
      .put("documentTypes", meta.getDocumentTypesJson(JsonDestination.OUTPUT))
      .put("searchResults", getDocumentsJson(meta, response.getDocuments()));
    return Response.ok(resultJson.toString(2)).build();
  }

  private JSONArray getDocumentsJson(CategoriesMetadata meta, List<JSONObject> documents) {
    return new JSONArray(documents.stream().map(documentJson -> {
      LOG.info("Processing document: " + documentJson.toString(2));
      DocumentType docType = meta.getDocumentType(documentJson.getString("document-type"))
        .orElseThrow(() -> new SiteSearchRuntimeException("Unknown document type returned in document: " + documentJson.toString(2))); 
      String primaryKey = documentJson.optString("primaryKey", "<unknown!>");
      JSONObject json = new JSONObject()
        .put("documentType", docType.getId())
        .put("primaryKey", primaryKey);
      String value;
      for (DocumentField field : docType.getFields()) {
        if (field.isSummary()) {
          if ((value = documentJson.optString(field.getName(), null)) != null) {
            json.put(field.getName(), value);
          }
          else {
            LOG.warn("Document of type '" + docType.getId() + "' with PK '" + primaryKey + "' does not contain summary field '" + field.getName());
          }
        }
      }
      return documentJson;
    })
    .collect(Collectors.toList()));
  }

  private static String formatFieldsForRequest(List<DocumentField> fields) {
    return fields.stream()
        .map(field -> field.getName())// + "^" + field.getBoost()) <- TURN BACK ON TO SEE HOW TO GET ERROR MESSAGES BACK FROM SOLR
        .collect(Collectors.joining(" "));
  }

  @GET
  @Path("/categories-metadata")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getCategoriesJson() {
    CategoriesMetadata meta = loadCategories();
    return Response.ok(
      new JSONObject()
        .put("categories", meta.toJson())
        .put("documentTypes", meta.getDocumentTypesJson(JsonDestination.OUTPUT))
        .toString(2)
    ).build();
  }

  private CategoriesMetadata loadCategories() {
    // initialize categories metadata object with categories document data
    CategoriesMetadata meta = Solr.executeQuery(CATAGORIES_METADOC_REQUEST, true, response -> {
      SolrResponse result = Solr.parseResponse(CATAGORIES_METADOC_REQUEST, response);
      return new CategoriesMetadata(result);
    });
    // supplement doc types with the fields in those doc types
    return Solr.executeQuery(FIELDS_METADOC_REQUEST, true, response -> {
      SolrResponse result = Solr.parseResponse(FIELDS_METADOC_REQUEST, response);
      return meta.addFieldData(result);
    });
  }

  @GET
  @Path("/build-status")
  @Produces(MediaType.TEXT_PLAIN)
  public Response getBuildStatus() {
    String statusFile = Paths.get(GusHome.getGusHome(), ".buildlog", "git_status").toAbsolutePath().toString();
    List<Map<String,String>> records = new ArrayList<>();
    try (BufferedReader br = new BufferedReader(new FileReader(statusFile))) {
      Map<String,String> buildRecord = new LinkedHashMap<>();
      while (br.ready()) {
        String line = br.readLine().trim();
        if (line.isEmpty()) {
          if (!buildRecord.isEmpty()) {
            records.add(buildRecord);
            buildRecord = new LinkedHashMap<>();
          }
        }
        else {
          String[] tokens = line.split("\\s+", 2);
          buildRecord.put(tokens[0], tokens.length > 1 ? tokens[1] : "");
        }
      }
      if (!buildRecord.isEmpty()) {
        records.add(buildRecord);
      }
    }
    catch (IOException e) {
      throw new SiteSearchRuntimeException("Could not query build status", e);
    }
    Map<String,Map<String,String>> lastBuildRecords = new HashMap<>();
    for (Map<String,String> record : records) {
      lastBuildRecords.put(record.get("Project:")+"."+record.get("Component:"), record);
    }
    List<String> sortedKeys = new ArrayList<>(lastBuildRecords.keySet());
    Collections.sort(sortedKeys);
    String result = sortedKeys.stream()
      .map(key -> FormatUtil.prettyPrint(lastBuildRecords.get(key), Style.MULTI_LINE))
      .collect(Collectors.joining("\n"));
    return Response.ok(result).build();
  }
}
