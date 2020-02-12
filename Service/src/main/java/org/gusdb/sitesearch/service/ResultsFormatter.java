package org.gusdb.sitesearch.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.solr.Solr.Highlighting;
import org.gusdb.fgputil.solr.SolrResponse;
import org.gusdb.sitesearch.service.exception.SiteSearchRuntimeException;
import org.gusdb.sitesearch.service.metadata.DocumentField;
import org.gusdb.sitesearch.service.metadata.DocumentType;
import org.gusdb.sitesearch.service.metadata.Metadata;
import org.json.JSONArray;
import org.json.JSONObject;

public class ResultsFormatter {

  private static final Logger LOG = Logger.getLogger(ResultsFormatter.class);

  public static JSONObject formatResults(Metadata meta, SolrResponse searchResults, Optional<String> restrictToProject) {
    return new JSONObject()
      .put("categories", meta.getCategoriesJson())
      .put("documentTypes", meta.getDocumentTypesJson(restrictToProject))
      .put("organismCounts", meta.getOrganismFacetCounts())
      .put("fieldCounts", meta.getFieldCounts())
      .put("searchResults", new JSONObject()
        .put("totalCount", searchResults.getTotalCount())
        .put("documents", getDocumentsJson(meta, searchResults.getDocuments(), searchResults.getHighlighting(), restrictToProject)));
  }

  private static JSONArray getDocumentsJson(Metadata meta, List<JSONObject> documents, Highlighting highlighting, Optional<String> restrictToProject) {
    return new JSONArray(documents.stream()
      // filter out batch-meta documents
      .filter(documentJson -> !(SolrCalls.BATCH_META_DOCTYPE.equals(documentJson.getString(SolrCalls.DOCUMENT_TYPE_FIELD))))
      // format raw document JSON to summary JSON
      .map(documentJson -> {
        //LOG.debug("Processing document: " + documentJson.toString(2));
        DocumentType docType = meta.getDocumentType(documentJson.getString(SolrCalls.DOCUMENT_TYPE_FIELD))
          .orElseThrow(() -> new SiteSearchRuntimeException("Unknown document type returned in document: " + documentJson.toString(2))); 
        JSONArray primaryKey = documentJson.getJSONArray(SolrCalls.PRIMARY_KEY_FIELD);
        JSONObject json = new JSONObject()
          .put("documentType", docType.getId())
          .put("primaryKey", primaryKey)
          .put("organism", documentJson.optString(SolrCalls.ORGANISM_FIELD, null))
          .put("score", documentJson.getDouble(SolrCalls.SCORE_FIELD))
          .put("wdkPrimaryKeyString", documentJson.optString(SolrCalls.WDK_PRIMARY_KEY_FIELD, null))
          .put("hyperlinkName", documentJson.optString(SolrCalls.HYPERLINK_NAME_FIELD, null))
          .put("foundInFields", highlighting.get(documentJson.getString(SolrCalls.ID_FIELD)));
        JSONObject summaryFields = new JSONObject();
        String value;
        JSONArray values;
        for (DocumentField field : docType.getFields(restrictToProject)) {
          if (field.isSummary()) {
            if (field.isMultiText()) {
              if ((values = documentJson.optJSONArray(field.getName())) != null) {
                summaryFields.put(field.getName(), values);
              }
              else {
                LOG.warn("Document of type '" + docType.getId() + "' with PK '" + primaryKey + "' does not contain multi-text summary field '" + field.getName());
              }
            }
            else {
              if ((value = documentJson.optString(field.getName(), null)) != null) {
                summaryFields.put(field.getName(), value);
              }
              else {
                LOG.warn("Document of type '" + docType.getId() + "' with PK '" + primaryKey + "' does not contain summary field '" + field.getName());
              }
            }
          }
        }
        return json.put("summaryFieldData", summaryFields);
      })
      .collect(Collectors.toList()));
  }
}
