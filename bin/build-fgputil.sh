#!/bin/bash
##########################################################
##
## Downloads and builds FgpUtil, updating locally if
## already present, and skipping the build if no updates
## were performed in a pull.
##
## In theory, this should limit FgpUtil builds to when
## they are necessary, but if, e.g. the local maven repo
## is cleared out, a site search build will fail, unable
## to find the dependencies.
##
##########################################################

buildRequired() {
  UPSTREAM=${1:-'@{u}'}
  LOCAL=$(git rev-parse @)
  REMOTE=$(git rev-parse "$UPSTREAM")
  BASE=$(git merge-base @ "$UPSTREAM")

  if [ $LOCAL = $REMOTE ]; then
      # Up to date
      echo "false"
  elif [ $LOCAL = $BASE ]; then
      # Need to pull
      echo "true"
  elif [ $REMOTE = $BASE ]; then
      # Need to push
      echo "true"
  else
      # Diverged
      echo "true"
  fi
}

downloadAndBuild() {
  mkdir -p .build
  cd .build
  
  if [[ -d FgpUtil ]]; then
      echo "FgpUtil already cloned; pulling changes..."
      cd FgpUtil
      needBuild=$(buildRequired)
      git pull
      cd ..
  else
      echo "FgpUtil not present; cloning fresh copy..."
      needBuild=true
      git clone --depth 1 https://github.com/VEuPathDB/FgpUtil.git
  fi
  
  if [[ $needBuild = 'true' ]]; then
    echo "Building FgpUtil..."
    cd FgpUtil
    mvn clean install -DskipTests
  else
    echo "Build up to date.  Skipping."
  fi
}

downloadAndBuild
