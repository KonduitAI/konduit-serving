package ai.konduit.serving.models.onnx.step;

// Copyright(c) Microsoft Corporation.All rights reserved.
// Licensed under the MIT License.
//

import ai.konduit.serving.models.onnx.utils.ONNXUtils;
import org.bytedeco.javacpp.*;
import org.bytedeco.onnxruntime.*;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import static org.bytedeco.onnxruntime.global.onnxruntime.*;

public class CXXApiAddSampleModified {

    public static void main(String[] args) throws Exception {
        //*************************************************************************
        // initialize  enviroment...one enviroment per process
        // enviroment maintains thread pools and other state info
        Env env = new Env(ORT_LOGGING_LEVEL_WARNING, "test");

        // initialize session options if needed
        SessionOptions session_options = new SessionOptions();
        session_options.SetIntraOpNumThreads(1);

        // If onnxruntime.dll is built with CUDA enabled, we can uncomment out this line to use CUDA for this
        // session (we also need to include cuda_provider_factory.h above which defines it)
        // #include "cuda_provider_factory.h"
        // OrtSessionOptionsAppendExecutionProvider_CUDA(session_options, 1);
        OrtSessionOptionsAppendExecutionProvider_Dnnl(session_options.asOrtSessionOptions(), 1);

        // Sets graph optimization level
        // Available levels are
        // ORT_DISABLE_ALL -> To disable all optimizations
        // ORT_ENABLE_BASIC -> To enable basic optimizations (Such as redundant node removals)
        // ORT_ENABLE_EXTENDED -> To enable extended optimizations (Includes level 1 + more complex optimizations like node fusions)
        // ORT_ENABLE_ALL -> To Enable All possible opitmizations
        session_options.SetGraphOptimizationLevel(ORT_ENABLE_EXTENDED);

        //*************************************************************************
        // create session and load model into memory
        // using squeezenet version 1.3
        // URL = https://github.com/onnx/models/tree/master/squeezenet
        String s = args.length > 0 ? args[0] : "C:\\Users\\agibs\\Documents\\GitHub\\konduit-serving\\konduit-serving-models\\konduit-serving-onnx\\src\\test\\resources\\add.onnx";
        Pointer model_path = Loader.getPlatform().startsWith("windows") ? new CharPointer(s) : new BytePointer(s);

        System.out.println("Using Onnxruntime C++ API");
        Session session = new Session(env, model_path, session_options);

        //*************************************************************************
        // print model input layer (node names, types, shape etc.)
        AllocatorWithDefaultOptions allocator = new AllocatorWithDefaultOptions();

        // print number of model input nodes
        long num_input_nodes = session.GetInputCount();
        PointerPointer input_node_names = new PointerPointer(num_input_nodes);
        LongPointer input_node_dims = null;  // simplify... this model has only 1 input node {1, 3, 224, 224}.
        // Otherwise need vector<vector<>>

        System.out.println("Number of inputs = " + num_input_nodes);

        // iterate over all input nodes
        for (long i = 0; i < num_input_nodes; i++) {
            // print input node names
            BytePointer input_name = session.GetInputName(i, allocator.asOrtAllocator());
            System.out.println("Input " + i + " : name=" + input_name.getString());
            input_node_names.put(i, input_name);
        }


        PointerPointer output_node_names = new PointerPointer("z");


        // create input tensor object from data values
        MemoryInfo memory_info = MemoryInfo.CreateCpu(OrtArenaAllocator, OrtMemTypeDefault);
        Value values = new Value(2);
        INDArray x = Nd4j.scalar(1.0f).reshape(1,1);
        Value xVal = ONNXUtils.getTensor(x,memory_info);
        INDArray y = Nd4j.scalar(1.0f).reshape(1,1);
        Value yVal = ONNXUtils.getTensor(y,memory_info);
        values.position(0).put(xVal);
        values.position(1).put(yVal);
        values.position(0);

        // score model & input tensor, get back output tensor
        ValueVector output_tensor = session.Run(new RunOptions(), input_node_names, values, 2, output_node_names, 1);
        assert output_tensor.size() == 1 && output_tensor.get(0).IsTensor();

        // Get pointer to output tensor float values
        FloatPointer floatarr = output_tensor.get(0).GetTensorMutableDataFloat();
        assert Math.abs(floatarr.get(0) - 0.000045) < 1e-6;

        // score the model, and print scores for first 5 classes
        for (int i = 0; i < 1; i++)
            System.out.println("Score for class [" + i + "] =  " + floatarr.get(i));

        System.out.println("Done!");
        System.exit(0);
    }
}