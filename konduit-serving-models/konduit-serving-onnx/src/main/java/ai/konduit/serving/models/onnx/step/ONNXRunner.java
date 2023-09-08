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

package ai.konduit.serving.models.onnx.step;

import ai.konduit.serving.annotation.runner.CanRun;
import ai.konduit.serving.models.onnx.utils.ONNXUtils;
import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacpp.*;
import org.bytedeco.onnxruntime.*;
import org.nd4j.common.base.Preconditions;
import org.nd4j.linalg.api.buffer.DataBuffer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static ai.konduit.serving.models.onnx.utils.ONNXUtils.getDataBuffer;
import static ai.konduit.serving.models.onnx.utils.ONNXUtils.getTensor;
import static org.bytedeco.onnxruntime.global.onnxruntime.*;

@Slf4j
@CanRun({ONNXStep.class})
public class ONNXRunner implements PipelineStepRunner {

    private  ONNXStep onnxStep;
    private Session session;
    private RunOptions runOptions;
    private MemoryInfo memoryInfo;
    private OrtAllocator allocator;
    private  SessionOptions sessionOptions;
    private   static Env env;
    private Pointer bp;

    public ONNXRunner(ONNXStep onnxStep) {
        this.onnxStep = onnxStep;
        if(env == null) {
            env = new Env(ONNXUtils.getOnnxLogLevelFromLogger(log), new BytePointer("konduit-serving-onnx-session-" + UUID.randomUUID().toString()));
            env.retainReference();
        }
        sessionOptions = new SessionOptions();
        sessionOptions.SetGraphOptimizationLevel(ORT_ENABLE_EXTENDED);
        sessionOptions.SetIntraOpNumThreads(1);
        sessionOptions.retainReference();
        allocator = new OrtAllocator();
        allocator.retainReference();
        bp = Loader.getPlatform().toLowerCase().startsWith("windows") ? new CharPointer(onnxStep.modelUri()) : new BytePointer(onnxStep.modelUri());
        runOptions = new RunOptions();
        memoryInfo = MemoryInfo.CreateCpu(OrtArenaAllocator, OrtMemTypeDefault);
        session = new Session(env, bp, sessionOptions);
        //retain the session reference to prevent pre emptive release of the session.
        session.retainReference();

    }



    @Override
    public void close() {
        if(session != null) {
            session.close();
        }

        sessionOptions.releaseReference();
        allocator.releaseReference();
        runOptions.releaseReference();
    }

    @Override
    public PipelineStep getPipelineStep() {
        return onnxStep;
    }

    @Override
    public Data exec(Context ctx, Data data) {
        Data ret = Data.empty();
        long numInputNodes = session.GetInputCount();
        long numOutputNodes = session.GetOutputCount();

        PointerPointer<BytePointer> inputNodeNames = new PointerPointer<>(numInputNodes);
        PointerPointer<BytePointer> outputNodeNames = new PointerPointer<>(numOutputNodes);

        Value inputVal = new Value(numInputNodes);

        for (int i = 0; i < numInputNodes; i++) {
            BytePointer inputName = session.GetInputNameAllocated(i, allocator);
            inputNodeNames.put(i, inputName);
            INDArray arr = data.getNDArray(inputName.getString()).getAs(INDArray.class);
            Value inputTensor = getTensor(arr, memoryInfo);
            Preconditions.checkState(inputTensor.IsTensor(),"Input must be a tensor.");
            inputVal.position(i).put(inputTensor);
        }

        //reset position after iterating
        inputVal.position(0);

        for (int i = 0; i < numOutputNodes; i++) {
            BytePointer outputName = session.GetOutputNameAllocated(i, allocator);
            outputNodeNames.put(i, outputName);
        }

        ValueVector outputVector = session.Run(
                runOptions,
                inputNodeNames,
                inputVal,
                numInputNodes,
                outputNodeNames,
                numOutputNodes);

        Map<String, INDArray> output = new LinkedHashMap<>();

        for (int i = 0; i < numOutputNodes; i++) {
            Value outValue = outputVector.get(i);

            DataBuffer buffer = getDataBuffer(outValue);
            output.put((outputNodeNames.get(BytePointer.class, i)).getString(), Nd4j.create(buffer));
        }

        Preconditions.checkNotNull(output,"Output must not be null!");
        for(String outputName : onnxStep.outputNames()) {
            Preconditions.checkNotNull(output.get(outputName),"Output name " + outputName + " not found in output!");
            ret.put(outputName, NDArray.create(output.get(outputName)));
        }


        return ret;


    }

}
