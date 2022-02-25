/*
 *  ******************************************************************************
 *  * Copyright (c) 2022 Konduit K.K.
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

import io.vertx.ext.web.handler.BodyHandler;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * This class contains important constants for different environment variable settings
 * for konduit-serving.
 * @deprecated To be removed - https://github.com/KonduitAI/konduit-serving/issues/298
 */
@Deprecated
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EnvironmentConstants {

    /**
     * An environment variable for setting the working directory for konduit serving.
     * The working directory contains the runtime files generated by vertx or
     * konduit-serving itself. The runtime files could contain logs,
     * running process details, vertx cache files etc.
     */
    public static final String WORKING_DIR = "KONDUIT_WORKING_DIR";

    /**
     * This variable is responsible for setting the path where the log files for a konduit server
     * is kept for the `/logs` endpoint.
     */
    public static final String ENDPOINT_LOGS_DIR = "KONDUIT_ENDPOINT_LOGS_DIR";

    /**
     * Default directory for containing the command line logs for konduit-serving
     */
    public static final String COMMAND_LOGS_DIR = "KONDUIT_COMMAND_LOGS_DIR";

    /**
     * Sets the directory where the file uploads are kept for Vertx {@link BodyHandler}
     */
    public static final String FILE_UPLOADS_DIR = "KONDUIT_FILE_UPLOADS_DIR";
}
