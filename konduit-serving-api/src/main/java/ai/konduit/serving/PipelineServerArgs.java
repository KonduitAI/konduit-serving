/*
 *
 *  * ******************************************************************************
 *  *  * Copyright (c) 2015-2019 Skymind Inc.
 *  *  * Copyright (c) 2019 Konduit AI.
 *  *  *
 *  *  * This program and the accompanying materials are made available under the
 *  *  * terms of the Apache License, Version 2.0 which is available at
 *  *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  *  * License for the specific language governing permissions and limitations
 *  *  * under the License.
 *  *  *
 *  *  * SPDX-License-Identifier: Apache-2.0
 *  *  *****************************************************************************
 *
 *
 */

package ai.konduit.serving;

import com.beust.jcommander.Parameter;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Data
@EqualsAndHashCode()
@ToString(callSuper = true)
public class PipelineServerArgs {

    @Parameter(
            names = {"--configHost"},
            help = true,
            description = "The host for downloading the configuration from"
    )
    private String configHost;
    @Parameter(
            names = {"--configPort"},
            help = true,
            description = "The port for downloading the configuration from"
    )
    private int configPort;
    @Parameter(
            names = {"--configStoreType"},
            help = true,
            description = "The configuration store type (usually http or file) where the configuration is stored"
    )
    private String configStoreType;
    @Parameter(
            names = {"--configPath"},
            help = true,
            description = "The path to the configuration. With http, this will be the path after host:port. " +
                    "With files, this will be an absolute path."
    )
    private String configPath;
    @Parameter(
            names = {"--workerNode"},
            help = true,
            description = "Whether this is a worker node or not"
    )
    private boolean workerNode = true;
    @Parameter(
            names = {"--ha"},
            help = true,
            description = "Whether this node is deployed as Highly available or not."
    )
    private boolean ha = false;
    @Parameter(
            names = {"--numInstances"},
            help = true,
            description = "The number of instances to deploy of this verticle."
    )
    private int numInstances = 1;
    @Parameter(
            names = {"--workerPoolSize"},
            help = true,
            description = "The number of workers for use with this verticle."
    )
    private int workerPoolSize = 1;
    @Parameter(
            names = {"--verticleClassName"},
            help = true,
            description = "The fully qualified class name to the verticle to be used."
    )
    private String verticleClassName;
    @Parameter(
            names = {"--vertxWorkingDirectory"},
            help = true,
            description = "The absolute path to use for vertx. This defaults to the user's home directory."
    )
    private String vertxWorkingDirectory = System.getProperty("user.home");

    @java.beans.ConstructorProperties({"configHost",
            "configPort",
            "configStoreType",
            "configPath",
            "workerNode",
            "ha",
            "numInstances",
            "workerPoolSize",
            "verticleClassName",
            "vertxWorkingDirectory"})
    @Builder
    public PipelineServerArgs(String configHost,
                              int configPort,
                              String configStoreType,
                              String configPath,
                              boolean workerNode,
                              boolean ha,
                              int numInstances,
                              int workerPoolSize,
                              String verticleClassName,
                              String vertxWorkingDirectory) {
        this.configHost = configHost;
        this.configPort = configPort;
        this.configStoreType = configStoreType;
        this.configPath = configPath;
        this.workerNode = workerNode;
        this.ha = ha;
        this.numInstances = numInstances;
        this.workerPoolSize = workerPoolSize;
        this.verticleClassName = verticleClassName;
        this.vertxWorkingDirectory = vertxWorkingDirectory;
    }

    public PipelineServerArgs() {
    }

    public String[] toArgs() {
        List<String> ret2 = new ArrayList<>(Collections.emptyList());

        if(configHost != null && !configHost.isEmpty()) {
            ret2.add("--configHost");
            ret2.add(configHost);
        }

        if(configPort > 0) {
            ret2.add("--configPort");
            ret2.add(String.valueOf(configPort));
        }

        if(configStoreType != null && !configStoreType.isEmpty())  {
            ret2.add("--configStoreType");
            ret2.add(configStoreType);
        }

        if(configPath != null && !configPath.isEmpty()) {
            ret2.add("--configPath");
            ret2.add(configPath);
        }

        if(numInstances > 0) {
            ret2.add("--numInstances");
            ret2.add(String.valueOf(numInstances));
        }
        else {
            ret2.add("--numInstances");
            ret2.add(String.valueOf(1));
        }

        if(workerPoolSize > 0) {
            ret2.add("--workerPoolSize");
            ret2.add(String.valueOf(workerPoolSize));
        }
        else {
            ret2.add("--workerPoolSize");
            ret2.add(String.valueOf(1));
        }

        if(vertxWorkingDirectory != null && !vertxWorkingDirectory.isEmpty()) {
            ret2.add("--vertxWorkingDirectory");
            ret2.add(vertxWorkingDirectory);
        }

        if(ha) {
            ret2.add("--ha");
        }

        if(verticleClassName != null && !verticleClassName.isEmpty()) {
            ret2.add("--verticleClassName");
            ret2.add(verticleClassName);
        }

        if(workerNode) {
            ret2.add("--workerNode");
        }

        return ret2.toArray(new String[ret2.size()]);
    }

}
