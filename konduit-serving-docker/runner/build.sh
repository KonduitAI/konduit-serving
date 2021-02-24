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

USAGE_STRING="Usage: bash build.sh [CPU|GPU] [--rebuild-distro] [--push]"
EXAMPLE_STRING_1="Example 1 (Creating a CPU version of the image): bash build.sh CPU"
EXAMPLE_STRING_2="Example 2 (Creating a GPU version and recompiling konduit distro): bash build.sh GPU --rebuild-distro"

function show_usage() {
  echo "${USAGE_STRING}"
  echo "${EXAMPLE_STRING_1}"
  echo "${EXAMPLE_STRING_2}"
}

if [[ $* == *--help* ]]
then
    echo ""
    echo "A command line utility for building konduit-serving runner docker image."
    echo ""
    show_usage
    echo ""
    exit 0
fi

CHIP="${1:-CPU}"

if [[ "$CHIP" != "CPU" && "$CHIP" != "GPU" ]]
then
    echo "Selected CHIP $CHIP should be one of [CPU, GPU]"
    show_usage
    exit 1
fi

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

cd "${SCRIPT_DIR}/../.."

KONDUIT_VERSION=$(mvn -q -Dexec.executable="echo" -Dexec.args='${project.version}' --non-recursive exec:exec)

cd "${SCRIPT_DIR}"

if [[ -d "${SCRIPT_DIR}"/konduit ]]
then
  rm -rf "${SCRIPT_DIR}"/konduit
fi

DISTRO_DIR="${SCRIPT_DIR}"/../../konduit-serving-tar/target/konduit-serving-tar-"${KONDUIT_VERSION}"-dist
if [[ ! -d "${DISTRO_DIR}" || $* == *--rebuild-distro* ]]
then
  bash "${SCRIPT_DIR}"/../../build.sh "${CHIP}" linux tar
fi

cp -r "${DISTRO_DIR}" "${SCRIPT_DIR}"/konduit

docker build --tag konduit/konduit-serving:"$(echo "${CHIP}" | awk '{ print tolower($0)}')" .

if [[ $* == *--push* ]]
then
    docker push konduit/konduit-serving:"$(echo "${CHIP}" | awk '{ print tolower($0)}')"
fi