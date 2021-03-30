# SiteSearchService
Provides a stand-alone RESTful web service which supports VEuPathDB Site Search requests.  It is essentially a facade over a backing SOLR instance, providing validation and translation of requests, and formatting of responses.  The service can run directly from command line or be containerized in a Docker instance (Dockerfile provided).

The backing SOLR instance must conform to the schema defined in [SolrDeployment](https://github.com/VEuPathDB/SolrDeployment/tree/master/configsets/site-search/conf).

The REST API does not have formal documentation but its endpoint definitions are defined by [the service class](Service/src/main/java/org/gusdb/sitesearch/service/Service.java).

## Build and install
To build an executable fat jar that contains the service, run:
```
make jar
```
This will download FgpUtil, the only dynamic dependency, and build the service.

To build a runnable docker image that contains the service, but not the enclosign SOLR instance, run:
```
make docker
```
This will create a "latest" image of an image tagged "site-search".

## Usage
Whether you are running from command line or docker, the service requires the $SOLR_URL environment variable, which points to the wrapped SOLR instance.

Two run scripts demonstrating these respective runs are available in:
```
bin/runLocal.sample
bin/runDocker.sample
```
A docker-compose.yml file is included to deploy this service with its SOLR instance in one step.

