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
package ai.konduit.serving.models.samediff.step.trainer;

import ai.konduit.serving.annotation.runner.CanRun;
import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.data.ValueType;
import ai.konduit.serving.pipeline.api.exception.ModelLoadingException;
import ai.konduit.serving.pipeline.api.protocol.URIResolver;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import lombok.SneakyThrows;
import org.nd4j.autodiff.listeners.At;
import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.autodiff.samediff.TrainingConfig;
import org.nd4j.autodiff.samediff.internal.InferenceSession;
import org.nd4j.common.base.Preconditions;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.MultiDataSet;

import java.io.File;
import java.util.*;

@CanRun(SameDiffTrainerStep.class)
public class SameDiffTrainerRunner implements PipelineStepRunner {


    private SameDiffTrainerStep step;
    private final SameDiff sd;

    public SameDiffTrainerRunner(SameDiffTrainerStep step) {
        this.step = step;

        String uri = step.modelUri();
        Preconditions.checkState(uri != null && !uri.isEmpty(), "No model URI was provided (model URI was null or empty)");

        try {
            File f = URIResolver.getFile(uri);
            Preconditions.checkState(f.exists(), "No model file exists at URI: %s", uri);
            sd = SameDiff.load(f, true);
            TrainingConfig.Builder builder = TrainingConfig.builder();
            if(step.l1() > 0) {
                builder.l1(step.l1());
            }

            if(step.updater() != null)
                builder.updater(step.updater());

            if(step.l2() > 0) {
                builder.l2(step.l2());
            }

            if(step.lossVariables() != null && !step.lossVariables().isEmpty()) {
                builder.minimize(step.lossVariables().toArray(new String[step.lossVariables().size()]));
            }

            if(step.weightDecayCoefficient() > 0) {
                builder.weightDecay(step.weightDecayCoefficient(), step.weightDecayApplyLearningRate());
            }

            Preconditions.checkState(sd.inputs() != null && !sd.inputs().isEmpty(),"Model inputs must not be empty! Please specify inputs on the same diff model.");
            builder.dataSetFeatureMapping(sd.inputs().toArray(new String[sd.inputs().size()]));
            Preconditions.checkState(step.lossVariables() != null && !step.lossVariables().isEmpty(),"No loss variables for training found! Please specify loss variables on the training step.");
            builder.dataSetLabelMapping(step.lossVariables());

            sd.setTrainingConfig(builder
                    .build());
        } catch (Throwable e) {
            throw new ModelLoadingException("Failed to load SameDiff model from URI " + step.modelUri(), e);
        }
    }


    @Override
    public void close() {

    }

    @Override
    public PipelineStep getPipelineStep() {
        return step;
    }

    @SneakyThrows
    @Override
    public Data exec(Context ctx, Data data) {
        List<String> inputs = sd.inputs();
        List<INDArray> inputArrays = new ArrayList<>();
        List<INDArray> labels = new ArrayList<>();
        for(String s : inputs) {
            if(!data.has(s))
                throw new IllegalStateException("Expected to find NDArray with name \"" + s + "\" in data - not found. Data keys: " + data.keys());
            if(data.type(s) != ValueType.NDARRAY)
                throw new IllegalStateException("Input Data field \"" + s + "\" is not an NDArray - is type : " + data.type(s));
            INDArray arr = data.getNDArray(s).getAs(INDArray.class);
            inputArrays.add(arr);
        }

        for(String s : step.lossVariables()) {
            INDArray arr = data.getNDArray(s).getAs(INDArray.class);
            labels.add(arr);
        }

        MultiDataSet multiDataSet = new MultiDataSet(inputArrays.toArray(new INDArray[inputArrays.size()]), labels.toArray(new INDArray[labels.size()]));


        List<String> outNames = step.lossVariables();
        Preconditions.checkState(outNames != null && !outNames.isEmpty(), "No output names were provided in the SameDiffStep configuration");
        sd.fit(multiDataSet);

        if(step.modelSaveOutputPath() != null)
            sd.save(new File(step.modelSaveOutputPath()),true);
        Data d = Data.empty();


        return d;
    }
}
