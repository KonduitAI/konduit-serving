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

package ai.konduit.serving.pipeline.step;

import ai.konduit.serving.config.Input;
import ai.konduit.serving.config.Output;
import ai.konduit.serving.pipeline.BasePipelineStep;
import ai.konduit.serving.util.ObjectMappers;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@Data
@SuperBuilder
@NoArgsConstructor
public class BertStep extends BasePipelineStep<BertStep> implements Serializable
{
    protected String modelPath, vocabPath;

    @Override
    public Input.DataFormat[] validInputTypes() {
        return new Input.DataFormat[]{
                Input.DataFormat.JSON
        };
    }

    @Override
    public Output.DataFormat[] validOutputTypes() {
        return new Output.DataFormat[]{
                Output.DataFormat.JSON
        };
    }

    @Override
    public String pipelineStepClazz() {
        return "ai.konduit.serving.pipeline.steps.BertStepRunner";
    }

    public static BertStep fromJson(String json){
        return ObjectMappers.fromJson(json, BertStep.class);
    }

    public static BertStep fromYaml(String yaml){
        return ObjectMappers.fromYaml(yaml, BertStep.class);
    }
}
