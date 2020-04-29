/*
 *
 *  * ******************************************************************************
 *  *  * Copyright (c) 2020 Konduit AI.
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

package ai.konduit.serving.threadpool.onnx.observables;

import org.nd4j.linalg.api.ndarray.INDArray;

import java.util.List;
import java.util.Map;
import java.util.Observer;

public interface OnnxObservable {

    /**
     * Get input batches
     * Note that usually the returned list will be of size 1 - however, in the batched case, not all inputs
     * can actually be batched (variable size inputs to fully convolutional net, for example). In these "can't batch"
     * cases, multiple input batches will be returned, to be processed
     *
     * @return List of pairs of input arrays and input mask arrays. Input mask arrays may be null.
     */
    List<Map<String, INDArray>> getInputBatches();

    void addInput(List<Map<String, INDArray>> inputs);

    void setOutputBatches(List<Map<String, INDArray>> output);

    void addObserver(Observer observer);

    List<Map<String, INDArray>> getOutput();

    Exception getOutputException();

    void setOutputException(Exception e);
}
