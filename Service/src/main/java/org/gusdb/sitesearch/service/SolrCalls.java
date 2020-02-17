package org.gusdb.sitesearch.service;

import static org.gusdb.fgputil.FormatUtil.NL;
import static org.gusdb.fgputil.FormatUtil.TAB;
import static org.gusdb.fgputil.FormatUtil.urlEncodeUtf8;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.gusdb.fgputil.solr.Solr;
import org.gusdb.fgputil.solr.SolrResponse;
import org.gusdb.sitesearch.service.metadata.DocumentField;
import org.gusdb.sitesearch.service.metadata.Metadata;
import org.gusdb.sitesearch.service.request.Pagination;
import org.gusdb.sitesearch.service.request.SearchRequest;
import org.json.JSONObject;

public class SolrCalls {

  // hard-coded document fields
  public static final String DOCUMENT_TYPE_FIELD = "document-type";
  public static final String ORGANISM_FIELD = "organism";
  public static final String PROJECT_FIELD = "project";
  public static final String ID_FIELD = "id";
  public static final String PRIMARY_KEY_FIELD = "primaryKey";
  public static final String SCORE_FIELD = "score";
  public static final String WDK_PRIMARY_KEY_FIELD = "wdkPrimaryKeyString";
  public static final String HYPERLINK_NAME_FIELD = "hyperlinkName";
  public static final String JSON_BLOB_FIELD = "json-blob";

  // hard-coded doc types
  public static final String CATEGORIES_META_DOCTYPE = "document-categories";
  public static final String FIELDS_META_DOCTYPE = "document-fields";
  public static final String BATCH_META_DOCTYPE = "batch-meta";

  // tuning constants
  private static final int PRIMARY_KEY_BOOST_VALUE = 100;
  private static final int FETCH_SIZE_FROM_SOLR = 10000;

  // search constants
  private static final String SORTING_FIELDS = SCORE_FIELD + " desc, " + ID_FIELD + " asc";

  // template for metadata document requests
  private static final Function<String,String> METADOC_REQUEST = docType ->
    "/select?q=*&fq=" + DOCUMENT_TYPE_FIELD + ":(" + docType + ")&fl=" + JSON_BLOB_FIELD + ":[json]&wt=json";

  // two different metadata requests required, defined by document type requested
  private static final String CATAGORIES_METADOC_REQUEST = METADOC_REQUEST.apply(CATEGORIES_META_DOCTYPE);
  private static final String FIELDS_METADOC_REQUEST = METADOC_REQUEST.apply(FIELDS_META_DOCTYPE);

  /**
   * Loads basic metadata (but not facet counts) using two SOLR searches which return:
   * 1. a single categories/documentTypes JSON document, defining doc types and their categories
   * 2. a single documentType fields JSON document, defining fields for each doc type
   * 
   * @return initial metadata object
   */
  public static Metadata initializeMetadata(Solr solr) {
    // initialize metadata object with categories and document data
    Metadata meta = solr.executeQuery(CATAGORIES_METADOC_REQUEST, true, response -> {
      SolrResponse result = Solr.parseResponse(CATAGORIES_METADOC_REQUEST, response);
      return new Metadata(result);
    });
    // supplement doc types with the fields in those doc types
    return solr.executeQuery(FIELDS_METADOC_REQUEST, true, response -> {
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
  public static SolrResponse getSearchResponse(Solr solr, SearchRequest request, Metadata meta,
      boolean omitResults, boolean applyOrganismFilter, FieldsMode fieldsMode) {

    // don't need any documents in result if only collecting organism facets
    Pagination pagination = omitResults ? new Pagination(0,0) :
      request.getPagination().get(); // should always be present for this call; bug if not

    // select search fields that will be applied to this search
    List<DocumentField> searchFields = meta.getSearchFields(
        request.getDocTypeFilter().map(filter -> filter.getDocType()),
        (fieldsMode.applyFieldsFilter() &&
         request.getDocTypeFilter().isPresent()) ?
            request.getDocTypeFilter().get().getFoundOnlyInFields() :
            Optional.empty(),
        request.getRestrictToProject());

    String searchFiltersParam = buildQueryFilterParams(request, applyOrganismFilter);
    String fieldQueryFacets = buildFieldQueryFacets(request.getSearchText(), searchFields, fieldsMode);
    
    String filteredDocsRequest =
        "/select" +                                                    // perform a search
        "?q=" + urlEncodeUtf8(request.getSearchText()) +               // search text
        "&qf=" + urlEncodeUtf8(formatFieldsForRequest(searchFields)) + // fields to search
        "&start=" + pagination.getOffset() +                           // first row to return
        "&rows=" + pagination.getNumRecords() +                        // number of documents to return
        "&facet=true" +                                                // use facets
        "&facet.field=" + DOCUMENT_TYPE_FIELD +                        // declare document-type as facet field
        "&facet.field=" + ORGANISM_FIELD +                             // declare organism as facet field
        fieldQueryFacets +                                             // special field facets
        "&defType=edismax" +                                           // chosen query parser
        "&fl=" + urlEncodeUtf8("* " + SCORE_FIELD) +                   // fields to return
        "&sort=" + urlEncodeUtf8(SORTING_FIELDS) +                     // how to sort results
        (omitResults ? "" : "&hl=true") +                              // turn on highlighting
        (omitResults ? "" : "&hl.fl=*") +                              // highlight matches on all fields
        (omitResults ? "" : "&hl.method=unified") +                    // chosen highlighting method
        searchFiltersParam;                                            // filters to apply to search
    return solr.executeQuery(filteredDocsRequest, true, resp -> {
      return Solr.parseResponse(filteredDocsRequest, resp);
    });
  }

  private static String buildFieldQueryFacets(String searchText, List<DocumentField> searchFields, FieldsMode fieldsMode) {
    return !fieldsMode.requestFieldQueryFacets() || searchFields.isEmpty() ? "" : searchFields.stream()
        .map(field -> "&facet.query=" + urlEncodeUtf8(field.getName() + ":(" + searchText + ")"))
        .collect(Collectors.joining());
  }

  private static String buildQueryFilterParams(SearchRequest request, boolean applyOrganismFilter) {
    return
      // apply project filter
      // example: -(project:[* TO *] AND -project:(PlasmoDB))
      request.getRestrictToProject().map(project ->
        "&fq=" + urlEncodeUtf8("-(" + PROJECT_FIELD + ":[* TO *] AND -" + PROJECT_FIELD + ":(" + project + "))")
      ).orElse("") +

      // apply docType filter
      // example: document-type:(gene)
      request.getDocTypeFilter().map(filter ->
         "&fq=" + urlEncodeUtf8(DOCUMENT_TYPE_FIELD + ":(" + filter.getDocType() + ")")
      ).orElse("") +

      // apply organism filter only if asked
      // example: -(organism:[* TO *] AND -organism:("Plasmodium falciparum 3D7" OR "Plasmodium falciparum 7G8"))
      (!applyOrganismFilter ? "" :
        request.getRestrictSearchToOrganisms().map(orgs ->
          "&fq=" + urlEncodeUtf8("-(" + ORGANISM_FIELD + ":[* TO *] AND -" + ORGANISM_FIELD + ":(" + getOrgFilterCondition(orgs) + "))")
        ).orElse(""));
  }

  private static String getOrgFilterCondition(List<String> organisms) {
    return organisms.stream(
        ).map(org -> "\"" + org + "\"")
        .collect(Collectors.joining(" OR "));
  }

  private static String formatFieldsForRequest(List<DocumentField> fields) {
    return PRIMARY_KEY_FIELD + "^" + PRIMARY_KEY_BOOST_VALUE + (fields.isEmpty() ? "" : fields.stream()
        .map(field -> " " + field.getName() + (field.getBoost() == 1 ? "" : ("^" + String.format("%.2f", field.getBoost()))))
        .collect(Collectors.joining("")));
  }

  public static void writeSearchResponse(Solr solr, SearchRequest request, Metadata meta, OutputStream output) throws IOException {
    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output));
    String nextCursorMark = "*";
    String lastCursorMark = null;
    String searchFields = formatFieldsForRequest(meta.getSearchFields(
        request.getDocTypeFilter().map(f -> f.getDocType()),
        request.getDocTypeFilter().flatMap(f -> f.getFoundOnlyInFields()),
        request.getRestrictToProject()));
    String searchFiltersParam = buildQueryFilterParams(request, true);
    String fieldsToReturn = PRIMARY_KEY_FIELD + " " + SCORE_FIELD;
    String staticPortionOfRequest =
        "/select" +                                        // perform a search
        "?q=" + urlEncodeUtf8(request.getSearchText()) +   // search text
        "&qf=" + urlEncodeUtf8(searchFields) +             // fields to search
        "&rows=" + FETCH_SIZE_FROM_SOLR +                  // number of documents to return
        "&defType=edismax" +                               // chosen query parser
        "&sort=" + urlEncodeUtf8(SORTING_FIELDS) +         // how to sort results
        "&fl=" + urlEncodeUtf8(fieldsToReturn) +           // fields to return
        "&echoParams=none" +                               // do not echo param info
        searchFiltersParam;                                // filters to apply to search
    while (!nextCursorMark.equals(lastCursorMark)) {
      String requestUrl = staticPortionOfRequest + "&cursorMark=" + urlEncodeUtf8(nextCursorMark);
      SolrResponse response = solr.executeQuery(requestUrl, true, resp -> {
        return Solr.parseResponse(requestUrl, resp);
      });
      for (JSONObject document : response.getDocuments()) {
        writer.write(document.getJSONArray(PRIMARY_KEY_FIELD).toString());
        writer.write(TAB);
        writer.write(String.valueOf(document.getDouble(SCORE_FIELD)));
        writer.write(NL);
      }
      lastCursorMark = nextCursorMark;
      nextCursorMark = response.getNextCursorMark().get();
    }
    writer.flush();
  }

  @SuppressWarnings("unused")
  public static Map<String,Integer> getFieldsHighlightingCounts(
      Solr solr, SearchRequest searchRequest, Metadata meta) {
    // TODO: implement (once performance testing is done on SOLR)
    return new HashMap<>();
  }

  public static class FieldsMode {

    public static final FieldsMode NORMAL = new FieldsMode(false);
    public static final FieldsMode FOR_FACETS = new FieldsMode(true);

    private final boolean _isForFieldsFacets;

    private FieldsMode(boolean isForFieldsFacets) {
      _isForFieldsFacets = isForFieldsFacets;
    }

    public boolean applyFieldsFilter() {
      return !_isForFieldsFacets;
    }

    public boolean requestFieldQueryFacets() {
      return _isForFieldsFacets;
    }
  }
}
