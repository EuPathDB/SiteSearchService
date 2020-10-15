# SiteSearchService
Provides a stand-alone RESTful web service which supports VEuPathDB Site Search requests.  It is essentially a facade over a backing SOLR instance, providing validation and translation of requests, and formatting of responses.  The service can run directly from command line or be containerized in a Docker instance (dockerfiles provided).

The backing SOLR instance must conform to the schema defined in [SolrDeployment](https://github.com/VEuPathDB/SolrDeployment/tree/master/configsets/site-search/conf).

The REST API does not have formal documentation but its endpoint definitions are defined by [the service class](Service/src/main/java/org/gusdb/sitesearch/service/Service.java).

## Build and install
SiteSearchService uses the GUS build system, which requires ant and maven.  It depends on the VEuPathDB projects 'install' and 'FgpUtil'.  An install script, which will create a GUS build directory structure, download the relevant projects, and build the software, can be run by executing:
```
curl https://raw.githubusercontent.com/VEuPathDB/SiteSearchService/master/bin/install.sh | bash
```

## Usage
```
runSiteSearchService <baseUri> <port> <configFile>;
Where:
  baseUri: should be a fully qualified URL to the service (e.g. http...) OR the service path beginning with a '/' (e.g. /my-service)
  port: port the service should be served from
  configFile: a JSON file including a single JSON object, with a single 'solrUrl' property. 
```  
