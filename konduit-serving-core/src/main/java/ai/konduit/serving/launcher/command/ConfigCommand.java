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
import io.vertx.core.cli.annotations.*;
import io.vertx.core.spi.launcher.DefaultCommand;

import java.io.File;

@Name("config")
@Summary("A helper command for creating configuration JSON")
@Description("This command is a utility to create a boilerplate configuration that can be used to start konduit servers.")
public class ConfigCommand extends DefaultCommand {

    private enum ConfigType {
        boilerplate,
        image,
        python,
        model,
        tensorflow,
        onnx,
        pmml,
        dl4j,
        keras
    }

    private ConfigType type;
    private boolean pretty;
    private File outputFile;

    @Option(longName = "type", shortName = "t", argName = "config-type")
    @DefaultValue("boilerplate")
    public void setType(String type) {
        this.type = ConfigType.valueOf(type);
    }
)
    @Option(longName = "pretty", shortName = "p", flag = true)
    @DefaultValue("false")
    public void setPretty(boolean pretty) {
        this.pretty = pretty;
    }

    @Option(longName = "--output", shortName = "-o", argName = "output-file")
    public void setOutputFile(String outputFile) {
        this.outputFile = new File(outputFile);
        if (this.outputFile.isDirectory() ||
                !this.outputFile.getParentFile().exists()) {
            throw new CLIException(String.format("The file %s is not a valid file location", outputFile));
        }
    }

    @Override
    public void run() throws CLIException {

    }

}
