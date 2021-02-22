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

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

cd "${SCRIPT_DIR}/../.."

KONDUIT_VERSION=$(mvn -q -Dexec.executable="echo" -Dexec.args='${project.version}' --non-recursive exec:exec)

cd "${SCRIPT_DIR}"

if [[ -d "${SCRIPT_DIR}"/konduit ]]
then
  rm -rf "${SCRIPT_DIR}"/konduit
fi

cp -r "${SCRIPT_DIR}"/../../konduit-serving-tar/target/konduit-serving-tar-"${KONDUIT_VERSION}"-dist "${SCRIPT_DIR}"/konduit

docker build --tag konduit/konduit-serving:latest .
