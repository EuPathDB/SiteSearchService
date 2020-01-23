package org.gusdb.sitesearch.service;

import static org.gusdb.fgputil.FormatUtil.urlEncodeUtf8;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.gusdb.sitesearch.service.metadata.DocumentField;
import org.gusdb.sitesearch.service.metadata.Metadata;
import org.gusdb.sitesearch.service.search.Pagination;
import org.gusdb.sitesearch.service.search.SearchRequest;
import org.gusdb.sitesearch.service.util.Solr;
import org.gusdb.sitesearch.service.util.SolrResponse;

/**
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

 */
public class SolrCalls {

  public static final String DOCUMENT_TYPE_FIELD = "document-type";
  public static final String ORGANISM_FIELD = "organism";

  private static final Function<String,String> METADOC_REQUEST = docType ->
    "/select?q=*&fq=" + DOCUMENT_TYPE_FIELD + ":(" + docType + ")&fl=json-blob:[json]&wt=json";

  private static final String CATAGORIES_METADOC_REQUEST = METADOC_REQUEST.apply("document-categories");
  private static final String FIELDS_METADOC_REQUEST = METADOC_REQUEST.apply("document-fields");

  public static Metadata initializeMetadata() {
    // initialize categories metadata object with categories document data
    Metadata meta = Solr.executeQuery(CATAGORIES_METADOC_REQUEST, true, response -> {
      SolrResponse result = Solr.parseResponse(CATAGORIES_METADOC_REQUEST, response);
      return new Metadata(result);
    });
    // supplement doc types with the fields in those doc types
    return Solr.executeQuery(FIELDS_METADOC_REQUEST, true, response -> {
      SolrResponse result = Solr.parseResponse(FIELDS_METADOC_REQUEST, response);
      return meta.addFieldData(result);
    });
  }

  public static SolrResponse getSearchResponse(SearchRequest request, Metadata meta, boolean forOrganismFacets) {
    Pagination pagination = forOrganismFacets ? new Pagination(0,0) : request.getPagination();
    String filteredDocsRequest =
        "/select" +
        "?q=" + urlEncodeUtf8(request.getSearchText()) +
        "&qf=" + urlEncodeUtf8(formatFieldsForRequest(meta.getFields(request.getFilter()))) +
        "&start=" + pagination.getOffset() +
        "&rows=" + pagination.getNumRecords() +
        "&facet=true" +
        "&facet.field=" + DOCUMENT_TYPE_FIELD +
        "&facet.field=" + ORGANISM_FIELD +
        "&defType=edismax" + // query parser
        buildQueryFilterParam(request, !forOrganismFacets);
    return Solr.executeQuery(filteredDocsRequest, true, resp -> {
      return Solr.parseResponse(filteredDocsRequest, resp);
    });
  }

  private static String buildQueryFilterParam(SearchRequest request, boolean includeOrganismFilter) {
    return ""; //request.getRestrictToProject().map(project -> "&fq=(-project:[* TO *] OR project:" + project + ")").orElse("");
    //if (request.getRestrictSearchToOrganisms().isPresent()) {
      //String filterString = request.getRestrictSearchToOrganisms().get().stream()
      //allDocsRequest += "&fq=organism:" +

          //examples 
          // (-PrivacyLevel:[* TO *] OR PrivacyLevel:2
          // -(-price:[300 TO 400] AND price:[* TO *])
    //}
  }

  private static String formatFieldsForRequest(List<DocumentField> fields) {
    return fields.stream()
        .map(field -> field.getName())// + "^" + field.getBoost()) <- TURN BACK ON TO SEE HOW TO GET ERROR MESSAGES BACK FROM SOLR
        .collect(Collectors.joining(" "));
  }

}
