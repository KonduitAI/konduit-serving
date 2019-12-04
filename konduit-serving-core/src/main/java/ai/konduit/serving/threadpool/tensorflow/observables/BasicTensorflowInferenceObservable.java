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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.util.Observable;

/**
 * This class holds reference input, and implements basic use case: SEQUENTIAL inference
 */
@Slf4j
public class BasicTensorflowInferenceObservable extends Observable implements TensorflowObservable {

    protected Exception exception;
    private INDArray[] input;
    @Getter
    private long id;
    private INDArray[] output;


    public BasicTensorflowInferenceObservable(INDArray[] inputs) {
        super();
        this.input = inputs;
    }


    @Override
    public INDArray[] getInputBatches() {
        return input;
    }

    @Override
    public void addInput(INDArray[] inputs) {
        this.input = inputs;
    }

    @Override
    public void setOutputBatches(INDArray[] output) {
        this.output = output;
        this.setChanged();
        notifyObservers();
    }

    @Override
    public INDArray[] getOutput() {
        checkOutputException();
        return output;
    }

    @Override
    public Exception getOutputException() {
        return exception;
    }

    @Override
    public void setOutputException(Exception exception) {
        this.exception = exception;
        this.setChanged();
        notifyObservers();
    }

    protected void checkOutputException() {
        if (exception != null) {
            if (exception instanceof RuntimeException) {
                throw (RuntimeException) exception;
            } else {
                throw new RuntimeException("Exception encountered while getting output: " + exception.getMessage(), exception);
            }
        }
    }
}
