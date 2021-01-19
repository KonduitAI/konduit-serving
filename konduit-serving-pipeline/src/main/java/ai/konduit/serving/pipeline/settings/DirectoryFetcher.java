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

package ai.konduit.serving.pipeline.settings;

import ai.konduit.serving.pipeline.settings.constants.Constants;
import ai.konduit.serving.pipeline.settings.constants.EnvironmentConstants;
import ai.konduit.serving.pipeline.settings.constants.PropertiesConstants;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.File;
import java.nio.file.Paths;

/**
 * This class is responsible for fetching different directories for konduit-serving
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DirectoryFetcher {

    /**
     * Creates the working directory if missing and fetches it.
     * @return konduit-serving working directory.
     */
    public static File getWorkingDir() {
        return createAndValidateDirectory(
                KonduitSettings.fetchValueBasedOnPriority(
                        System.getenv(EnvironmentConstants.WORKING_DIR),
                        System.getProperty(PropertiesConstants.WORKING_DIR),
                        getDefaultWorkingDir()
                )
        );
    }

    /**
     * Creates the vertx runtime and cache data directory, if missing and fetches it.
     * @return konduit-serving vertx cache and runtime data directory.
     */
    public static File getVertxDir() {
        return createAndValidateDirectory(
                KonduitSettings.fetchValueBasedOnPriority(
                        System.getenv(EnvironmentConstants.VERTX_DIR),
                        System.getProperty(PropertiesConstants.VERTX_DIR),
                        getDefaultVertxDir()
                )
        );
    }

    /**
     * Creates the build directory if missing and fetches it.
     * @return konduit-serving build directory.
     */
    public static File getBuildDir() {
        return createAndValidateDirectory(
                KonduitSettings.fetchValueBasedOnPriority(
                        System.getenv(EnvironmentConstants.BUILD_DIR),
                        System.getProperty(PropertiesConstants.BUILD_DIR),
                        getDefaultBuildDir()
                )
        );
    }

    /**
     * Creates the profiles directory if missing and fetches it.
     * @return konduit-serving profiles directory.
     */
    public static File getProfilesDir() {
        return createAndValidateDirectory(
                KonduitSettings.fetchValueBasedOnPriority(
                        System.getenv(EnvironmentConstants.PROFILES_DIR),
                        System.getProperty(PropertiesConstants.PROFILES_DIR),
                        getDefaultProfilesDir()
                )
        );
    }

    /**
     * Creates the server data directory if missing and fetches it.
     * @return konduit-serving server data directory
     */
    public static File getServersDataDir() {
        return createAndValidateDirectory(new File(getWorkingDir(), Constants.DEFAULT_SERVERS_DATA_DIR_NAME));
    }

    /**
     * Creates the logs endpoint logs data directory if missing and fetches it.
     * @return konduit-serving logs endpoint logs data directory
     */
    public static File getEndpointLogsDir() {
        return createAndValidateDirectory(
                KonduitSettings.fetchValueBasedOnPriority(
                        System.getenv(EnvironmentConstants.ENDPOINT_LOGS_DIR),
                        System.getProperty(PropertiesConstants.ENDPOINT_LOGS_DIR),
                        getDefaultEndpointLogsDir()
                )
        );
    }

    /**
     * Creates the command logs directory if missing and fetches it.
     * @return konduit-serving command logs directory
     */
    public static File getCommandLogsDir() {
        return createAndValidateDirectory(
                KonduitSettings.fetchValueBasedOnPriority(
                        System.getenv(EnvironmentConstants.COMMAND_LOGS_DIR),
                        System.getProperty(PropertiesConstants.COMMAND_LOGS_DIR),
                        getDefaultCommandLogsDir()
                )
        );
    }

    /**
     * Creates the file uploads directory if missing and fetches it.
     * @return konduit-serving file upldads directory
     */
    public static File getFileUploadsDir() {
        return createAndValidateDirectory(
                KonduitSettings.fetchValueBasedOnPriority(
                        System.getenv(EnvironmentConstants.FILE_UPLOADS_DIR),
                        System.getProperty(PropertiesConstants.FILE_UPLOADS_DIR),
                        getDefaultFileUploadsDir()
                )
        );
    }

    /**
     * Returns user home directory
     * @return user home directory
     */
    public static String getUserHomeDir() {
        return System.getProperty("user.home");
    }

    /**
     * Creates the default working directory if missing and fetches it.
     * @return konduit-serving default working directory absolute path.
     */
    public static String getDefaultWorkingDir() {
        return createAndValidateDirectory(Paths.get(getUserHomeDir(), Constants.DEFAULT_WORKING_BASE_DIR_NAME).toFile()).getAbsolutePath();
    }

    /**
     * Creates the default vertx runtime and cache data directory, if missing and fetches it.
     * @return konduit-serving default vertx runtime and cache data directory absolute path.
     */
    public static String getDefaultVertxDir() {
        return createAndValidateDirectory(new File(getWorkingDir(), Constants.DEFAULT_VERTX_DIR_NAME)).getAbsolutePath();
    }

    /**
     * Creates the default build directory if missing and fetches it.
     * @return konduit-serving default build directory absolute path.
     */
    public static String getDefaultBuildDir() {
        return createAndValidateDirectory(new File(getWorkingDir(), Constants.DEFAULT_BUILD_DIR_NAME)).getAbsolutePath();
    }

    /**
     * Creates the default profiles directory if missing and fetches it.
     * @return konduit-serving default profiles directory absolute path.
     */
    public static String getDefaultProfilesDir() {
        return createAndValidateDirectory(new File(getWorkingDir(), Constants.DEFAULT_PROFILES_DIR_NAME)).getAbsolutePath();
    }

    /**
     * Creates the default logs endpoint data directory if missing and fetches it.
     * @return konduit-serving default logs endpoint data directory absolute path.
     */
    public static String getDefaultEndpointLogsDir() {
        return createAndValidateDirectory(new File(getWorkingDir(), Constants.DEFAULT_ENDPOINT_LOGS_DIR_NAME)).getAbsolutePath();
    }

    /**
     * Creates the default command logs directory if missing and fetches it.
     * @return konduit-serving default command logs directory absolute path.
     */
    public static String getDefaultCommandLogsDir() {
        return createAndValidateDirectory(new File(getWorkingDir(), Constants.DEFAULT_COMMAND_LOGS_DIR_NAME)).getAbsolutePath();
    }

    /**
     * Creates the default file upload directory if missing and fetches it.
     * @return konduit-serving default file upload directory absolute path.
     */
    public static String getDefaultFileUploadsDir() {
        return createAndValidateDirectory(System.getProperty("java.io.tmpdir")).getAbsolutePath();
    }

    /**
     * Creates a directory based on the given path string if missing
     * @param directoryPath a string contain the path string of the directory location
     * @return the created directory.
     */
    public static File createAndValidateDirectory(String directoryPath) {
        return createAndValidateDirectory(new File(directoryPath));
    }

    /**
     * Creates a directory based on the given path
     * @param directory a string contain the path of the directory location
     * @return the created directory.
     */
    public static File createAndValidateDirectory(File directory) {
        if(directory.exists()) {
            if(directory.isDirectory()) {
                return directory;
            } else {
                throw new IllegalStateException("Invalid directory: " + directory.getAbsolutePath());
            }
        } else {
            if (directory.mkdirs()) {
                if (directory.isDirectory()) {
                    return directory;
                } else {
                    throw new IllegalStateException("Invalid directory: " + directory.getAbsolutePath());
                }
            } else {
                throw new IllegalStateException("Unable to create directory: " + directory.getAbsolutePath());
            }
        }
    }

}
