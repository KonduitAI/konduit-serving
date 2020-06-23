/*
 * *****************************************************************************
 * Copyright (c) 2020 Konduit K.K.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ****************************************************************************
 */

package ai.konduit.serving.build.cli.launcher.extension.command;

import io.vertx.core.cli.CLIException;
import io.vertx.core.cli.annotations.Description;
import io.vertx.core.cli.annotations.Name;
import io.vertx.core.cli.annotations.Summary;
import io.vertx.core.spi.launcher.DefaultCommand;
import lombok.extern.slf4j.Slf4j;

@Name("profile")
@Summary("Command to List, view, edit, create and delete konduit serving run profiles.")
@Description("A utility command to create, view, edit, list and delete konduit serving run profiles. Run profiles " +
        "configures the background run architecture such as CPU, GPU (CUDA). Konduit serving tries to identify the " +
        "best profiles during the first server launch but you can manage your own profile configurations with this " +
        "command. \n\n"+
        "Example usages:\n" +
        "--------------\n" +
        "- Creates a CUDA profile with the name 'CUDA-10.1' with the installation \n" +
        "  path given:\n" +
        "$ konduit profile create -t CUDA -n CUDA-10.1 -p /usr/local/cuda\n\n" +
        "- Creates a CUDA profile with the name 'CUDA-10.2' using the \n" +
        "  cuda redist package (no pre CUDA installation required):\n" +
        "$ konduit profile create -t CUDA-redist-10.2 -n CUDA-10.2\n\n" +
        "- Creates a simple CPU profile for x86_avx2 architecture with name 'CPU-1':\n" +
        "$ konduit profile create -t x86_avx2 -n CPU-1\n\n" +
        "- Listing all the profiles:\n" +
        "$ konduit profile list\n\n" +
        "- Viewing a profile:\n" +
        "$ konduit profile view CPU-1\n\n" +
        "- Edit a profile with name 'CPU-1' from old type to 'x86':\n" +
        "$ konduit profile edit -n CPU-1 -t x86 \n\n" +
        "--------------")
@Slf4j
public class ProfileCommand extends DefaultCommand {
    @Override
    public void run() throws CLIException {

    }
}
