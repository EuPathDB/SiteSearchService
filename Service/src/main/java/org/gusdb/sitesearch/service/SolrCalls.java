package org.gusdb.sitesearch.service;

import static org.gusdb.fgputil.FormatUtil.urlEncodeUtf8;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.gusdb.sitesearch.service.metadata.DocumentField;
import org.gusdb.sitesearch.service.metadata.Metadata;
import org.gusdb.sitesearch.service.request.Pagination;
import org.gusdb.sitesearch.service.request.SearchRequest;
import org.gusdb.sitesearch.service.solr.Solr;
import org.gusdb.sitesearch.service.solr.SolrResponse;

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

  // hard-coded document fields
  public static final String DOCUMENT_TYPE_FIELD = "document-type";
  public static final String ORGANISM_FIELD = "organism";
  public static final String PROJECT_FIELD = "project";
  public static final String ID_FIELD = "id";

  // template for metadata document requests
  private static final Function<String,String> METADOC_REQUEST = docType ->
    "/select?q=*&fq=" + DOCUMENT_TYPE_FIELD + ":(" + docType + ")&fl=json-blob:[json]&wt=json";

  // two different metadata requests required, defined by document type requested
  private static final String CATAGORIES_METADOC_REQUEST = METADOC_REQUEST.apply("document-categories");
  private static final String FIELDS_METADOC_REQUEST = METADOC_REQUEST.apply("document-fields");

  /**
   * Loads basic metadata (but not facet counts) using two SOLR searches which return:
   * 1. a single categories/documentTypes JSON document, defining doc types and their categories
   * 2. a single documentType fields JSON document, defining fields for each doc type
   * 
   * @return initial metadata object
   */
  public static Metadata initializeMetadata() {
    // initialize metadata object with categories and document data
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

  /**
   * Performs a SOLR search defined by the parameters of the request object and
   * using fields defined by the metadata object
   * 
   * @param request request specified by the service caller
   * @param meta metadata object populated by "static" calls to SOLR
   * @param forOrganismFacets whether this search is specifically made to fetch
   * counts of organisms in result.  If so, organism filter will NOT be applied,
   * and we will request zero documents (an empty page) since it is not needed.
   * Highlighting will also be turned off since it is not needed.
   * @return SOLR search response
   */
  public static SolrResponse getSearchResponse(SearchRequest request, Metadata meta, boolean forOrganismFacets) {
    // don't need any documents in result if only collecting organism facets
    Pagination pagination = forOrganismFacets ? new Pagination(0,0) : request.getPagination();
    // selecting search fields will apply fields filter if present
    String searchFields = formatFieldsForRequest(meta.getSearchFields(request.getFilter()));
    String searchFiltersParam = buildQueryFilterParams(request, !forOrganismFacets);
    String filteredDocsRequest =
        "/select" +                                        // perform a search
        "?q=" + urlEncodeUtf8(request.getSearchText()) +   // search text
        "&qf=" + urlEncodeUtf8(searchFields) +             // fields to search
        "&start=" + pagination.getOffset() +               // first row to return
        "&rows=" + pagination.getNumRecords() +            // number of documents to return
        "&facet=true" +                                    // use facets
        "&facet.field=" + DOCUMENT_TYPE_FIELD +            // declare document-type as facet field
        "&facet.field=" + ORGANISM_FIELD +                 // declare organism as facet field
        "&defType=edismax" +                               // chosen query parser
        (forOrganismFacets ? "" : "&hl=true") +            // turn on highlighting
        (forOrganismFacets ? "" : "&hl.fl=*") +            // highlight matches on all fields
        (forOrganismFacets ? "" : "&hl.method=unified") +  // chosen highlighting method
        searchFiltersParam;                                // filters to apply to search
    return Solr.executeQuery(filteredDocsRequest, true, resp -> {
      return Solr.parseResponse(filteredDocsRequest, resp);
    });
  }

  private static String buildQueryFilterParams(SearchRequest request, boolean includeOrganismFilter) {
    return
      // apply project filter
      request.getRestrictToProject().map(project -> "&fq=" + urlEncodeUtf8("(" + PROJECT_FIELD + ":[* TO *] OR " + PROJECT_FIELD + ":(" + project + "))")).orElse("") +
      // apply docType filter
      request.getFilter().map(filter -> "&fq=" + urlEncodeUtf8(DOCUMENT_TYPE_FIELD + ":(" + filter.getDocType() + ")")).orElse("") +
      // apply organism filter only if asked
      (!includeOrganismFilter ? "" :
        request.getRestrictSearchToOrganisms().map(orgs -> ("&fq=" + urlEncodeUtf8("(" + ORGANISM_FIELD + ":[* TO *] OR " + getOrgFilterCondition(orgs) + ")"))).orElse(""));
  }

  private static String getOrgFilterCondition(List<String> organisms) {
    return organisms.stream(
        ).map(org -> ORGANISM_FIELD + ":(" + org + ")")
        .collect(Collectors.joining(" OR "));
  }

  private static String formatFieldsForRequest(List<DocumentField> fields) {
    return fields.stream()
        .map(field -> field.getName())// + "^" + field.getBoost()) <- FIXME: TURN BACK ON TO SEE HOW TO GET ERROR MESSAGES BACK FROM SOLR
        .collect(Collectors.joining(" "));
  }

}
