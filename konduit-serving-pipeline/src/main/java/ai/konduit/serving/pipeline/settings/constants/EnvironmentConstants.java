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

package ai.konduit.serving.pipeline.settings.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * This class contains important constants for different environment variable settings
 * for konduit-serving.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EnvironmentConstants {

    /**
     * Environment variable name for storing port number for the konduit server.
     * This variable will be prioritizes over {@link ai.konduit.serving.vertx.config.InferenceConfiguration#port()}
     */
    public static final String KONDUIT_SERVING_PORT = "KONDUIT_SERVING_PORT";

    /**
     * An environment variable for setting the working directory for konduit serving.
     * The working directory contains the runtime files generated by vertx or
     * konduit-serving itself. The runtime files could contain logs,
     * running process details, vertx cache files etc.
     */
    public static final String WORKING_DIR = "KONDUIT_WORKING_DIR";

    /**
     * Environment variable specifying vertx runtime and cache directory.
     */
    public static final String VERTX_DIR = "KONDUIT_VERTX_DIR";

    /**
     * Environment variable specifying build data directory where build logs for the build CLI are kept.
     */
    public static final String BUILD_DIR = "KONDUIT_BUILD_DIR";

    /**
     * Environment variable specifying profiles data directory where details of individual profiles are kept.
     */
    public static final String PROFILES_DIR = "KONDUIT_PROFILES_DIR";

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
     * Sets the directory where the file uploads are kept for Vertx BodyHandler
     */
    public static final String FILE_UPLOADS_DIR = "KONDUIT_FILE_UPLOADS_DIR";

    public static final String CONSUMER_TOPIC_NAME = "KONDUIT_CONSUMER_TOPIC_NAME";

    public static final String KAFKA_CONSUMER_SERIALIZER_CLASS = "KONDUIT_KAFKA_CONSUMER_SERIALIZER_CLASS";

    public static final String KAFKA_CONSUMER_DESERIALIZER_CLASS = "KONDUIT_KAFKA_CONSUMER_DESERIALIZER_CLASS";

    public static final String CONSUMER_GROUP_ID = "KONDUIT_CONSUMER_GROUP_ID";

    public static final String CONSUMER_OFFSET_RESET = "KONDUIT_CONSUMER_OFFSET_RESET";

    public static final String CONSUMER_AUTO_COMMIT = "KONDUIT_CONSUMER_AUTO_COMMIT";

    public static final String PRODUCER_TOPIC_NAME = "KONDUIT_PRODUCER_TOPIC_NAME";

    public static final String KAFKA_PRODUCER_SERIALIZER_CLASS = "KONDUIT_KAFKA_PRODUCER_SERIALIZER_CLASS";

    public static final String KAFKA_PRODUCER_DESERIALIZER_CLASS = "KONDUIT_KAFKA_PRODUCER_DESERIALIZER_CLASS";

    public static final String PRODUCER_ACKS = "KONDUIT_PRODUCER_ACKS";
}
