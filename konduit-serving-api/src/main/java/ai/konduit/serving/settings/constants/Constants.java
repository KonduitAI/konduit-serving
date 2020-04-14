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
public class Constants {
    public static final String DEFAULT_WORKING_BASE_DIR_NAME = ".konduit-serving";
    public static final String DEFAULT_ENDPOINT_LOGS_DIR_NAME = "endpoint_logs";
    public static final String DEFAULT_COMMAND_LOGS_DIR_NAME = "command_logs";
    public static final String SERVERS_DATA_DIR_NAME = "servers";
    public static final String MAIN_ENDPOINT_LOGS_FILE = "main.log";
}
