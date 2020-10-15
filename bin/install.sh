#!/bin/bash

echo "Creating GUS build directory structure under ./SiteSearchService"
mkdir SiteSearchService
cd SiteSearchService
mkdir -p gus_home/config
echo "perl=/usr/bin/perl" > gus_home/config/gus.config
mkdir project_home
cd project_home
echo "Cloning required project repositories"
echo " - install"
git clone https://github.com/VEuPathDB/install.git > /dev/null
echo " - FgpUtil"
git clone https://github.com/VEuPathDB/FgpUtil.git > /dev/null
echo " - SiteSearchService"
git clone https://github.com/VEuPathDB/SiteSearchService.git > /dev/null
cd ..
echo "Setting build environment"
source install/bin/gusEnv.bash
bld SiteSearchData
echo
echo "You're all set!"
echo
echo "To run SiteSearchService:"
echo
echo "1. > cd ./SiteSearchService; source project_home/install/bin/gusEnv.bash"
echo
echo "2. Create a JSON config file containing the URL of your SOLR core, e.g."
echo
echo "  { \"solrUrl\": \"https://mysolr.example.com/solr/site_search\" } "
echo
echo "3. > runSiteSearchService <baseUri> <port> <configFile>"
echo
