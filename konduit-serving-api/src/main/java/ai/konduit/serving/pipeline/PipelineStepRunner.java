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

package ai.konduit.serving.pipeline;

import ai.konduit.serving.pipeline.step.CustomPipelineStep;
import org.datavec.api.records.Record;
import org.datavec.api.writable.Writable;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.io.Closeable;
import java.util.Map;


/**
 * Pipeline steps represent a component
 * in pre processing data ending
 * in data in an {@link INDArray} form.
 * <p>
 * A runner is the actual implementation
 * of the {@link BasePipelineStep}
 * which is just a configuration interface
 * for a runner.
 * <p>
 * A runner takes in 1 or more input
 * {@link Record} and returns 1 or more output {@link Record}.
 * <p>
 * There are a  number of implementations. You can also create a custom one
 * using the {@link CustomPipelineStep} and {@link CustomPipelineStepUDF}
 * definitions. This is recommended as the easiest way of creating your own custom ones.
 * Otherwise, we try to provide any number of off the shelf ones
 * for running python scripts or machine learning models.
 *
 * @author Adam Gibson
 * @deprecated To be replaced by {@link ai.konduit.serving.pipeline.api.step.PipelineStepRunner} - see https://github.com/KonduitAI/konduit-serving/issues/298
 */
@Deprecated
public interface PipelineStepRunner extends Closeable {

    /**
     * Destroy the pipeline runner.
     * <p>
     * This means cleaning up used resources.
     * This method will be called when a pipeline needs to be finalized.
     */
    void close();

    PipelineStep<?> getPipelineStep();

    /**
     * Transform a set of {@link Object}
     * via this operation.
     *
     * @param input the input array
     * @return the output from the transform
     */
    Writable[][] transform(Object... input);

    /**
     * Transform a set of {@link Object}
     * via this operation.
     *
     * @param input the input array
     * @return the output from the transform
     */
    Writable[][] transform(Object[][] input);

    /**
     * Transform a set of {@link INDArray}
     * via this operation.
     *
     * @param input the input array
     * @return the output from the transform
     */
    Record[] transform(Record[] input);

    static Object someMethod(Map input) {
        return null;
    }
}