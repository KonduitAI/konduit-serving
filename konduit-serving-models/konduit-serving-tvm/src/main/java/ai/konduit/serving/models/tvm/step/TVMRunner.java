/*
 *  ******************************************************************************
 *  * Copyright (c) 2022 Konduit K.K.
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

package ai.konduit.serving.models.tvm.step;

import ai.konduit.serving.annotation.runner.CanRun;
import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.data.ValueType;
import ai.konduit.serving.pipeline.api.protocol.URIResolver;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import ai.konduit.serving.pipeline.impl.data.ValueNotFoundException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.tvm.presets.tvm;
import org.nd4j.common.base.Preconditions;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.tvm.runner.TvmRunner;


import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@CanRun(TVMStep.class)
public class TVMRunner implements PipelineStepRunner {

    private final TVMStep step;
    private TvmRunner tvmRunner;

    static {
        //ensure native libraries get loaded
        Loader.load(tvm.class);
    }

    public TVMRunner(@NonNull TVMStep step) {
        this.step = step;
        if(!step.lazyInit()) {
            init();
        }
        else {
            log.warn("Lazy initialization of tvm model specified. Model will be initialized first time pipeline step is executed.");
        }
    }

    @Override
    public void close() {
        tvmRunner.close();
    }

    @Override
    public PipelineStep getPipelineStep() {
        return step;
    }

    @Override
    public Data exec(Context ctx, Data data) {
        if(tvmRunner == null) {
            log.info("Lazy initialization of model found. Initializing model on first run.");
            init();
        }
        Preconditions.checkState(step.inputNames() != null, "TVMStep input array names are not set (null)");
        Map<String,INDArray> input = new HashMap<>();
        for (String s : step.inputNames()) {
            if(!data.has(s)){
                throw new ValueNotFoundException( "Error in TVMStep: Input data does not have a value corresponding to TensorFlowStep.inputNames value \"" +
                        s + "\" - data keys = " + data.keys());
            }
            if(data.type(s) != ValueType.NDARRAY) {
                String listType = data.type(s) == ValueType.LIST ? data.listType(s).toString() : null;
                throw new ValueNotFoundException( "Error in TVMStep (" + name() + "): Input data value corresponding to TensorFlowStep.inputNames value \"" +
                        s + "\" is not an NDArray type - is " + (listType == null ? data.type(s) : "List<" + listType + ">"));
            }


            NDArray arr = data.getNDArray(s);
            INDArray arr2 = arr.getAs(INDArray.class);
            input.put(s,arr2);

        }

        Data out = Data.empty();
        List<String> outNames = step.outputNames();
        Map<String, INDArray> exec = tvmRunner.exec(input);
        for(Map.Entry<String,INDArray> outputValues : exec.entrySet()) {
            if(!outNames.contains(outputValues.getKey())) {
                throw new IllegalStateException("Output names " + outNames + " did not contain value output from tvm " + outputValues.getKey() + " - please ensure the output names are the same as the target model being run.");
            }

            out.put(outputValues.getKey(),NDArray.create(outputValues.getValue()));
        }

        return out;
    }


    protected void init() {
        try {
            initHelper();
        } catch (Throwable t) {
            throw new RuntimeException("Error loading TVM model", t);
        }
    }

    protected void initHelper() throws Exception {
        String uri = step.modelUri();
        File origFile = URIResolver.getFile(uri);
        Preconditions.checkState(origFile.exists(), "Model file does not exist: " + uri);
        System.out.println("Files " + Arrays.toString(origFile.getParentFile().list()) + " with parent directory " + origFile.getParentFile().getAbsolutePath());
        tvmRunner = TvmRunner.builder().modelUri(origFile.getAbsolutePath()).build();
    }
}
