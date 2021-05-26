#!/usr/bin/env bash

set -e

if [ -z "${1}" ]
  then
    echo No version specified
    echo ""
    echo Usage: sh update-konduit-version.sh 0.2.0-SNAPSHOT
    exit 0
fi

echo Updating konduit-serving to version "${1}"
mvn versions:set -DnewVersion="${1}"

echo Version updated to "${1}"



