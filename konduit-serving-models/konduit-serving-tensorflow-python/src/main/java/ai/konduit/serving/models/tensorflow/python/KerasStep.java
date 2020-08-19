/*
 *  ******************************************************************************
 *  * Copyright (c) 2020 Konduit K.K.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Apache License, Version 2.0 which is available at
 *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  * License for the specific language governing permissions and limitations
 *  * under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *  *****************************************************************************
 */


package ai.konduit.serving.models.tensorflow.python;

import ai.konduit.serving.annotation.json.JsonName;
import ai.konduit.serving.annotation.runner.CanRun;
import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.ValueType;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunnerFactory;
import ai.konduit.serving.pipeline.util.DataUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.nd4j.common.base.Preconditions;
import org.nd4j.python4j.PythonGIL;
import org.nd4j.shade.jackson.annotation.JsonProperty;


/**
 * A pipeline step that configures a Keras model that is to be executed.
 */
@lombok.Data
@Accessors(fluent = true)
@JsonName("KERAS")
@NoArgsConstructor
@Schema(description = "A pipeline step that configures a Keras model that is to be executed.")
public class KerasStep implements PipelineStep {

    @Schema(description = "Path to Keras model (.h5 file or saved model directory).")
    private String modelUri;
    @Schema(description = "Names of NDArray fields which should be input to the model.")
    private String[] inputKeys;
    @Schema(description = "Names of output fields to which output arrays will be written).")
    private String[] outputKeys;


    /**
     *
     * @param modelUri Path to Keras model (.h5 file or saved model directory).
     * @param inputKeys Names of NDArray fields which should be input to the model.
     * @param outputKeys Names of output fields to which output arrays will be written.
     */
    public KerasStep(@JsonProperty("modelUri") String modelUri, @JsonProperty("inputKeys") String[] inputKeys,
                     @JsonProperty("outputKeys") String[] outputKeys){
        this.modelUri = modelUri;
        this.inputKeys = inputKeys;
        this.outputKeys = outputKeys;
    }


    public static class Factory implements PipelineStepRunnerFactory {
        @Override
        public boolean canRun(PipelineStep step) {
            return step instanceof KerasStep;
        }

        @Override
        public PipelineStepRunner create(PipelineStep step) {
            Preconditions.checkState(step instanceof KerasStep, "Unable to run step of type %s", step);
            return new Runner((KerasStep) step);
        }
    }

    @CanRun(KerasStep.class)
    public static class Runner implements PipelineStepRunner {
        private final KerasStep step;
        private final KerasModel model;

        private void validateStep() {
            if (step.inputKeys == null || step.inputKeys.length == 0) {
                if (model.numInputs() > 1) {
                    throw new IllegalArgumentException("Error in KerasStep: Keras model has multiple inputs, but input keys were not provided.");
                }
            } else if (step.inputKeys.length != model.numInputs()) {
                throw new IllegalArgumentException("Error in KerasStep: Keras model has " + model.numInputs() + " inputs but " + step.inputKeys.length + " input keys were provided.");
            }
            if (step.outputKeys == null || step.outputKeys.length == 0) {
                throw new IllegalArgumentException("Error in KerasStep: Output keys not specified");
            } else if (step.outputKeys.length != model.numOutputs()) {
                throw new IllegalArgumentException("Error in KerasStep: Keras model has " + model.numOutputs() + " outputs but " + step.outputKeys.length + " output keys were provided.");
            }
        }

        public Runner(KerasStep step) {
            try (PythonGIL gil = PythonGIL.lock()) {
                this.step = step;
                this.model = new KerasModel(step.modelUri);
                validateStep();
            }
        }

        @Override
        public Data exec(Context ctx, Data input) {
            try (PythonGIL gil = PythonGIL.lock()) {
                NumpyArray[] inputArrays;
                if (step.inputKeys == null || step.inputKeys.length == 0) {
                    String errMultipleKeys = "Error in KerasStep: Multiple NDarray values (%s and %s) received for single input model. Specify input key explicitly.";
                    String errNoKeys = "Error in KerasStep: No NDarray values received.";
                    String key = DataUtils.inferField(input, ValueType.NDARRAY, false, errMultipleKeys, errNoKeys);
                    inputArrays = new NumpyArray[]{input.getNDArray(key).getAs(NumpyArray.class)};

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
            model.close();
        }

        @Override
        public PipelineStep getPipelineStep() {
            return step;
        }
    }
}
