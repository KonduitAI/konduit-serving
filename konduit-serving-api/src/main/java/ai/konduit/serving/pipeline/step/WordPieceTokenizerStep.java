/*
 *
 *  * ******************************************************************************
 *  *  * Copyright (c) 2020 Konduit K.K.
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
import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;


/**
 * WordTokenizerStep defines custom texts tokenization step
 *
 * @author chia wei
 */
@Data
@SuperBuilder
public class WordPieceTokenizerStep extends BasePipelineStep<WordPieceTokenizerStep> implements Serializable
{
    protected String vocabPath;

    @Builder.Default
    protected Integer sentenceMaxLen = 256;

    public WordPieceTokenizerStep()
    {
        this.sentenceMaxLen = 256;
    }
    @Override
    public Input.DataFormat[] validInputTypes() {
        return new Input.DataFormat[]{
                Input.DataFormat.JSON
        };
    }

    @Override
    public Output.DataFormat[] validOutputTypes() {
        return new Output.DataFormat[]{
                Output.DataFormat.NUMPY
        };
    }

    @Override
    public String pipelineStepClazz() {
        return "ai.konduit.serving.pipeline.steps.WordPieceTokenizerStepRunner";
    }

    public static WordPieceTokenizerStep fromJson(String json){
        return ObjectMappers.fromJson(json, WordPieceTokenizerStep.class);
    }

    public static WordPieceTokenizerStep fromYaml(String yaml){
        return ObjectMappers.fromYaml(yaml, WordPieceTokenizerStep.class);
    }
}
