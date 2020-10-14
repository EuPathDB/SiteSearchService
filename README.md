# SiteSearchService
A stand-alone, containerized, REST web service supporting VEuPathDB Site Search requests.  

Is implemented to use Solr as the data store.  Expects data in solr to conform to the schema defined in [SolrDeployment](https://github.com/VEuPathDB/SolrDeployment/tree/master/configsets/site-search/conf)

For the REST API, see [Service.java](Service/src/main/java/org/gusdb/sitesearch/service/Service.java)

## Build and install

## Usage
```
runSiteSearchService <baseUri> <port> <configFile>;
Where:
  baseUri: should be a fully qualified URL to the service (e.g. http...) OR the service path beginning with a '/' (e.g. /my-service)
  configFile: a JSON file including a single JSON object, with a single 'solrUrl' property. 
```  


