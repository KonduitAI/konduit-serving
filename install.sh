#!/bin/bash

STOP_EXECUTION=false

if [[ -z $1 ]]; then
  echo "No install location found..."
  echo ""
  echo "Usage: source install.sh <jar-install-location>"
  echo "Example: "
  echo "source install.sh konduit.jar"
else
  UNAME_OUT="$(uname -s)"
  case "${UNAME_OUT}" in
      Linux*)     PLATFORM=linux-x86_64;;
      Darwin*)    PLATFORM=macosx-x86_64;;
      CYGWIN*)    PLATFORM=windows-x86_64;;
      MINGW*)     PLATFORM=windows-x86_64;;
      *)          PLATFORM="UNKNOWN:${UNAME_OUT}" && echo "unknown platform found $PLATFORM" && STOP_EXECUTION=true
  esac

  echo "Found platform: ""${PLATFORM}"""

  if ! command -v realpath &> /dev/null; then
      echo "Command 'realpath' could not be found."

      if [ "${PLATFORM}" = "macosx-x86_64" ]; then
        echo "Installing through 'brew install coreutils"

        if ! command -v brew &> /dev/null; then
          echo "Command 'brew' could not be found."
          STOP_EXECUTION=true
        fi
      else
        STOP_EXECUTION=true
      fi
  fi

  if [ ${STOP_EXECUTION} = "false" ]; then
    # Just downloading all the dependencies for finding the project version
    mvn help:evaluate -D expression=project.version | grep -e '^[^\[]'

    set -uxo pipefail

    mvn -T 1C clean install -DskipTests=true -Dmaven.test.skip=true -pl konduit-serving-cli -am -Djavacpp.platform="${PLATFORM}"

    MODULE_NAME=konduit-serving-cli
    PROJECT_VERSION="$(mvn help:evaluate -D expression=project.version | grep -e '^[^\[]')"

    export JAR_PATH="$(realpath "$1")"

    cp $MODULE_NAME/target/$MODULE_NAME-"$PROJECT_VERSION".jar "$JAR_PATH"
    SCRIPT_PATH="$(cd "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P)"

    if ! [[ $PATH == ${SCRIPT_PATH}:* ]]; then
      export PATH=$SCRIPT_PATH:$PATH
    fi

    set +uxo pipefail
  fi
fi