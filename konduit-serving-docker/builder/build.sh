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

USAGE_STRING="Usage: bash build.sh [--push]"
EXAMPLE_STRING_1="Example 1 (Creating builder docker image): bash build.sh"

function show_usage() {
  echo "${USAGE_STRING}"
  echo "${EXAMPLE_STRING_1}"
}

if [[ $* == *--help* ]]
then
    echo ""
    echo "A command line utility for building konduit-serving builder docker image."
    echo ""
    show_usage
    echo ""
    exit 0
fi

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

cd "${SCRIPT_DIR}"

docker build --tag konduit/konduit-serving-builder:latest .

if [[ $* == *--push* ]]
then
    docker push konduit/konduit-serving-builder:latest
fi