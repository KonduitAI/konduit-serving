/* ******************************************************************************
 * Copyright (c) 2020 Konduit K.K.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/


package ai.konduit.serving;

import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.data.ValueType;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunnerFactory;
import lombok.experimental.Accessors;
import org.nd4j.python4j.PythonGIL;


@lombok.Data
@Accessors(fluent = true)
public class KerasStep implements PipelineStep {
    private String modelPath;
    private String[] inputKeys;
    private String[] outputKeys;

    public static class Factory implements PipelineStepRunnerFactory {
        @Override
        public boolean canRun(PipelineStep step) {
            return step instanceof KerasStep;
        }

        @Override
        public PipelineStepRunner create(PipelineStep step) {
            return new Runner((KerasStep) step);
        }
    }

    public static class Runner implements PipelineStepRunner {
        private final KerasStep step;
        private final KerasModel model;

        private void validateStep() {
            if (step.inputKeys == null || step.inputKeys.length == 0) {
                if (model.numInputs() > 1) {
                    throw new IllegalArgumentException("Keras model has multiple inputs, but input keys were not provided.");
                }
            } else if (step.inputKeys.length != model.numInputs()) {
                throw new IllegalArgumentException("Keras model has " + model.numInputs() + " inputs but " + step.inputKeys.length + " input keys were provided.");
            }
            if (step.outputKeys == null || step.outputKeys.length == 0) {
                throw new IllegalArgumentException("Output keys not specified");
            } else if (step.outputKeys.length != model.numOutputs()) {
                throw new IllegalArgumentException("Keras model has " + model.numOutputs() + " outputs but " + step.outputKeys.length + " output keys were provided.");
            }
        }

        public Runner(KerasStep step) {
            try (PythonGIL gil = PythonGIL.lock()) {
                this.step = step;
                this.model = new KerasModel(step.modelPath);
                validateStep();
            }
        }

        @Override
        public Data exec(Context ctx, Data input) {
            try (PythonGIL gil = PythonGIL.lock()) {
                NumpyArray[] inputArrays;
                if (step.inputKeys == null || step.inputKeys.length == 0) {
                    inputArrays = new NumpyArray[1];
                    int numInputArrays = 0;
                    for (String key : input.keys()) {
                        if (input.type(key) == ValueType.NDARRAY) {
                            inputArrays[0] = input.getNDArray(key).getAs(NumpyArray.class);
                            numInputArrays += 1;
                        }
                    }
                    if (numInputArrays == 0) {
                        throw new IllegalArgumentException("No NDarray values received..");
                    } else if (numInputArrays > 1) {
                        throw new IllegalArgumentException("Multiple NDarray values received for single input model. Specify input key explicitly.");
                    }
                } else {
                    inputArrays = new NumpyArray[step.inputKeys.length];
                    for (int i = 0; i < inputArrays.length; i++) {
                        inputArrays[i] = input.getNDArray(step.inputKeys[i]).getAs(NumpyArray.class);
                    }
                }
                NumpyArray[] out = model.predict(inputArrays);
                for (int i = 0; i < step.outputKeys.length; i++) {
                    input.put(step.outputKeys[i], new NumpyArray.NumpyNDArray(out[i]));
                }
                return input;
            }
        }


        @Override

        public void close() {

        }

        @Override
        public PipelineStep getPipelineStep() {
            return step;
        }
    }
}
