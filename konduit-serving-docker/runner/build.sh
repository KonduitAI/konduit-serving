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

SCRIPT_DIR="$(dirname "$0")"
cd "${SCRIPT_DIR}/../.."

KONDUIT_VERSION=$(mvn -q -Dexec.executable="echo" -Dexec.args='${project.version}' --non-recursive exec:exec)

cd "${SCRIPT_DIR}"

docker build --build-arg "SCRIPT_DIR=$SCRIPT_DIR" --build-arg "KONDUIT_VERSION=$KONDUIT_VERSION" --tag konduit/konduit-serving-builder:latest .
