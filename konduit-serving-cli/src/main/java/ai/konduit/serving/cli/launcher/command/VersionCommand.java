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

package ai.konduit.serving.cli.launcher.command;

import io.vertx.core.cli.annotations.Description;
import io.vertx.core.cli.annotations.Name;
import io.vertx.core.cli.annotations.Summary;
import io.vertx.core.spi.launcher.DefaultCommand;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Name(value = "version", priority = 1)
@Summary("Displays konduit-serving version.")
@Description("Prints the konduit-serving version used by the application along with other build details.")
public class VersionCommand extends DefaultCommand {

    private static String version;

    @Override
    public void run() {
        out.println(getVersion());
    }

    /**
     * Reads the version from the {@code serving-version.txt} file.
     *
     * @return the version
     */
    public static String getVersion() {
        if (version != null) {
            return version;
        }
        try (InputStream is = VersionCommand.class.getClassLoader().getResourceAsStream("META-INF/konduit-serving-cli-git.properties")) {
            if (is == null) {
                throw new IllegalStateException("Cannot find konduit-serving-cli-git.properties on classpath");
            }
            Properties gitProperties = new Properties();
            gitProperties.load(is);
            version = String.format("Version: %s%nCommit hash: %s%nCommit time: %s%nBuild time: %s%n",
                    gitProperties.getProperty("git.build.version"),
                    gitProperties.getProperty("git.commit.id").substring(0, 8),
                    gitProperties.getProperty("git.commit.time"),
                    gitProperties.getProperty("git.build.time"));
            return version;
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }
}

