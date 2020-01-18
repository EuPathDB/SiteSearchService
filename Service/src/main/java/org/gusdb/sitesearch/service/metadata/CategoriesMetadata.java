package org.gusdb.sitesearch.service.metadata;

import static org.gusdb.fgputil.functional.Functions.getMapFromList;
import static org.gusdb.fgputil.functional.Functions.getMapFromValues;
import static org.gusdb.fgputil.functional.Functions.reduce;
import static org.gusdb.fgputil.json.JsonIterators.arrayIterable;
import static org.gusdb.fgputil.json.JsonIterators.arrayStream;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.MapBuilder;
import org.gusdb.fgputil.Tuples.TwoTuple;
import org.gusdb.sitesearch.service.util.SiteSearchRuntimeException;
import org.gusdb.sitesearch.service.util.SolrResponse;
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

public class CategoriesMetadata {

  private static final Logger LOG = Logger.getLogger(CategoriesMetadata.class);

  private final List<Category> _categories;
  private final Map<String,DocumentType> _docTypes;

  public CategoriesMetadata(SolrResponse result) {
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

  private JSONObject getSingular(List<JSONObject> documents, String docType) {
    if (documents.size() != 1) {
      String message = documents.size() == 0 ? "No" : "More than one (" + documents.size() + ")";
      throw new SiteSearchRuntimeException(message + " SOLR documents found with type '" + docType + "'");
    }
    return documents.get(0);
  }

  public CategoriesMetadata addFieldData(SolrResponse result) {
    JSONObject document = getSingular(result.getDocuments(), "document-fields");

    // put fields data in a map for easy access
    Map<String,List<DocumentField>> fieldMap = getMapFromList(
      arrayIterable(document.getJSONArray("json-blob")), val -> {
        JSONObject obj = val.getJSONObject();
        return new TwoTuple<String,List<DocumentField>>(
          obj.getString("document-type"),
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
        LOG.warn("Categories metadata contains document-type '" + docType.getId() +
            "' but Fields metadata does not.  This means no records of that " +
            "document-type will ever be found since its fields cannot be specified.");
      }
    }

    // warn if fields contains doc types that categories does not
    Set<String> knownDocTypes = _docTypes.keySet();
    for (String fieldDocType : fieldMap.keySet()) {
      if (!knownDocTypes.contains(fieldDocType)) {
        LOG.warn("Fields metadata contains document-type '" + fieldDocType +
            "' but Categories metadata does not.  This means no records of that " +
            "document-type will ever be found; the document-type is not used.");
      }
    }

    return this;
  }

  public JSONArray toJson() {
    JSONArray catsJson = new JSONArray();
    for (Category category : _categories) {
      catsJson.put(category.toJson());
    }
    return catsJson;
  }

}
