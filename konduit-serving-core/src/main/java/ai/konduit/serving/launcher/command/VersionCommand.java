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

package ai.konduit.serving.launcher.command;

import io.vertx.core.cli.CLIException;
import io.vertx.core.cli.annotations.Description;
import io.vertx.core.cli.annotations.Name;
import io.vertx.core.cli.annotations.Summary;
import io.vertx.core.spi.launcher.DefaultCommand;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

@Name(value = "version", priority = 1)
@Summary("Displays the version.")
@Description("Prints the vert.x core version used by the application.")
@Slf4j
public class VersionCommand extends DefaultCommand {

    private static String version;

    @Override
    public void run() throws CLIException {
        log.info(getVersion());
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
        try (InputStream is = io.vertx.core.impl.launcher.commands.VersionCommand.class.getClassLoader().getResourceAsStream("META-INF/serving/serving-version.txt")) {
            if (is == null) {
                throw new IllegalStateException("Cannot find serving-version.txt on classpath");
            }
            try (Scanner scanner = new Scanner(is, "UTF-8").useDelimiter("\\A")) {
                return version = scanner.hasNext() ? scanner.next().trim() : "";
            }
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }
}

