package org.gusdb.sitesearch.service.metadata;

import static org.gusdb.fgputil.functional.Functions.getMapFromList;
import static org.gusdb.fgputil.functional.Functions.getMapFromValues;
import static org.gusdb.fgputil.functional.Functions.reduce;
import static org.gusdb.fgputil.json.JsonIterators.arrayIterable;
import static org.gusdb.fgputil.json.JsonIterators.arrayStream;
import static org.gusdb.sitesearch.service.SolrCalls.CATEGORIES_META_DOCTYPE;
import static org.gusdb.sitesearch.service.SolrCalls.DOCUMENT_TYPE_FIELD;
import static org.gusdb.sitesearch.service.SolrCalls.FIELDS_META_DOCTYPE;
import static org.gusdb.sitesearch.service.SolrCalls.JSON_BLOB_FIELD;
import static org.gusdb.sitesearch.service.SolrCalls.ORGANISM_FIELD;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.FormatUtil;
import org.gusdb.fgputil.MapBuilder;
import org.gusdb.fgputil.Tuples.TwoTuple;
import org.gusdb.fgputil.solr.Solr.FacetQueryResults;
import org.gusdb.fgputil.solr.SolrResponse;
import org.gusdb.sitesearch.service.exception.InvalidRequestException;
import org.gusdb.sitesearch.service.exception.SiteSearchRuntimeException;
import org.gusdb.sitesearch.service.request.DocTypeFilter;
import org.gusdb.sitesearch.service.request.SearchRequest;
import org.json.JSONArray;
import org.json.JSONObject;

public class Metadata {

  private static final Logger LOG = Logger.getLogger(Metadata.class);

  private final List<Category> _categories;
  private final Map<String,DocumentType> _docTypes;
  private Map<String,Integer> _organismFacetCounts;
  private Map<String,Integer> _fieldFacetCounts;

  public Metadata(SolrResponse result) {
    JSONObject document = getSingular(result.getDocuments(), CATEGORIES_META_DOCTYPE);
    _categories = arrayStream(document.getJSONArray(JSON_BLOB_FIELD))
      .map(jsonType -> jsonType.getJSONObject())
      .map(catObj -> new Category(catObj.getString("name"))
        .addDocumentTypes(arrayStream(catObj.getJSONArray("documentTypes"))
          .map(jsonType -> jsonType.getJSONObject())
          .map(docTypeObj -> new DocumentType(
              docTypeObj.getString("id"),
              docTypeObj.getString("displayName"),
              docTypeObj.getString("displayNamePlural"),
              docTypeObj.getBoolean("hasOrganismField"),
              docTypeObj.optDouble("boost", 1),
              docTypeObj.optString("wdkSearchUrlName", null)))
          .collect(Collectors.toList())))
      .collect(Collectors.toList());
    _docTypes = reduce(_categories,
      (map, cat) -> map.putAll(getMapFromValues(cat, docType -> docType.getId())),
      new MapBuilder<String,DocumentType>()).toMap();
  }

  private static JSONObject getSingular(List<JSONObject> documents, String docType) {
    if (documents.size() != 1) {
      String message = documents.size() == 0 ? "No" : "More than one (" + documents.size() + ")";
      throw new SiteSearchRuntimeException(message + " SOLR documents found with type '" + docType + "'");
    }
    return documents.get(0);
  }

  public Metadata addFieldData(SolrResponse result) {
    JSONObject document = getSingular(result.getDocuments(), FIELDS_META_DOCTYPE);

    // put fields data in a map for easy access
    Map<String,List<DocumentField>> fieldMap = getMapFromList(
      arrayIterable(document.getJSONArray(JSON_BLOB_FIELD)), val -> {
        JSONObject obj = val.getJSONObject();
        return new TwoTuple<String,List<DocumentField>>(
          obj.getString(DOCUMENT_TYPE_FIELD),
          arrayStream(obj.getJSONArray("fields"))
            .map(field -> new DocumentField(field.getJSONObject()))
            .collect(Collectors.toList()));
        }
      );

    // for each type in categories, add fields
    for (DocumentType docType : _docTypes.values()) {
      if (fieldMap.containsKey(docType.getId())) {
        docType.addFields(fieldMap.get(docType.getId()));
      }
      else {
        LOG.warn("Categories metadata contains " + DOCUMENT_TYPE_FIELD + " '" + docType.getId() +
            "' but fields metadata does not.  This means no records of that " +
            "type will ever be found since its fields cannot be specified.");
      }
    }

    // warn if fields contains doc types that categories does not
    Set<String> knownDocTypes = _docTypes.keySet();
    for (String fieldDocType : fieldMap.keySet()) {
      if (!knownDocTypes.contains(fieldDocType)) {
        LOG.warn("Fields metadata contains " + DOCUMENT_TYPE_FIELD + " '" + fieldDocType +
            "' but categories metadata does not.  This means no records of that " +
            "type will ever be found; the type is not used.");
      }
    }

    return this;
  }

  public void applyDocTypeFacetCounts(Map<String,Map<String, Integer>> allFacets) {
    Map<String,Integer> map = getFieldFacets(allFacets, DOCUMENT_TYPE_FIELD);
    for (DocumentType docType : _docTypes.values()) {
      docType.setCount(map.containsKey(docType.getId()) ? map.get(docType.getId()) : 0);
    }
  }

  private static Map<String, Integer> getFieldFacets(Map<String, Map<String, Integer>> allFacets, String field) {
    return Optional.ofNullable(allFacets.get(field))
      .orElseThrow(() -> new SiteSearchRuntimeException("SOLR response did not include facets for '" + field + "'."));
  }

  public JSONArray getCategoriesJson() {
    JSONArray catsJson = new JSONArray();
    for (Category category : _categories) {
      catsJson.put(category.toJson());
    }
    return catsJson;
  }

  public JSONArray getDocumentTypesJson(Optional<String> projectId) {
    JSONArray json = new JSONArray();
    for (DocumentType docType : _docTypes.values()) {
      json.put(docType.toJson(projectId));
    }
    return json;
  }

  public TwoTuple<List<DocumentField>,Boolean> getSearchFields(SearchRequest request, boolean applyFieldsFilter) {

    // gather filtering options
    Optional<String> docTypeFilter = request.getDocTypeFilter().map(filter -> filter.getDocType());
    Optional<List<String>> fieldsFilter =
      applyFieldsFilter && docTypeFilter.isPresent() ?
        request.getDocTypeFilter().get().getFoundOnlyInFields() : Optional.empty();
    Optional<String> projectFilter = request.getRestrictToProject();

    // build out list of fields and mark if all fields included
    List<DocumentField> fields = new ArrayList<>();
    boolean allFieldsIncluded = true;
    for (DocumentType type : _docTypes.values()) {
      // if no docType filter, then add all fields for all types
      if (docTypeFilter.isEmpty()) {
        fields.addAll(type.getSearchFields(projectFilter));
      }
      // if docType filter present, add fields for only requested docType
      else if (docTypeFilter.get().equals(type.getId())) {
        // if no fields filter present, add all fields for this docType
        if (fieldsFilter.isEmpty()) {
          fields.addAll(type.getSearchFields(projectFilter));
        }
        // if fields filter present, only add fields for this docType which are also in the requested list
        else {
          allFieldsIncluded = false;
          List<String> requestedSearchFields = fieldsFilter.get();
          for (DocumentField field : type.getSearchFields(projectFilter)) {
            if (requestedSearchFields.contains(field.getName())) {
              fields.add(field);
            }
          }
        }
      }
    }
    return new TwoTuple<>(fields, allFieldsIncluded);
  }

  public Optional<DocumentType> getDocumentType(String docTypeId) {
    return Optional.ofNullable(_docTypes.get(docTypeId));
  }

  public void setOrganismFacetCounts(
      Optional<List<String>> restrictMetadataToOrganisms,
      Map<String, Map<String, Integer>> allFacets) {
    Map<String,Integer> orgFacets = getFieldFacets(allFacets, ORGANISM_FIELD);
    _organismFacetCounts = new HashMap<>();
    if (restrictMetadataToOrganisms.isEmpty()) {
      _organismFacetCounts.putAll(orgFacets);
    }
    else {
      for (String facetOrg : orgFacets.keySet()) {
        if (restrictMetadataToOrganisms.get().contains(facetOrg)) {
          _organismFacetCounts.put(facetOrg, orgFacets.get(facetOrg));
        }
      }
    }
  }

  public Map<String, Integer> getOrganismFacetCounts() {
    return _organismFacetCounts;
  }

  public void setFieldFacetCounts(Optional<DocTypeFilter> docTypeFilter, FacetQueryResults facetCounts) {
    _fieldFacetCounts = new HashMap<>();
    if (docTypeFilter.isEmpty()) return;
    for (String queryString : facetCounts.keySet()) {
      String fieldName = queryString.substring(0, queryString.indexOf(":"));
      _fieldFacetCounts.put(fieldName, facetCounts.get(queryString));
    }
  }

  public Map<String,Integer> getFieldCounts() {
    return _fieldFacetCounts;
  }

  public void validateRequest(SearchRequest request) {

    // validate document type in docType filter if present
    if (request.getDocTypeFilter().isEmpty()) return;
    DocTypeFilter filter = request.getDocTypeFilter().get();
    String docType = filter.getDocType();
    if (!_docTypes.containsKey(docType)) {
      throw new InvalidRequestException("Document type filtered '" + docType +
          "' is not valid.  Must be one of: " + FormatUtil.join(_docTypes.keySet(), ", "));
    }

    // validate fields in field filter if present
    if (filter.getFoundOnlyInFields().isEmpty()) return;
    List<String> requestedFieldNames = filter.getFoundOnlyInFields().get();
    List<String> validFieldNames = _docTypes.get(docType)
        .getSearchFields(request.getRestrictToProject())
        .stream().map(DocumentField::getName)
        .collect(Collectors.toList());
    List<String> invalidNames = new ArrayList<>();
    for (String filterField : requestedFieldNames) {
      if (!validFieldNames.contains(filterField)) {
        invalidNames.add(filterField);
      }
    }
    if (!invalidNames.isEmpty()) {
      throw new InvalidRequestException("Invalid field names in filter: " + FormatUtil.join(invalidNames, ", "));
    }
  }

}
