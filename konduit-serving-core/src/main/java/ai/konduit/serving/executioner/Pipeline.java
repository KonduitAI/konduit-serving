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

package ai.konduit.serving.executioner;

import ai.konduit.serving.pipeline.BasePipelineStep;
import ai.konduit.serving.pipeline.PipelineStep;
import ai.konduit.serving.pipeline.PipelineStepRunner;
import ai.konduit.serving.util.SchemaTypeUtils;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import org.datavec.api.records.Record;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.util.List;

/**
 * Run a pipeline. A pipeline
 * is just runs a sequence
 * of {@link PipelineStepRunner}
 * created from a set of {@link BasePipelineStep}
 *
 * @deprecated To be replaced by {@link ai.konduit.serving.pipeline.api.pipeline.Pipeline} - see https://github.com/KonduitAI/konduit-serving/issues/298
 */
@Builder
@Deprecated
public class Pipeline {

    @Singular
    @Getter
    private List<PipelineStepRunner> steps;


    /**
     * Create a pipeline from a list of pipeline steps.
     * All this does is calls a constructor present on each {@link PipelineStepRunner}
     * that takes in a parameter of {@link BasePipelineStep}
     * and adds it to the {@link #steps}
     * list in a created Pipeline instance
     *
     * @param configurations the list of {@link BasePipelineStep}
     *                       to create a pipeline from
     * @return the created pipeline
     */
    public static Pipeline getPipeline(List<PipelineStep> configurations) {
        PipelineBuilder builder = Pipeline.builder();
        for (PipelineStep config : configurations) {
            builder = builder.step(config.createRunner());
        }

        return builder.build();
    }

    public void close() {
        for (PipelineStepRunner pipelineStepRunner : steps) {
            pipelineStepRunner.close();
        }
    }

    /**
     * Executes a pipeline on a set of input {@link Record}
     *
     * @param inputs the array of records (one "row" per input).
     * @return the output set of records
     */
    public Record[] doPipeline(Record[] inputs) {
        for (PipelineStepRunner pipelineStepRunner : steps)
            inputs = pipelineStepRunner.transform(inputs);

        return inputs;
    }


    /**
     * Runs a pipeline an a set of {@link INDArray}
     * See {@link SchemaTypeUtils#toArrays(Record[])}
     * for the format of the input that goes in to
     * the pipeline
     *
     * @param inputs the input {@link INDArray}
     * @return an extracted set of {@link INDArray}
     * from a set of {@link Record}
     */
    public INDArray[] doPipelineArrays(Record[] inputs) {
        return SchemaTypeUtils.toArrays(doPipeline(inputs));
    }


}
