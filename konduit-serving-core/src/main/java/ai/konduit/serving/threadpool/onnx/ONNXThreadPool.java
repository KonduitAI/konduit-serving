/*
 *
 *  * ******************************************************************************
 *  *  * Copyright (c) 2015-2019 Skymind Inc.
 *  *  * Copyright (c) 2019-2020 Konduit AI.
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

package ai.konduit.serving.threadpool.onnx;


import ai.konduit.serving.model.loader.ModelLoader;
import ai.konduit.serving.threadpool.onnx.observables.BasicOnnxInferenceObservable;
import ai.konduit.serving.threadpool.onnx.observables.BatchedOnnxInferenceObservable;
import ai.konduit.serving.threadpool.onnx.observables.OnnxObservable;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.deeplearning4j.parallelism.inference.InferenceMode;
import org.deeplearning4j.parallelism.inference.observers.BasicInferenceObserver;

import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.LongPointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.javacpp.indexer.Indexer;
import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.onnxruntime.Session;
import org.bytedeco.onnxruntime.MemoryInfo;
import org.bytedeco.onnxruntime.OrtMemoryInfo;
import org.bytedeco.onnxruntime.RunOptions;
import org.bytedeco.onnxruntime.Value;
import org.bytedeco.onnxruntime.TypeInfo;
import org.bytedeco.onnxruntime.OrtTypeInfo;
import org.bytedeco.onnxruntime.OrtApi;
import org.bytedeco.onnxruntime.OrtStatus;
import org.bytedeco.onnxruntime.TensorTypeAndShapeInfo;
import org.bytedeco.onnxruntime.OrtTensorTypeAndShapeInfo;
import org.bytedeco.onnxruntime.ValueVector;
import org.bytedeco.onnxruntime.AllocatorWithDefaultOptions;
import static org.bytedeco.onnxruntime.global.onnxruntime.OrtArenaAllocator;
import static org.bytedeco.onnxruntime.global.onnxruntime.OrtMemTypeDefault;
import static org.bytedeco.onnxruntime.global.onnxruntime.ONNX_TENSOR_ELEMENT_DATA_TYPE_FLOAT;
import static org.bytedeco.onnxruntime.global.onnxruntime.ORT_API_VERSION;
import static org.bytedeco.onnxruntime.global.onnxruntime.OrtGetApiBase;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.buffer.DataBuffer;

import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Observer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class is simple wrapper for
 * ONNXThreadPool using batched input
 * Adapted from {@link org.deeplearning4j.parallelism.ParallelInference}
 *
 * @author Adam Gibson, Alex Merritt
 */
@Slf4j
public class ONNXThreadPool {

    public final static int DEFAULT_NUM_WORKERS = Nd4j.getAffinityManager().getNumberOfDevices();
    public final static int DEFAULT_BATCH_LIMIT = 32;
    public final static InferenceMode DEFAULT_INFERENCE_MODE = InferenceMode.BATCHED;
    public final static int DEFAULT_QUEUE_LIMIT = 64;
    private ModelLoader<Session> onnxModelLoader;
    private long nanos;
    private int workers;
    private int batchLimit;
    private InferenceMode inferenceMode;
    private int queueLimit;
    // this queue
    private BlockingQueue<OnnxObservable> observables;
    private InferenceWorker[] zoo;
    private ObservablesProvider provider;


    protected ONNXThreadPool() {
        //
    }

    protected void init() {
        observables = new LinkedBlockingQueue<>(queueLimit);

        int numDevices = Nd4j.getAffinityManager().getNumberOfDevices();
        int currentDevice = Nd4j.getAffinityManager().getDeviceForCurrentThread();
        AtomicBoolean assignedRoot = new AtomicBoolean(false);

        zoo = new InferenceWorker[workers];
        for (int i = 0; i < workers; i++) {
            int cDevice = i % numDevices;
            boolean cRoot = !assignedRoot.get() && cDevice == currentDevice;
            assignedRoot.compareAndSet(false, cRoot);

            zoo[i] = new InferenceWorker(i, observables, true, onnxModelLoader);

            Nd4j.getAffinityManager().unsafeSetDevice(cDevice);
            zoo[i].setDaemon(true);
            zoo[i].start();
        }


        if (inferenceMode == InferenceMode.BATCHED) {
            log.debug("Initializing ObservablesProvider...");
            nanos = System.nanoTime();
            provider = new ObservablesProvider(nanos, batchLimit, observables);
        }
    }

    protected long getWorkerCounter(int workerIdx) {
        return zoo[workerIdx].getCounterValue();
    }

    /**
     * This method gracefully shuts down ONNXThreadPool instance
     */
    public synchronized void shutdown() {
        if (zoo == null)
            return;

        for (int e = 0; e < zoo.length; e++) {
            if (zoo[e] == null)
                continue;

            zoo[e].interrupt();
            zoo[e].shutdown();
            zoo[e] = null;
        }
        zoo = null;

        System.gc();
    }


    /**
     * Generate predictions/outputSchema from the network, optionally using input masks for predictions
     *
     * @param input Input to the network
     * @return Output from the network
     */
    public List<Map<String, INDArray>> output(List<Map<String, INDArray>> input) {
        // basically, depending on model type we either throw stuff to specific model, or wait for batch

        BasicInferenceObserver observer = new BasicInferenceObserver();
        OnnxObservable observable;


        if (inferenceMode == InferenceMode.SEQUENTIAL) {
            observable = new BasicOnnxInferenceObservable(input);
            observable.addObserver(observer);
            try {
                observables.put(observable);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        } else {
            observable = provider.setInput(observer, input);
        }

        try {
            // submit query to processing
            // and block until Observable returns
            //observer.wait();

            observer.waitTillDone();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        return observable.getOutput();
    }


    public static class Builder {
        private ModelLoader<Session> onnxModelLoader;
        private int workers = DEFAULT_NUM_WORKERS;
        private int batchLimit = DEFAULT_BATCH_LIMIT;
        private InferenceMode inferenceMode = DEFAULT_INFERENCE_MODE;
        private int queueLimit = DEFAULT_QUEUE_LIMIT;

        public Builder(@NonNull ModelLoader<Session> onnxModelLoader) {
            this.onnxModelLoader = onnxModelLoader;
        }


        /**
         * This method allows you to define mode that'll be used during inference. Options are:
         * <p>
         * SEQUENTIAL: Input will be sent to last-used worker unmodified.
         * BATCHED: Multiple inputs will be packed into single batch, and
         * sent to last-used device.
         *
         * @param inferenceMode the inference mode to use
         * @return the builder
         */
        public Builder inferenceMode(@NonNull InferenceMode inferenceMode) {
            this.inferenceMode = inferenceMode;
            return this;
        }


        /**
         * This method defines, how many model copies will be used for inference.
         * <p>
         * PLEASE NOTE: This method primarily suited for multi-GPU systems
         *
         * @param workers the number of workers to use
         * @return the builder
         */
        public Builder workers(int workers) {
            if (workers < 1)
                throw new IllegalStateException("Workers should be positive value");

            this.workers = workers;
            return this;
        }

        /**
         * This method defines, how many input samples can
         * be batched within given time frame.
         * <p>
         * PLEASE NOTE: This value has no effect in
         * SEQUENTIAL inference mode
         *
         * @param limit the limit to use
         * @return the builder
         */
        public Builder batchLimit(int limit) {
            if (limit < 1)
                throw new IllegalStateException("Batch limit should be positive value");

            this.batchLimit = limit;
            return this;
        }

        /**
         * This method defines buffer queue size.
         * <p>
         * Default value: 64
         *
         * @param limit the limit to use
         * @return the builder
         */
        public Builder queueLimit(int limit) {
            if (limit < 1)
                throw new IllegalStateException("Queue limit should be positive value");

            this.queueLimit = limit;
            return this;
        }

        /**
         * This method builds new ONNXThreadPool instance
         *
         * @return the built thread pool
         */
        public ONNXThreadPool build() {
            ONNXThreadPool inference = new ONNXThreadPool();
            inference.batchLimit = this.batchLimit;
            inference.queueLimit = this.queueLimit;
            inference.inferenceMode = this.inferenceMode;
            inference.workers = this.workers;
            inference.onnxModelLoader = this.onnxModelLoader;
            inference.init();

            return inference;
        }
    }

    protected static class ObservablesProvider {
        private final Object locker = new Object();
        private BlockingQueue<OnnxObservable> targetQueue;
        private long nanos;
        private int batchLimit;
        private volatile BatchedOnnxInferenceObservable currentObservable;

        protected ObservablesProvider(long nanos, int batchLimit, @NonNull BlockingQueue<OnnxObservable> queue) {
            this.targetQueue = queue;
            this.nanos = nanos;
            this.batchLimit = batchLimit;
        }

        protected OnnxObservable setInput(@NonNull Observer observer, List<Map<String, INDArray>> input) {
            synchronized (locker) {
                boolean isNew = false;
                if (currentObservable == null || currentObservable.getCounter() >= batchLimit
                        || currentObservable.isLocked()) {
                    isNew = true;
                    currentObservable = new BatchedOnnxInferenceObservable();
                }

                currentObservable.addInput(input);
                currentObservable.addObserver(observer);

                try {
                    if (isNew)
                        targetQueue.put(currentObservable);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }

                return currentObservable;
            }
        }
    }

    /**
     * This class actually does inference with respect to device affinity
     */
    private class InferenceWorker extends Thread implements Runnable {
        private BlockingQueue<OnnxObservable> inputQueue;
        private AtomicBoolean shouldWork = new AtomicBoolean(true);
        private AtomicBoolean isStopped = new AtomicBoolean(false);
        private Session replicatedModel;
        private AtomicLong counter = new AtomicLong(0);
        private boolean rootDevice;

        private ModelLoader<Session> onnxModelLoader;

        private InferenceWorker(int id, @NonNull BlockingQueue inputQueue, boolean rootDevice, @NonNull ModelLoader<Session> modelLoader) {
            this.inputQueue = inputQueue;
            this.onnxModelLoader = modelLoader;
            this.rootDevice = rootDevice;
            this.setDaemon(true);
            this.setName("InferenceThread-" + id);

        }

        protected long getCounterValue() {
            return counter.get();
        }

        @Override
        public void run() {
            try {
                // model should be replicated & initialized here
                this.replicatedModel = onnxModelLoader.loadModel();

                AllocatorWithDefaultOptions allocator = new AllocatorWithDefaultOptions();	

		Long num_input_nodes = replicatedModel.GetInputCount();
		Long num_output_nodes = replicatedModel.GetOutputCount();
                PointerPointer input_node_names = new PointerPointer(num_input_nodes);
                PointerPointer output_node_names = new PointerPointer(num_output_nodes);

		LongPointer[] input_node_dims = new LongPointer[num_input_nodes.intValue()];

		for (int i = 0; i < num_output_nodes; i++) {
	            BytePointer output_name = replicatedModel.GetOutputName(i, allocator.asOrtAllocator());
                    output_node_names.put(i, output_name);
		}
		long[] inputSizes = new long[num_input_nodes.intValue()];
		long inputSize = 0;
                for (int i = 0; i < num_input_nodes; i++) {
		    BytePointer input_name = replicatedModel.GetInputName(i, allocator.asOrtAllocator());
                    input_node_names.put(i, input_name);

                    TypeInfo typeInfo = replicatedModel.GetInputTypeInfo(i);
                    TensorTypeAndShapeInfo tensor_info = typeInfo.GetTensorTypeAndShapeInfo();
                    int type = tensor_info.GetElementType();

		    input_node_dims[i] = tensor_info.GetShape();

	            int acc = 1;
		    for (long j = 0; j < input_node_dims[i].capacity(); j++)
	                acc *= input_node_dims[i].get(j);

		    inputSizes[i] = acc;
		    inputSize += acc;
		}

                while (shouldWork.get()) {
                    OnnxObservable request = inputQueue.take();

                    if (request != null) {
                        counter.incrementAndGet();

                        List<Map<String, INDArray>> batches = request.getInputBatches();
                        List<Map<String, INDArray>> out = new ArrayList<>(batches.size());

                        try {
                            for (Map<String, INDArray> inBatch : batches) {	
			        Collection<INDArray> inputArrays = inBatch.values();	    
			        INDArray inputArray = Nd4j.concat(0, inputArrays.toArray(new INDArray[inputArrays.size()]));
                             
			        Value[] inputTensors = new Value[num_input_nodes.intValue()];

                                for (int i = 0; i < num_input_nodes; i++) {
		                  BytePointer input_name = (BytePointer)input_node_names.get(i);
		
				  FloatPointer input_tensor_values = (FloatPointer)inBatch.get(input_name.getString()).data().pointer();

				//TODO: Handle other data types here, currently only Float inputs supported
				//FloatPointer input_tensor_values = (FloatPointer)inputArray.data().pointer();

				  MemoryInfo memory_info = MemoryInfo.CreateCpu(OrtArenaAllocator, OrtMemTypeDefault);

				//hardcoded to Float
				  Value inputTensor = Value.CreateTensor(memory_info.asOrtMemoryInfo(), input_tensor_values, inputSizes[i] * Float.SIZE / 8, input_node_dims[i], 4, ONNX_TENSOR_ELEMENT_DATA_TYPE_FLOAT);
				  inputTensors[i] = inputTensor;

				}
				PointerPointer inputTensorsPP = new PointerPointer(inputTensors);
				ValueVector outputVector = replicatedModel.Run(new RunOptions(), input_node_names, new Value(inputTensorsPP), num_input_nodes, output_node_names, num_output_nodes);

				Map<String, INDArray> output = new HashMap<String, INDArray>();

                                for (int i = 0; i < num_output_nodes; i++) {
					FloatPointer fpOut = outputVector.get(i).GetTensorMutableDataFloat();
					Indexer indexer = FloatIndexer.create(fpOut);
					DataBuffer buffer = Nd4j.createBuffer(fpOut, DataType.FLOAT, fpOut.capacity(), indexer);
					output.put(((BytePointer)output_node_names.get(i)).getString(), Nd4j.create(buffer));
				}
                                out.add((Map<String, INDArray>) output);
                            }

                            request.setOutputBatches(out);
                        } catch (Exception e) {
                            log.error("Error occurred doing inference", e);
                            request.setOutputException(e);

                        }


                    } else {
                        // just do nothing, i guess and hope for next round?
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                // do nothing
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                isStopped.set(true);
            }
        }

        protected void shutdown() {
            shouldWork.set(false);
            while (!isStopped.get()) {
                // block until main loop is finished
            }
        }
    }


}
