# SiteSearchService
Provides a stand-alone RESTful web service which supports VEuPathDB Site Search requests.  It is essentially a facade over a backing SOLR instance, providing validation and translation of requests, and formatting of responses.  The service can run directly from command line or be containerized in a Docker instance (Dockerfile provided).

The backing SOLR instance must conform to the schema defined in [SolrDeployment](https://github.com/VEuPathDB/SolrDeployment/tree/master/configsets/site-search/conf).

The REST API does not have formal documentation but its endpoint definitions are defined by [the service class](https://github.com/VEuPathDB/SiteSearchService/blob/master/src/main/java/org/gusdb/sitesearch/service/Service.java).

## Build and install
To build an executable fat jar that contains the service, run:
```
make jar
```

To build a runnable docker image that contains the service, but not the enclosign SOLR instance, run:
```
make docker
```
This will create an image named "site-search" and tagged with "latest".

## Usage
Whether you are running from command line or docker, the service requires the $SOLR_URL environment variable, which points to the wrapped SOLR instance.

Two run scripts demonstrating these respective runs are available in:
```
bin/runLocal.sample
bin/runDocker.sample
```
A docker-compose.yml file is included to deploy this service with its SOLR instance in one step.

## Serving from multiple cores
There are cases where you may want to serve data from another core, but do not want to create another stack with its own solr.  In that case, you can run another service and set SOLR_URL to the path of the core in the existing solr.  This is how the "orthosearch" service is setup (see docker-compose.yml), which runs another copy of the service, but configured to point to its own core.  Traefik rules are then setup to direct to the orthoservice appropriately.
