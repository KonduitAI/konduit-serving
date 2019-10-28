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

import ai.konduit.serving.config.SchemaType;
import org.datavec.api.records.Record;
import org.datavec.api.writable.NDArrayWritable;
import org.datavec.api.writable.Writable;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.util.List;
import java.util.Map;


/**
 * Pipeline steps represent a component
 * in pre processing data ending
 * in data in an ndarray form.
 *
 * @author Adam Gibson
 */
public interface PipelineStepRunner {

    /**
     * Destroy the pipeline runner
     */
    void destroy();

    /**
     * Returns the expected input types
     * for this step
     * @return pipeline step
     */
    Map<String, SchemaType[]> inputTypes();

    /**
     * Returns the expected output types
     * for this step
     * @return pipeline step
     */
    Map<String, SchemaType[]> outputTypes();

    /**
     * Transform a set of {@link INDArray}
     * via this operation.
     * @param input the input array
     * @return the output from the transform
     */
    NDArrayWritable[][] transform(Writable[]... input);

    /**
     * Transform a set of {@link INDArray}
     * via this operation.
     * @param input the input array
     * @return the output from the transform
     */
    Record[] transform(Record[] input);
}
