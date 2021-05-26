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

package ai.konduit.serving.models.nd4j.tensorflow.step;

import ai.konduit.serving.annotation.runner.CanRun;
import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.protocol.URIResolver;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.nd4j.common.base.Preconditions;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.tensorflow.conversion.graphrunner.GraphRunner;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@CanRun(Nd4jTensorFlowStep.class)
public class Nd4jTensorFlowRunner implements PipelineStepRunner {

    private final Nd4jTensorFlowStep step;
    private GraphRunner sess;

    @SneakyThrows
    public Nd4jTensorFlowRunner(@NonNull Nd4jTensorFlowStep step) {
        this.step = step;
        File origFile = URIResolver.getFile(step.modelUri());
        Preconditions.checkState(origFile.exists(), "Model file does not exist: " + step.modelUri());
        sess = GraphRunner.builder()
                .inputNames(step.inputNames())
                .graphPath(origFile)
                .outputNames(step.outputNames()).
                build();
    }

    @Override
    public void close() {
        sess.close();
    }

    @Override
    public PipelineStep getPipelineStep() {
        return step;
    }

    @Override
    public Data exec(Context ctx, Data data) {
        Preconditions.checkState(step.inputNames() != null, "TensorFlowStep input array names are not set (null)");
        Map<String,INDArray> inputData = new LinkedHashMap<>();
        for(String key : data.keys()) {
            NDArray ndArray = data.getNDArray(key);
            INDArray arr = ndArray.getAs(INDArray.class);
            inputData.put(key,arr);
        }

        Map<String, INDArray> graphOutput = this.sess.run(inputData);

        Data out = Data.empty();
        for (Map.Entry<String, INDArray> entry : graphOutput.entrySet()) {
            out.put(entry.getKey(), NDArray.create(entry.getValue()));

        }
        return out;
    }
}
