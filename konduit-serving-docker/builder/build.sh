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
cd "${SCRIPT_DIR}"

docker build --build-arg "SCRIPT_DIR=$SCRIPT_DIR" --tag konduit/konduit-serving-builder:latest .
