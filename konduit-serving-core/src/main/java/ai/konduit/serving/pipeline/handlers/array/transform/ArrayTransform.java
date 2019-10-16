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

package ai.konduit.serving.pipeline.handlers.array.transform;

import ai.konduit.serving.pipeline.PipelineStepRunner;

/**
 * Run a transform using a computation graph
 * on a set of input arrays.
 *
 *
 */
public interface ArrayTransform extends PipelineStepRunner {



    /**
     * Set the input names for the array transform
     * @param inputNames the input names
     *                   for the graph to transform
     */
    void setInputNames(String...inputNames);

    /**
     * Set the output names for the array transform
     * @param outputNames the output names for the
     *                    graph to transform
     */
    void setOutputNames(String...outputNames);

    /**
     * Returns the input names for the graph
     * used to transform input arrays
     * @return the input names for the transform
     */
    String[] inputNames();

    /**
     * Returns the output names for the graph
     * used to transform output arrays
     * @return the output names for the transform
     */
    String[] outputNames();

}
