#!/usr/bin/env bash
#
# /* ******************************************************************************
#  * Copyright (c) 2021 Konduit K.K.
#  *
#  * This program and the accompanying materials are made available under the
#  * terms of the Apache License, Version 2.0 which is available at
#  * https://www.apache.org/licenses/LICENSE-2.0.
#  *
#  * Unless required by applicable law or agreed to in writing, software
#  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
#  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
#  * License for the specific language governing permissions and limitations
#  * under the License.
#  *
#  * SPDX-License-Identifier: Apache-2.0
#  ******************************************************************************/
#

set -e

cd "$(dirname "$0")"

USAGE_STRING="Usage: bash build.sh [CPU|GPU] [linux|windows|macosx] [tar|zip|exe|rpm|deb] [<EXTRA_MAVEN_OPTIONS>] [<EXTRA_BUILD_OPTIONS>]"
EXAMPLE_STRING_1="Example 1: bash build.sh GPU linux tar,deb"
EXAMPLE_STRING_2="Example 2: bash build.sh GPU linux tar,deb -nsu -Ppublish"

function show_usage() {
  echo "${USAGE_STRING}"
  echo "${EXAMPLE_STRING_1}"
  echo "${EXAMPLE_STRING_2}"
}

if [[ $* == *--help* ]]
then
    echo ""
    echo "A command line utility for building konduit-serving distro packages."
    echo ""
    show_usage
    echo ""
    exit 0
fi

echo "Starting build..."

case "$(uname -s)" in
    Linux*)     INFERRED_PLATFORM=linux;;
    Darwin*)    INFERRED_PLATFORM=macosx;;
    CYGWIN*)    INFERRED_PLATFORM=windows;;
    MINGW*)     INFERRED_PLATFORM=windows;;
    *)          INFERRED_PLATFORM=linux
esac

CHIP=$(echo "${1:-CPU}" | awk '{ print toupper($0)}')
PLATFORM=$(echo "${2:-${INFERRED_PLATFORM}}" | awk '{ print tolower($0)}')
DISTRO_TYPE=$(echo "${3:-tar}" | awk '{ print tolower($0)}')
EXTRA_MAVEN_OPTIONS="${4}"
EXTRA_BUILD_OPTIONS="${5}"

PROJECT_VERSION=$(mvn -q -Dexec.executable="echo" -Dexec.args='${project.version}' --non-recursive exec:exec)

if [[ "$CHIP" != "CPU" && "$CHIP" != "GPU" ]]
then
    echo "Selected CHIP $CHIP should be one of [CPU, GPU]"
    show_usage
    exit 1
fi

if [[ "$PLATFORM" != "linux" && "$PLATFORM" != "windows" && "$PLATFORM" != "macosx" ]]
then
    echo "Selected PLATFORM $PLATFORM should be one of [linux, windows, macosx]"
    show_usage
    exit 1
fi

if [[ "$DISTRO_TYPE" == *tar* || "$DISTRO_TYPE" == *zip* || "$DISTRO_TYPE" == *exe* || "$DISTRO_TYPE" == *rpm* || "$DISTRO_TYPE" == *deb* ]]
then
    echo ""
else
    echo "DISTRO TYPE $DISTRO_TYPE should contain one of [tar, zip, exe, rpm, deb]"
    show_usage
    exit 1
fi

if [[ "$PLATFORM" == macosx && "$CHIP" == "GPU" ]]
then
    echo "GPU chip type is not available for macosx platform"
    exit 1
fi

echo "Building project version: ${PROJECT_VERSION}"

echo "Building a konduit-serving distributable JAR file..."

echo "Selecting CHIP=$CHIP"

echo "Building $CHIP version of konduit-serving for ${PLATFORM} with distro types: (${DISTRO_TYPE}) ..."

if [[ "$CHIP" == "CPU" ]]
then
    BUILD_PROFILES=-Ppython,uberjar,tar
else
    BUILD_PROFILES=-Ppython,uberjar,tar,gpu,intel,cuda-redist
fi 

BUILD_PROFILES=${BUILD_PROFILES},${DISTRO_TYPE}

# Uncomment the other option to toggle maven download logs
# To allow maven download logs
REMOVE_DOWNLOAD_LOGS=""
# To stop maven download logs
#REMOVE_DOWNLOAD_LOGS="-Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn"

RUN_COMMAND="mvn -B ${EXTRA_MAVEN_OPTIONS} clean install ${REMOVE_DOWNLOAD_LOGS} -Dmaven.test.skip=true -Denforcer.skip=true -Djavacpp.platform=${PLATFORM}-x86_64 ${BUILD_PROFILES} -Ddevice=${CHIP} ${EXTRA_BUILD_OPTIONS}"
echo "Running command: ${RUN_COMMAND}"

${RUN_COMMAND}

if [[ "$DISTRO_TYPE" == *tar* || "$DISTRO_TYPE" == *zip* ]]
then
    echo "----------------------------------------"
    echo "TAR and ZIP distros are available at: "
    chmod u+x konduit-serving-tar/target/konduit-serving-tar-${PROJECT_VERSION}-dist/bin/konduit
    echo "konduit-serving-tar/target/konduit-serving-tar-${PROJECT_VERSION}-dist.tar.gz"
    echo "konduit-serving-tar/target/konduit-serving-tar-${PROJECT_VERSION}-dist.zip"
fi

if [[ "$DISTRO_TYPE" == *rpm* ]]
then
    echo "----------------------------------------"
    echo "RPM distro is available at: "
    ls konduit-serving-rpm/target/rpm/konduit-serving-custom-"${PLATFORM}"-x86_64-"${CHIP}"/RPMS/x86_64/konduit-serving-custom-"${PLATFORM}"-x86_64-"${CHIP}"-"${PROJECT_VERSION}"*.x86_64.rpm
fi

if [[ "$DISTRO_TYPE" == *deb* ]]
then
    echo "----------------------------------------"
    echo "DEB distro is available at: "
    echo "konduit-serving-deb/target/konduit-serving-custom-${CHIP}_${PROJECT_VERSION}.deb"
fi

if [[ "$DISTRO_TYPE" == *exe* ]]
then
    echo "----------------------------------------"
    echo "EXE distro is available at: "
    echo "konduit-serving-exe/target/konduit.exe"
fi