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

if [[ -f "${PRE_SETUP_FILE}" ]]
then
  echo "Executing pre setup script from ${PRE_SETUP_FILE}"

  bash "${PRE_SETUP_FILE}"
fi

if [[ -f "${CONDA_ENVIRONMENT_FILE}" ]]
then
  echo "Installing conda dependencies from ${CONDA_ENVIRONMENT_FILE}"

  conda env create -f "${CONDA_ENVIRONMENT_FILE}"
  # shellcheck disable=SC2086
  # shellcheck disable=SC2046
  CONDA_ENVIRONMENT_NAME=$(head -n 1 ${CONDA_ENVIRONMENT_FILE} | cut -d' ' -f2)
  source activate "${CONDA_ENVIRONMENT_NAME}"
fi

if [[ -f "${PIP_REQUIREMENTS_FILE}" ]]
then
  echo "Installing pip packages from ${PIP_REQUIREMENTS_FILE}"

  pip install -r "${PIP_REQUIREMENTS_FILE}"
fi

if [[ -f "${POST_SETUP_FILE}" ]]
then
  echo "Executing post setup script from ${POST_SETUP_FILE}"

  bash "${POST_SETUP_FILE}"
fi
