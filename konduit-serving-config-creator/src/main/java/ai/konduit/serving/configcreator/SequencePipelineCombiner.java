/*
 *  ******************************************************************************
 *  *
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Apache License, Version 2.0 which is available at
 *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  *  See the NOTICE file distributed with this work for additional
 *  *  information regarding copyright ownership.
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  * License for the specific language governing permissions and limitations
 *  * under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *  *****************************************************************************
 */
package ai.konduit.serving.configcreator;

import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import ai.konduit.serving.pipeline.util.ObjectMappers;
import org.apache.commons.io.FileUtils;
import org.nd4j.shade.jackson.databind.ObjectMapper;
import picocli.CommandLine;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "sequence-pipeline-creator",mixinStandardHelpOptions = true,description = "Combine a list of pipeline json or yaml files (specified by file format) together to form a pipeline.")
public class SequencePipelineCombiner implements Callable<Void> {
    @CommandLine.Option(names = {"--pipeline"},description = "Pipeline String",required = true)
    private List<File> pipelineStep;
    @CommandLine.Option(names = {"--file-format"},description = "Pipeline String")
    private String format = "json";
    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec; // injected by picocli


    private ObjectMapper jsonMapper = ObjectMappers.json();
    private ObjectMapper yamlMapper = ObjectMappers.yaml();

    @Override
    public Void call() throws Exception {
        SequencePipeline.Builder pipelineBuilder = SequencePipeline.builder();
        for(File f : pipelineStep) {
            if(format.equals("json")) {
                PipelineStep pipelineStep = jsonMapper.readValue(f, PipelineStep.class);
                pipelineBuilder.add(pipelineStep);
            } else if(format.equals("yml") || format.equals("yaml")) {
                PipelineStep pipelineStep = yamlMapper.readValue(f,PipelineStep.class);
                pipelineBuilder.add(pipelineStep);
            }
        }
        if(format.equals("json")) {
            spec.commandLine().getOut().println(pipelineBuilder.build().toJson());
        } else if(format.equals("yml") || format.equals("yaml")) {
            spec.commandLine().getOut().println(pipelineBuilder.build().toYaml());
        } else {
            System.err.println("Invalid format: please specify json,yml,yaml");
        }

        return null;
    }


}
