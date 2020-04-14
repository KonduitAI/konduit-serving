/*
 *  ******************************************************************************
 *  * Copyright (c) 2020 Konduit K.K.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Apache License, Version 2.0 which is available at
 *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  * License for the specific language governing permissions and limitations
 *  * under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *  *****************************************************************************
 */

package ai.konduit.serving.settings.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EnvironmentConstants {
    public static final String WORKING_DIR = "KONDUIT_WORKING_DIR";
    public static final String ENDPOINT_LOGS_DIR = "KONDUIT_ENDPOINT_LOGS_DIR";
    public static final String COMMAND_LOGS_DIR = "KONDUIT_COMMAND_LOGS_DIR";
    public static final String FILE_UPLOADS_DIR = "KONDUIT_FILE_UPLOADS_DIR";
}