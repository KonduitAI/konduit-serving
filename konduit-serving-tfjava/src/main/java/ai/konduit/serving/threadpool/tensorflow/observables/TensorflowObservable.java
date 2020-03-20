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

package ai.konduit.serving.threadpool.tensorflow.observables;

import org.nd4j.linalg.api.ndarray.INDArray;

import java.util.Observer;

public interface TensorflowObservable {

    /**
     * Get input batches - and their associated input mask arrays, if any<br>
     * Note that usually the returned list will be of size 1 - however, in the batched case, not all inputs
     * can actually be batched (variable size inputs to fully convolutional net, for example). In these "can't batch"
     * cases, multiple input batches will be returned, to be processed
     *
     * @return List of pairs of input arrays and input mask arrays. Input mask arrays may be null.
     */
    INDArray[] getInputBatches();

    void addInput(INDArray[] inputs);


    void setOutputBatches(INDArray[] output);

    void addObserver(Observer observer);

    INDArray[] getOutput();

    Exception getOutputException();

    void setOutputException(Exception e);

}
