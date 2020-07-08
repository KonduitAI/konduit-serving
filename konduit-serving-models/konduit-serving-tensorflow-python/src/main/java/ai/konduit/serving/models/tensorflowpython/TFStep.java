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


package ai.konduit.serving.models.tensorflowpython;

import ai.konduit.serving.annotation.json.JsonName;
import ai.konduit.serving.annotation.runner.CanRun;
import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.ValueType;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunnerFactory;
import ai.konduit.serving.pipeline.util.DataUtils;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.nd4j.common.base.Preconditions;
import org.nd4j.python4j.PythonGIL;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@lombok.Data
@Accessors(fluent = true)
@JsonName("TF")
@NoArgsConstructor
public class TFStep implements PipelineStep {
    private String modelUri;
    private Map<String, String> inputKeyMap;
    private String inputKey;
    private Map<String, String> outputKeyMap;
    private String outputKey;
    public static class Factory implements PipelineStepRunnerFactory{
        @Override
        public boolean canRun(PipelineStep step) {
            return step instanceof TFStep;
        }
        @Override
        public PipelineStepRunner create(PipelineStep step) {
            Preconditions.checkState(step instanceof TFStep, "Unable to run step of type %s", step);
            return new TFStep.Runner((TFStep) step);
        }
    }

    @CanRun(TFStep.class)
    public static class Runner implements PipelineStepRunner{
        private final TFStep step;
        private final TFModel model;
        private final Map<String, String> outputKeyMap;
        private final Map<String, String> inputKeyMap;

        private void validateStep() {
            if (step.inputKeyMap == null || step.inputKeyMap.isEmpty()) {
                if (model.numInputs() > 1) {
                    throw new IllegalArgumentException("Error in TFStep: Keras model has multiple inputs, but input keys were not provided.");
                }
            } else if (step.inputKeyMap.size() != model.numInputs()) {
                throw new IllegalArgumentException("Error in KerasStep: Keras model has " + model.numInputs() + " inputs but " + step.inputKeyMap.size() + " input keys were provided.");
            }
            if (step.outputKeyMap == null || step.outputKeyMap.isEmpty()) {
                if (step.outputKey != null){
                    if (model.numOutputs() > 1){
                        throw new IllegalArgumentException("Error in TFStep: Model has multiple outputs. Provide outputKeyMap.");
                    }
                }else{
                    throw new IllegalArgumentException("Error in TFStep: outputKeyMap not specified");
                }

            } else if (step.outputKeyMap.size() != model.numOutputs()) {
                throw new IllegalArgumentException("Error in TFStep: Model has " + model.numOutputs() + " outputs but " + step.outputKeyMap.size() + " output keys were provided.");
            }
        }
        public Runner(TFStep step) {
            try (PythonGIL gil = PythonGIL.lock()) {
                this.step = step;
                this.model = new TFModel(step.modelUri);
                validateStep();
                if (step.outputKeyMap == null){
                    this.outputKeyMap = Collections.singletonMap(this.model.outputNames()[0], step.outputKey);
                }else{
                    this.outputKeyMap = step.outputKeyMap;
                }
                if (step.inputKeyMap == null && step.inputKey != null){
                    this.inputKeyMap = Collections.singletonMap(this.model.inputNames()[0], step.inputKey);
                }else{
                    this.inputKeyMap = step.inputKeyMap;
                }
            }
        }

        @Override
        public Data exec(Context ctx, Data input) {
            try (PythonGIL gil = PythonGIL.lock()) {
                Map<String, NumpyArray> inputArrays;
                if (step.inputKeyMap == null || step.inputKeyMap.isEmpty()) {
                    String errMultipleKeys = "Error in TFStep: Multiple NDarray values (%s and %s) received for single input model. Specify input key explicitly.";
                    String errNoKeys = "Error in TFStep: No NDarray values received.";
                    String key = DataUtils.inferField(input, ValueType.NDARRAY, false, errMultipleKeys, errNoKeys);
                    inputArrays = Collections.singletonMap(model.inputNames()[0], input.getNDArray(key).getAs(NumpyArray.class));

                } else {
                    inputArrays = new HashMap<>();
                    for (Map.Entry<String, String> e: this.inputKeyMap.entrySet()){
                        inputArrays.put(e.getKey(), input.getNDArray(e.getValue()).getAs(NumpyArray.class));
                    }
                }
                Map<String, NumpyArray> out = model.predict(inputArrays);
                for(Map.Entry<String, String> e: outputKeyMap.entrySet()){
                    input.put(e.getValue(), new NumpyArray.NumpyNDArray(out.get(e.getKey())));
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
