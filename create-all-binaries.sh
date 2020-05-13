#!/bin/bash

set -euxo pipefail

declare -a platforms=("windows-x86_64" "macosx-x86_64" "linux-x86_64")
declare -a chips=("gpu")
declare -a spins=("minimal" "pmml" "python" "all")

mkdir -p builds

PROJECT_VERSION="$(mvn help:evaluate -D expression=project.version | grep -e '^[^\[]')"

## now loop through the above array
for platform in "${platforms[@]}"
do
  for chip in "${chips[@]}"
  do
    for spin in "${spins[@]}"
    do
      if [[ "${platform}" == 'macosx-x86_64' && "${chip}" == 'gpu' ]]; then
        continue
      fi

      if [[ "${chip}" == 'gpu' ]]; then
        declare -a cuda_versions=("10.1" "10.2")

        for cuda_version in "${cuda_versions[@]}"
        do
          echo "Compiling for ${platform} | gpu | ${cuda_version} | ${spin}"

          python build_jar.py -p "${platform}" -c "${chip}" -cv ${cuda_version} -s "${spin}" \
              --target builds/konduit-serving-uberjar-"${PROJECT_VERSION}"-"${spin}"-"${platform}"-"${chip}"-${cuda_version}.jar
        done
      else
        echo "Compiling for ${platform} | ${chip} | ${spin}"

        python build_jar.py -p "${platform}" -c "${chip}" -s "${spin}" \
            --target builds/konduit-serving-uberjar-"${PROJECT_VERSION}"-"${spin}"-"${platform}"-"${chip}".jar
      fi
    done
  done
done

