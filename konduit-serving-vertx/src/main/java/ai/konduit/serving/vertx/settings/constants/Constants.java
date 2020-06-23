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

package ai.konduit.serving.vertx.settings.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * This class contains important keys for different operations inside konduit-serving
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Constants {

    /**
     * The name of the default base name of the konduit-serving working directory.
     */
    public static final String DEFAULT_WORKING_BASE_DIR_NAME = ".konduit-serving";

    /**
     * The name of the default konduit-serving build data directory .
     */
    public static final String DEFAULT_BUILD_DIR_NAME = "build";

    /**
     * Default base directory name for the endpoints log (/logs).
     */
    public static final String DEFAULT_ENDPOINT_LOGS_DIR_NAME = "endpoint_logs";

    /**
     * Default directory name for containing the command log files.
     */
    public static final String DEFAULT_COMMAND_LOGS_DIR_NAME = "command_logs";

    /**
     * Default directory name for containing the running server data. The files in
     * this directory usually contains the server configurations. The format of the files is
     * {@code <pid>.data}
     */
    public static final String SERVERS_DATA_DIR_NAME = "servers";

    /**
     * Name of the log file which contains the logging data for the {@code /logs}
     * endpoint.
     */
    public static final String MAIN_ENDPOINT_LOGS_FILE = "main.log";
}
