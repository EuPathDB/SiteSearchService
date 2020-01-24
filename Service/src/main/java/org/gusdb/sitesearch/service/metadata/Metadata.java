package org.gusdb.sitesearch.service.metadata;

import static org.gusdb.fgputil.functional.Functions.getMapFromList;
import static org.gusdb.fgputil.functional.Functions.getMapFromValues;
import static org.gusdb.fgputil.functional.Functions.reduce;
import static org.gusdb.fgputil.json.JsonIterators.arrayIterable;
import static org.gusdb.fgputil.json.JsonIterators.arrayStream;
import static org.gusdb.sitesearch.service.SolrCalls.DOCUMENT_TYPE_FIELD;
import static org.gusdb.sitesearch.service.SolrCalls.ORGANISM_FIELD;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.MapBuilder;
import org.gusdb.fgputil.Tuples.TwoTuple;
import org.gusdb.sitesearch.service.exception.SiteSearchRuntimeException;
import org.gusdb.sitesearch.service.request.DocTypeFilter;
import org.gusdb.sitesearch.service.solr.SolrResponse;
import org.json.JSONArray;
import org.json.JSONObject;

/*{
    "response": {
      "docs": [{"json-blob": [
        {
          "documentTypes": [
            {
              "wdkSearchUrlName": "GenesByText",
              "displayName": "Gene",
              "displayNamePlural": "Genes",
              "id": "gene"
            },
            {
              "wdkSearchUrlName": "GenomicSeqByText",
              "displayName": "Genomic sequence",
              "displayNamePlural": "Genomic sequences",
              "id": "genomic-sequence"
            }
          ],
          "name": "Genome"
        },
        {
          "documentTypes": [{
            "wdkSearchUrlName": "OrganismsByText",
            "displayName": "Organism",
            "displayNamePlural": "Organisms",
            "id": "organism"
          }],
          "name": "Organism"
        },
        {
          "documentTypes": [{
            "wdkSearchUrlName": "EstsByText",
            "displayName": "EST",
            "displayNamePlural": "ESTs",
            "id": "est"
          }],
          "name": "Transcriptomics"
        },
        {
          "documentTypes": [
            {
              "wdkSearchUrlName": "PathwaysByText",
              "displayName": "Metabolic pathway",
              "displayNamePlural": "Metabolic pathways",
              "id": "pathway"
            },
            {
              "wdkSearchUrlName": "CompoundsByText",
              "displayName": "Compound",
              "displayNamePlural": "Compounds",
              "id": "compound"
            }
          ],
          "name": "Metabolism"
        },
        {
          "documentTypes": [
            {
              "wdkSearchUrlName": "DatasetsByText",
              "displayName": "Dataset",
              "displayNamePlural": "Datasets",
              "id": "dataset"
            },
            {
              "displayName": "Search",
              "displayNamePlural": "Searches",
              "id": "search",
              "fields": [
                "ATTR__search_displayName",
                "ATTR__search_description"
              ]
            }
          ],
          "name": "Data access"
        }
      ]}],
      "numFound": 1,
      "start": 0
    },
    "responseHeader": {
      "QTime": 5,
      "params": {
        "q": "*",
        "fl": "json-blob:[json]",
        "fq": "document-type:(document-categories)",
        "wt": "json"
      },
      "status": 0
    }
*/
public class Metadata {

  private static final Logger LOG = Logger.getLogger(Metadata.class);

  private final List<Category> _categories;
  private final Map<String,DocumentType> _docTypes;
  private Map<String,Integer> _organismFacetCounts;

  public Metadata(SolrResponse result) {
    JSONObject document = getSingular(result.getDocuments(), "document-categories");
    _categories = arrayStream(document.getJSONArray("json-blob"))
      .map(jsonType -> jsonType.getJSONObject())
      .map(catObj -> new Category(catObj.getString("name"))
        .addDocumentTypes(arrayStream(catObj.getJSONArray("documentTypes"))
          .map(jsonType -> jsonType.getJSONObject())
          .map(docTypeObj -> new DocumentType(
              docTypeObj.getString("id"),
              docTypeObj.getString("displayName"),
              docTypeObj.getString("displayNamePlural"),
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
    JSONObject document = getSingular(result.getDocuments(), "document-fields");

    // put fields data in a map for easy access
    Map<String,List<DocumentField>> fieldMap = getMapFromList(
      arrayIterable(document.getJSONArray("json-blob")), val -> {
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

  public JSONArray getDocumentTypesJson(JsonDestination dest) {
    JSONArray json = new JSONArray();
    for (DocumentType docType : _docTypes.values()) {
      json.put(docType.toJson(dest));
    }
    return json;
  }

  public List<DocumentField> getSearchFields(Optional<DocTypeFilter> filter) {
    List<DocumentField> fields = new ArrayList<>();
    for (DocumentType type : _docTypes.values()) {
      // if no docType filter, then add all fields for all types
      if (filter.isEmpty()) {
        fields.addAll(type.getFields());
      }
      // if docType filter present, add fields for only requested docType
      else if (filter.get().getDocType().equals(type.getId())) {
        // if no fields filter present, add all fields for this docType
        if (filter.get().getFoundOnlyInFields().isEmpty()) {
          fields.addAll(type.getFields());
        }
        // if fields filter present, only add fields for this docType which are also in the requested list
        else {
          List<String> requestedSearchFields = filter.get().getFoundOnlyInFields().get();
          for (DocumentField field : type.getFields()) {
            if (requestedSearchFields.contains(field.getName())) {
              fields.add(field);
            }
          }
        }
      }
    }
    return fields;
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

}
