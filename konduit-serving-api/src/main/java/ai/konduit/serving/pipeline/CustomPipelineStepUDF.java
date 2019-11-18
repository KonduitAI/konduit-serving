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

import org.datavec.api.records.Record;

/**
 * A user defined function for use with the
 * {@link CustomPipelineStep}. This is where
 * a user defines an extension for use within a
 * {@link CustomPipelineStep}.
 *
 * A user defined function should take in an
 * array of {@link Record} and output an array of {@link Record}
 * the records should be 1 input per name and 1 output per output name.
 * Generally, this will just be an array of length 1 reflecting the
 * "default" input name and "default" output name.
 *
 * Multiple names are generally used for more complex neural networks.
 *
 * @author Adam Gibson
 */
public interface CustomPipelineStepUDF {


    /**
     * The user defined function where a user can define custom behavior
     * for a {@link CustomPipelineStep}
     * @param input the input records (generally 1 record per input name for the pipeline step)
     * @return the output records (generally 1 record per output name for the pipeline step)
     */
    Record[] udf(Record[] input);

}
