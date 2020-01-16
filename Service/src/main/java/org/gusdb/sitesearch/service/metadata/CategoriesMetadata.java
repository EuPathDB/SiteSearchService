package org.gusdb.sitesearch.service.metadata;

import static org.gusdb.fgputil.json.JsonIterators.arrayStream;

import java.util.List;
import java.util.stream.Collectors;

import org.gusdb.sitesearch.service.util.SiteSearchRuntimeException;
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

  private List<Category> _categories;

  public CategoriesMetadata(JSONObject responseBody) {
    JSONObject response = responseBody.getJSONObject("response");
    int numFound = response.getInt("numFound");
    if (numFound != 1) {
      String message = numFound == 0 ? "No" : "More than one (" + numFound + ")";
      throw new SiteSearchRuntimeException(message + " SOLR documents found with type 'document-categories'");
    }
    JSONArray documents = response
      .getJSONArray("docs")
      .getJSONObject(0)
      .getJSONArray("json-blob");
    _categories = arrayStream(documents)
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
  }

}
