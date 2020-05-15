#!/bin/bash

set -euo pipefail

BINARY_FILE_NAME=$1

echo "---"
echo "Looking for old asset file on repo..."
RELEASE_DATA=$(curl -X GET https://api.github.com/repos/KonduitAI/konduit-serving/releases/25710929)

id=" "
eval "$(echo "${RELEASE_DATA}" | grep -C7 "name.:.\+""${BINARY_FILE_NAME}""" | grep -m 1 "id.:" | grep -w id | tr : = | tr -cd '[[:alnum:]]=')"
echo "---"
if [ "${id}" = " " ]; then
    echo "No old asset file found with the same file name."
else
    echo "Deleting old asset file..."
    curl -u "$(cat ~/TOKEN)" -X "DELETE" "https://api.github.com/repos/KonduitAI/konduit-serving/releases/assets/""${id}"""
    echo "Old asset file deleted..."
fi

echo "---"
echo "Uploading ${BINARY_FILE_NAME}"
curl -u "$(cat ~/TOKEN)" --data-binary @builds/"${BINARY_FILE_NAME}" -H "Content-Type:application/octet-stream" -X POST "https://uploads.github.com/repos/KonduitAI/konduit-serving/releases/25710929/assets?name=""${BINARY_FILE_NAME}"""
echo "---"
echo "Uploaded ""${BINARY_FILE_NAME}"" at URL: https://github.com/KonduitAI/konduit-serving/releases/download/cli_base/${BINARY_FILE_NAME}"