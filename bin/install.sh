#!/bin/bash

mkdir SiteSearchService
cd SiteSearchService
mkdir -p gus_home/config
echo "perl=/usr/bin/perl" > gus_home/config/gus.config
mkdir project_home
cd project_home
tsrc init git@github.com:VEuPathDB/tsrc.git --group siteSearchData
cd ..
source install/bin/gusEnv.bash
bld SiteSearchData
echo
echo "You're all set!"
echo
echo "To run SiteSearchService, create a JSON config file containing the URL of your SOLR core, e.g."
echo
echo "  { \"solrUrl\": \"https://mysolr.example.com/solr/site_search\" } "
echo
echo "Then run:"
echo
echo "  > runSiteSearchService <baseUri> <port> <configFile>"
echo
