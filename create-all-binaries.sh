#!/bin/bash

set -euxo pipefail

declare -a platforms=("windows-x86_64" "macosx-x86_64" "linux-x86_64")
declare -a chips=("cpu" "gpu")
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

      echo "Compiling for $platform | $chip | $spin"

      python build_jar.py --os "${platform}" --chip "${chip}" --spin "${spin}" \
          --target builds/konduit-serving-uberjar-"${PROJECT_VERSION}"-"${spin}"-"${platform}"-"${chip}".jar
    done
  done
done

