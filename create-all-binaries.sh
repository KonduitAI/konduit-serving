#!/bin/bash

set -euo pipefail

declare -a platforms=("windows-x86_64" "macosx-x86_64" "linux-x86_64")
declare -a chips=("cpu" "gpu")
declare -a spins=("minimal" "pmml" "python" "all")

mkdir -p builds

PROJECT_VERSION="$(mvn help:evaluate -D expression=project.version | grep -e '^[^\[]')"
LOGS_FILE=builds/mavenlogs.log

if [[ -f "${LOGS_FILE}" ]]; then
    rm "${LOGS_FILE}"
fi

echo "You can find maven build logs at: $(realpath "${LOGS_FILE}")"

INDEX=0
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
        declare -a cuda_versions=("10.0" "10.1" "10.2")

        for cuda_version in "${cuda_versions[@]}"
        do
          echo "-------------------------------------------------------"

          INDEX=$((INDEX + 1))

          OUTPUT_FILE_NAME=konduit-serving-uberjar-"${PROJECT_VERSION}"-"${spin}"-"${platform}"-"${chip}"-cuda-"${cuda_version}".jar
          TARGET=builds/"${OUTPUT_FILE_NAME}"
          COMMAND="python build_jar.py -p ""${platform}"" -c ""${chip}"" -cv ""${cuda_version}"" -s ""${spin}"" --target ""${TARGET}"""

          echo "${INDEX}: Compiling for ${platform} | gpu | ${cuda_version} | ${spin} ==> Command: $(${COMMAND} -sbc)"
          bash -c "${COMMAND}" >> "${LOGS_FILE}"

          echo "==> Binary created at: $(realpath "${TARGET}")"
          echo "STATS: $(stat -c "%s %y" "${TARGET}" | awk '{ byte=$1 /1024/1024; print "("byte "M | Date: " $2 " | Time: " $3 " "  $4")"}')"

          ./upload-binary.sh "${OUTPUT_FILE_NAME}"
        done
      else
        echo "-------------------------------------------------------"

        INDEX=$((INDEX + 1))

        OUTPUT_FILE_NAME=konduit-serving-uberjar-"${PROJECT_VERSION}"-"${spin}"-"${platform}"-"${chip}".jar
        TARGET=builds/"${OUTPUT_FILE_NAME}"
        COMMAND="python build_jar.py -p ""${platform}"" -c ""${chip}"" -s ""${spin}"" --target ""${TARGET}"""

        echo "${INDEX}: Compiling for ${platform} | ${chip} | ${spin} ==> Command: $(${COMMAND} -sbc)"
        bash -c "${COMMAND}" >> "${LOGS_FILE}"

        echo "==> Binary created at: $(realpath "${TARGET}")"
        echo "STATS: $(stat -c "%s %y" "${TARGET}" | awk '{ byte=$1 /1024/1024; print "("byte "M | Date: " $2 " | Time: " $3 " "  $4")"}')"

        ./upload-binary.sh "${OUTPUT_FILE_NAME}"
      fi
    done
  done
done

