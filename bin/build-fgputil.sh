#!/bin/bash

mkdir -p .build
cd .build

if [[ -d FgpUtil ]]; then
    echo "FgpUtil already cloned; pulling changes..."
    cd FgpUtil; git pull; cd ..
else
    echo "FgpUtil not present; cloning fresh copy..."
    git clone --depth 1 https://github.com/VEuPathDB/FgpUtil.git
fi

cd FgpUtil
mvn clean install -DskipTests
