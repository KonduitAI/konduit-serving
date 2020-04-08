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

package ai.konduit.serving.settings;

import ai.konduit.serving.settings.constants.Constants;
import ai.konduit.serving.settings.constants.EnvironmentConstants;
import ai.konduit.serving.settings.constants.PropertiesConstants;
import org.nd4j.shade.guava.base.Strings;

import java.nio.file.Paths;
import java.io.File;

public class Fetcher {

    public static File getWorkingDir() {
        String workingDirEnv = System.getenv(EnvironmentConstants.WORKING_DIR);
        String workingDirProp = System.getProperty(PropertiesConstants.WORKING_DIR);

        String workingDirPath = !Strings.isNullOrEmpty(workingDirEnv) ? workingDirEnv :
                !Strings.isNullOrEmpty(workingDirProp) ? workingDirProp : getDefaultWorkingDir();

        return createAndValidateDirectory(workingDirPath);
    }

    public static File getEndpointLogsDir() {
        String endpointLogsDirEnv = System.getenv(EnvironmentConstants.ENDPOINT_LOGS_DIR);
        String endpointLogsDirProp = System.getProperty(PropertiesConstants.ENDPOINT_LOGS_DIR);

        String endpointLogsDirPath = !Strings.isNullOrEmpty(endpointLogsDirEnv) ? endpointLogsDirEnv :
                !Strings.isNullOrEmpty(endpointLogsDirProp) ? endpointLogsDirProp : getDefaultEndpointLogsDir();
        
        return createAndValidateDirectory(endpointLogsDirPath);
    }

    public static File getCommandLogsDir() {
        String commandLogsDirEnv = System.getenv(EnvironmentConstants.COMMAND_LOGS_DIR);
        String commandLogsDirProp = System.getProperty(PropertiesConstants.COMMAND_LOGS_DIR);

        String commandLogsDirPath = !Strings.isNullOrEmpty(commandLogsDirEnv) ? commandLogsDirEnv :
                !Strings.isNullOrEmpty(commandLogsDirProp) ? commandLogsDirProp : getDefaultCommandLogsDir();

        return createAndValidateDirectory(commandLogsDirPath);
    }

    public static String getUserHomeDir() {
        return System.getProperty("user.home");
    }

    public static String getDefaultWorkingDir() {
        return createAndValidateDirectory(Paths.get(getUserHomeDir(), Constants.DEFAULT_BASE_DIR).toFile()).getAbsolutePath();
    }

    public static String getDefaultEndpointLogsDir() {
        return createAndValidateDirectory(new File(getWorkingDir(), "endpoint_logs")).getAbsolutePath();
    }

    public static String getDefaultCommandLogsDir() {
        return createAndValidateDirectory(new File(getWorkingDir(), "command_logs")).getAbsolutePath();
    }

    public static File createAndValidateDirectory(String directoryPath) {
        return createAndValidateDirectory(new File(directoryPath));
    }

    public static File createAndValidateDirectory(File directory) {
        if(directory.isDirectory() && directory.exists()) {
            return directory;
        } else {
            if(directory.exists() && !directory.isDirectory()) {
                throw new IllegalStateException("Invalid directory: " + directory.getAbsolutePath());
            } else if(!directory.exists()) {
                if(directory.mkdirs()) {
                    if(directory.isDirectory()) {
                        return directory;
                    } else {
                        throw new IllegalStateException("Invalid directory: " + directory.getAbsolutePath());
                    }
                } else {
                    throw new IllegalStateException("Unable to create directory: " + directory.getAbsolutePath());
                }
            } else {
                throw new IllegalStateException("Invalid konduit working directory: " + directory.getAbsolutePath());
            }
        }
    }
}
