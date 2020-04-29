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

import ai.konduit.serving.config.MemMapConfig;
import ai.konduit.serving.config.ServingConfig;
import ai.konduit.serving.config.TextConfig;
import ai.konduit.serving.model.PythonConfig;
import ai.konduit.serving.model.TensorDataType;
import ai.konduit.serving.pipeline.PipelineStep;
import ai.konduit.serving.pipeline.step.ModelStep;
import ai.konduit.serving.pipeline.step.PythonStep;
import ai.konduit.serving.util.ObjectMappers;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.nd4j.shade.jackson.annotation.JsonAlias;
import org.nd4j.shade.jackson.annotation.JsonSetter;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@XmlRootElement
@Schema
public class InferenceConfiguration implements Serializable, TextConfig {

    @Singular
    private List<PipelineStep> steps;

    @JsonAlias({"serving"})
    private ServingConfig servingConfig;

    private MemMapConfig memMapConfig;

    @JsonSetter("steps")
    public void stepSetup(List<PipelineStep> steps) throws Exception {
        for(PipelineStep step : steps) {
            if(step instanceof PythonStep) {
                PythonStep pythonStep = (PythonStep) step;
                Map<String, PythonConfig> pythonConfigs = pythonStep.getPythonConfigs();

                if (pythonConfigs != null) {
                    for (String key : pythonConfigs.keySet()) {
                        if (pythonStep.getInputNames() == null || !pythonStep.getInputNames().contains(key))
                            pythonStep.step(key, pythonConfigs.get(key));
                    }
                }
            }

            if(step instanceof ModelStep) {
                ModelStep modelStep = (ModelStep) step;
                Map<String, TensorDataType> inputDataTypes = modelStep.getInputDataTypes();
                Map<String, TensorDataType> outputDataTypes = modelStep.getOutputDataTypes();

                if(inputDataTypes != null && !inputDataTypes.isEmpty()) {
                    modelStep.setInputNames(new ArrayList<>(inputDataTypes.keySet()));
                }

                if(outputDataTypes != null && !outputDataTypes.isEmpty()) {
                    modelStep.setOutputNames(new ArrayList<>(outputDataTypes.keySet()));
                }
            }
        }

        this.steps = steps;
    }

    /**
     * Create a configuration from a yaml string
     *
     * @param yaml the yaml to create from
     * @return the initialized object from the yaml content
     */
    public static InferenceConfiguration fromYaml(String yaml) {
        return ObjectMappers.fromYaml(yaml, InferenceConfiguration.class);
    }

    /**
     * Create a {@link InferenceConfiguration}
     * from a json string
     *
     * @param json the json to create the configuration from
     * @return InferenceConfiguration
     */
    public static InferenceConfiguration fromJson(String json) {
        return ObjectMappers.fromJson(json, InferenceConfiguration.class);
    }
}
