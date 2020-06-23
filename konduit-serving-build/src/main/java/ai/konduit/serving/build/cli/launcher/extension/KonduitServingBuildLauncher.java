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

package ai.konduit.serving.build.cli.launcher.extension;

import ai.konduit.serving.build.cli.launcher.extension.command.ProfileCommand;
import ai.konduit.serving.build.cli.launcher.extension.command.ServeBuildCommand;
import ai.konduit.serving.cli.launcher.KonduitServingLauncher;

public class KonduitServingBuildLauncher extends KonduitServingLauncher {

    public static void main(String[] args) {
        new KonduitServingBuildLauncher().exec(args);
    }

    @Override
    public void setMainCommands() {
        super.setMainCommands();

        this.register(ServeBuildCommand.class, ServeBuildCommand::new)
            .register(ProfileCommand.class, ProfileCommand::new);
    }
}
