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

package ai.konduit.serving.pipeline.step.model;

import ai.konduit.serving.pipeline.step.ModelStep;
import ai.konduit.serving.util.ObjectMappers;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import org.nd4j.tensorflow.conversion.graphrunner.SavedModelConfig;

/**
 * TensorFlow extension of {@link ModelStep}.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(fluent = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TensorFlowStep extends ModelStep {

    public static final String TENSORFLOW_EXECUTION_CONFIG_KEY = "tensorFlowConfig";
    private String configProtoPath;
    private SavedModelConfig savedModelConfig;

    public static TensorFlowStep fromJson(String json){
        return ObjectMappers.fromJson(json, TensorFlowStep.class);
    }

    public static TensorFlowStep fromYaml(String yaml){
        return ObjectMappers.fromYaml(yaml, TensorFlowStep.class);
    }

    @Override
    public String getInferenceExecutionerFactoryClassName() {
        return "ai.konduit.serving.executioner.inference.factory.TensorflowInferenceExecutionerFactory";
    }
}