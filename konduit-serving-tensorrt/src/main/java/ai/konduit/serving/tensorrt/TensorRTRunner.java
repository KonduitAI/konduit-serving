/*
 * *****************************************************************************
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
 * ****************************************************************************
 */

package ai.konduit.serving.tensorrt;

import ai.konduit.serving.annotation.runner.CanRun;
import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import com.google.common.primitives.Longs;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.tensorrt.global.nvonnxparser;
import org.bytedeco.tensorrt.nvinfer.*;
import org.bytedeco.tensorrt.nvonnxparser.IParser;
import org.nd4j.common.base.Preconditions;
import org.nd4j.common.util.ArrayUtil;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.bytedeco.cuda.global.cudart.*;
import static org.bytedeco.tensorrt.global.nvinfer.OptProfileSelector;
import static org.bytedeco.tensorrt.global.nvinfer.createInferBuilder;
import static org.bytedeco.tensorrt.global.nvparsers.shutdownProtobufLibrary;
import static org.bytedeco.tensorrt.nvinfer.ILogger.Severity.kINFO;

@CanRun(TensorRTStep.class)
@Slf4j
public class TensorRTRunner implements PipelineStepRunner {

    private ICudaEngine engine;
    private IBuilder builder;
    private TensorRTStep tensorRTStep;
    private TensorRTLogger tensorRTLogger;
    private INetworkDefinition iNetworkDefinition;
    private IParser iParser;
    private IBuilderConfig builderConfig;
    private IOptimizationProfile optimizationProfile;
    private Map<String,long[]> outputDimensions;
    public TensorRTRunner(TensorRTStep tensorRTStep) {
        this.tensorRTStep = tensorRTStep;
        Preconditions.checkNotNull(tensorRTStep.outputDimensions(),"Output dimensions missing!");
        Preconditions.checkNotNull(tensorRTStep.outputNames(),"Missing output names!");
        Preconditions.checkState(tensorRTStep.outputDimensions().size() == tensorRTStep.outputNames().size(),"Output names and output dimensions must be the same size. Output names size was " + tensorRTStep.outputNames().size() + " and output dimensions size was " + tensorRTStep.outputDimensions().size());
        outputDimensions = new LinkedHashMap<>();
        tensorRTStep.outputDimensions().forEach(input -> {
            outputDimensions.put(input.name(),input.dimensions());
        });

        init();
    }


    static void CHECK(int status)
    {
        if (status != 0)
        {
            System.out.println("Cuda failure: " + status);
            throw new IllegalStateException("Failure with status " + status);
        }
    }

    private void init() {
        tensorRTLogger = new TensorRTLogger();
        builder = createInferBuilder(tensorRTLogger);
        iNetworkDefinition = builder.createNetworkV2(tensorRTStep.batchSize());
        iParser = nvonnxparser.createParser(iNetworkDefinition,tensorRTLogger);
        Preconditions.checkNotNull(tensorRTStep.modelUri(),"No model found!");
        File newFile = new File(tensorRTStep.modelUri());
        if(!newFile.exists()) {
            throw new IllegalStateException("Unable to find model file " + tensorRTStep.modelUri());
        }
        if(!iParser.parseFromFile(newFile.getAbsolutePath(),kINFO.value)) {
            throw new IllegalStateException("Unable to parse onnx model from " + tensorRTStep.modelUri());
        }


        builder.setMaxBatchSize(tensorRTStep.batchSize());
        optimizationProfile = builder.createOptimizationProfile();
        if(tensorRTStep.minDimensions() != null) {
            for(NamedDimension dimensionsForName : tensorRTStep.minDimensions()) {
                optimizationProfile.setDimensions(dimensionsForName.name(),OptProfileSelector.kMIN,dims32For(dimensionsForName.dimensions()));
            }
        }



        if(tensorRTStep.maxDimensions() != null) {
            for(NamedDimension dimensionsForName : tensorRTStep.maxDimensions()) {
                optimizationProfile.setDimensions(dimensionsForName.name(),OptProfileSelector.kMAX,dims32For(dimensionsForName.dimensions()));

            }
        }


        if(tensorRTStep.optimalDimensions() != null) {
            for(NamedDimension dimensionsForName : tensorRTStep.optimalDimensions()) {
                optimizationProfile.setDimensions(dimensionsForName.name(),OptProfileSelector.kOPT,dims32For(dimensionsForName.dimensions()));

            }
        }



        builderConfig = builder.createBuilderConfig();
        builderConfig.setMaxWorkspaceSize(tensorRTStep.maxWorkspaceSize());

        builderConfig.addOptimizationProfile(optimizationProfile);

        builder.buildSerializedNetwork(iNetworkDefinition,builderConfig);

        engine = builder.buildEngineWithConfig(iNetworkDefinition, builderConfig);

        Preconditions.checkNotNull(engine,"Failed to create cuda engine!");
    }


    private Dims32 dims32For(long[] input) {
        //Dimensions is 1 + (batch size) input.length
        Dims32 dims32 = null;
        switch(input.length) {
            case 4:
                dims32 = new Dims4();
                break;
            case 1:
                dims32 = new Dims2();
                break;
            case 2:
                dims32 = new Dims3();
                break;
            default:
                dims32 = new Dims32();

        }
        for(int i = 0; i < input.length; i++) {
            dims32.d(i,(int) input[i]);
        }

        return dims32;
    }



    // Logger for GIE info/warning/errors
    static class TensorRTLogger extends ILogger
    {
        @Override public void log(Severity severity, String msg)
        {
            severity = severity.intern();

            // suppress info-level messages
            if (severity == kINFO) return;

            switch (severity)
            {
                case kINTERNAL_ERROR:
                   log.error("INTERNAL_ERROR: " + msg);
                    break;
                case kERROR:
                    log.error("INTERNAL_ERROR: " + msg);
                    break;
                case kWARNING:
                    log.warn("INTERNAL_ERROR: " + msg);
                break;
                case kINFO:
                    log.info("INTERNAL_ERROR: " + msg);
                    break;
                default: log.info("UNKNOWN: " + msg);
                break;
            }
        }
    }

    @Override
    public void close() {
        engine.destroy();
        builder.destroy();
        shutdownProtobufLibrary();
    }

    @Override
    public PipelineStep getPipelineStep() {
        return tensorRTStep;
    }

    @Override
    public Data exec(Context ctx, Data data) {
        Data ret = Data.empty();
        IExecutionContext iExecutionContext = engine.createExecutionContext();
        PointerPointer buffers = new PointerPointer(tensorRTStep.inputNames().size() + tensorRTStep.outputNames().size());
        INDArray firstInput = data.getNDArray(tensorRTStep.inputNames().get(0)).getAs(INDArray.class);
        long batchSize = firstInput.size(0);
        for(int i = 0; i < tensorRTStep.inputNames().size(); i++) {
            INDArray input = data.getNDArray(tensorRTStep.inputNames().get(i)).getAs(INDArray.class);
            long bytes = input.length() * input.dataType().width();
            CHECK(cudaMalloc(buffers.position(i),bytes));
            CHECK(cudaMemcpy(buffers.position(i).get(),
                    input.data().pointer(),
                    bytes,
                    cudaMemcpyHostToDevice));
        }


        Preconditions.checkState(tensorRTStep.outputNames().size() == tensorRTStep.outputDimensions().size());
        for(int i = 0; i < tensorRTStep.outputNames().size(); i++) {
            long[] outputShape = outputDimensions.get(tensorRTStep.outputNames().get(i));
            int idx = tensorRTStep.inputNames().size() +   i;
            long bytes = ArrayUtil.prod(outputShape) * firstInput.data().getElementSize();
            CHECK(cudaMalloc(buffers.position( idx), bytes));
        }

        if(!iExecutionContext.executeV2(buffers.position(0))) {
            throw new IllegalStateException("Execution did not work");
        }

        for(int i = 0; i < tensorRTStep.outputNames().size(); i++) {
            long[] outputShape = tensorRTStep.outputDimensions().get(i).dimensions();
            INDArray output = Nd4j.create(Longs.concat(new long[]{batchSize},outputShape)).castTo(DataType.FLOAT);
            int idx = tensorRTStep.inputNames().size() +   i;
            long bytes = output.length() * output.data().getElementSize();
            CHECK(cudaMemcpy(
                    output.data().pointer(),
                    buffers.position( idx).get(),
                    bytes,
                    cudaMemcpyDeviceToHost));

            ret.put(tensorRTStep.outputNames().get(i), NDArray.create(output));

        }


        for(int i = 0; i < tensorRTStep.inputNames().size() + tensorRTStep.outputNames().size(); i++) {
            cudaFree(buffers.position(i).get());
        }



        return ret;
    }
}
