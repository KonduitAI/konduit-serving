#!/bin/bash
OLD_VERSION=$1
VERSION=$2
#Spark versions, like <version>xxx_spark_2-SNAPSHOT</version>
for f in $(find . -name 'pom.xml' -not -path '*target*'); do
    sed -i "s/version>.*_spark_.*</version>${VERSION}_spark_2</g" $f
done
mvn versions:set -DallowSnapshots=true -DgenerateBackupPoms=false -DnewVersion="$VERSION"
# back to a version stanza
sed -i "s/<version>$OLD_VERSION<\/version>/<version>$VERSION<\/version>/" pom.xml
