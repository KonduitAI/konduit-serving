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

package ai.konduit.serving.threadpool.onnx;


import ai.konduit.serving.model.loader.ModelLoader;
import ai.konduit.serving.threadpool.onnx.observables.BasicOnnxInferenceObservable;
import ai.konduit.serving.threadpool.onnx.observables.BatchedOnnxInferenceObservable;
import ai.konduit.serving.threadpool.onnx.observables.OnnxObservable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacpp.*;
import org.bytedeco.javacpp.indexer.*;
import org.bytedeco.onnxruntime.*;
import org.deeplearning4j.parallelism.inference.InferenceMode;
import org.deeplearning4j.parallelism.inference.observers.BasicInferenceObserver;
import org.nd4j.linalg.api.buffer.DataBuffer;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.bytedeco.onnxruntime.global.onnxruntime.*;

/**
 * This class is simple wrapper for
 * ONNXThreadPool using batched input
 * Adapted from {@link org.deeplearning4j.parallelism.ParallelInference}
 *
 * @author Adam Gibson, Alex Merritt
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ONNXThreadPool {

    public static final int DEFAULT_NUM_WORKERS = Nd4j.getAffinityManager().getNumberOfDevices();
    public static final int DEFAULT_BATCH_LIMIT = 32;
    public static final InferenceMode DEFAULT_INFERENCE_MODE = InferenceMode.BATCHED;
    public static final int DEFAULT_QUEUE_LIMIT = 64;
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

            zoo[i] = new InferenceWorker(i, observables, onnxModelLoader);

            Nd4j.getAffinityManager().unsafeSetDevice(cDevice);
            zoo[i].setDaemon(true);
            zoo[i].start();
        }


        if (inferenceMode == InferenceMode.BATCHED) {
            log.debug("Initializing ObservablesProvider...");
            provider = new ObservablesProvider(batchLimit, observables);
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
    public Map<String, INDArray> output(Map<String, INDArray> input) {
        // basically, depending on model type we either throw stuff to specific model, or wait for batch

        BasicInferenceObserver observer = new BasicInferenceObserver();
        OnnxObservable observable;


        //Batch of 1
        List<Map<String, INDArray>> inputs = Collections.singletonList(input);

        if (inferenceMode == InferenceMode.SEQUENTIAL) {
            observable = new BasicOnnxInferenceObservable(inputs);
            observable.addObserver(observer);
            try {
                observables.put(observable);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        } else {
            observable = provider.setInput(observer, inputs);
        }

        try {
            // submit query to processing
            // and block until Observable returns
            observer.waitTillDone();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return observable.getOutput().get(0);
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
        private int batchLimit;
        private volatile BatchedOnnxInferenceObservable currentObservable;

        protected ObservablesProvider(int batchLimit, @NonNull BlockingQueue<OnnxObservable> queue) {
            this.targetQueue = queue;
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
        private AtomicLong counter = new AtomicLong(0);

        private ModelLoader<Session> onnxModelLoader;

        private InferenceWorker(int id, @NonNull BlockingQueue inputQueue, @NonNull ModelLoader<Session> modelLoader) {
            this.inputQueue = inputQueue;
            this.onnxModelLoader = modelLoader;
            this.setDaemon(true);
            this.setName("InferenceThread-" + id);

        }

        protected long getCounterValue() {
            return counter.get();
        }

        @Override
        public void run() {
            try (PointerScope scope = new PointerScope()) {

                // model should be replicated & initialized here
		Session replicatedModel = onnxModelLoader.loadModel();

                long numInputNodes = replicatedModel.GetInputCount();
                long numOutputNodes = replicatedModel.GetOutputCount();
                try (AllocatorWithDefaultOptions allocator = new AllocatorWithDefaultOptions();
                     PointerPointer<BytePointer> inputNodeNames = new PointerPointer<>(numInputNodes);
                     PointerPointer<BytePointer> outputNodeNames = new PointerPointer<>(numOutputNodes)) {

                    LongPointer[] inputNodeDims = new LongPointer[(int) numInputNodes];

                    for (int i = 0; i < numOutputNodes; i++) {
                        BytePointer outputName = replicatedModel.GetOutputName(i, allocator.asOrtAllocator());
                        outputNodeNames.put(i, outputName);
                    }
                    long[] inputSizes = new long[(int) numInputNodes];
                    int[] inputTypes = new int[(int) numInputNodes];

                    for (int i = 0; i < numInputNodes; i++) {
                        BytePointer inputName = replicatedModel.GetInputName(i, allocator.asOrtAllocator());

                        inputNodeNames.put(i, inputName);

                        TypeInfo typeInfo = replicatedModel.GetInputTypeInfo(i);
                        inputTypes[i] = typeInfo.GetONNXType();

                        TensorTypeAndShapeInfo tensorInfo = typeInfo.GetTensorTypeAndShapeInfo();
                        inputNodeDims[i] = tensorInfo.GetShape();

                        int acc = 1;
                        for (long j = 0; j < inputNodeDims[i].capacity(); j++)
                            acc *= inputNodeDims[i].get(j);

                        inputSizes[i] = acc;
                    }

                    while (shouldWork.get()) {
                        OnnxObservable request = inputQueue.take();
                        counter.incrementAndGet();

                        List<Map<String, INDArray>> batches = request.getInputBatches();

                        List<Map<String, INDArray>> out = doBatchInference(request, replicatedModel, inputNodeNames, outputNodeNames, inputTypes,
					inputSizes, inputNodeDims); 

                        request.setOutputBatches(out);

                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                // do nothing
            } catch (Exception e) {
		log.error("Error occurred doing inference", e);

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

        protected void validateType(DataType expected, INDArray array){
            if (!array.dataType().equals(expected))
                throw new RuntimeException("INDArray data type (" + array.dataType() + ") does not match required ONNX data type (" + expected + ")");
        }

	private List<Map<String, INDArray>> doBatchInference(OnnxObservable request, Session replicatedModel, PointerPointer<BytePointer> inputNodeNames,
                     PointerPointer<BytePointer> outputNodeNames, int[] inputTypes, long[] inputSizes, LongPointer[] inputNodeDims) {
        
		List<Map<String, INDArray>> batches = request.getInputBatches();

                long numInputNodes = replicatedModel.GetInputCount();	
                long numOutputNodes = replicatedModel.GetOutputCount(); 
 
                List<Map<String, INDArray>> out = new ArrayList<>(batches.size());

		try {
	    		for (Map<String, INDArray> inBatch : batches) { 
	    			Value[] inputTensors = new Value[(int) numInputNodes];

                                for (int i = 0; i < numInputNodes; i++) {
                                    BytePointer inputName = inputNodeNames.get(BytePointer.class, i);
                                    Value inputTensor = getTensor(inBatch.get(inputName.getString()), inputTypes[i], inputSizes[i],inputNodeDims[i]);
                                    inputTensors[i] = inputTensor;
                                }

                                Value inputVal = new Value(numInputNodes);

                                for (int i = 0; i < numInputNodes; i++) {
                                    inputVal.position(i).put(inputTensors[i]);
                                }

                                ValueVector outputVector = replicatedModel.Run(new RunOptions(), inputNodeNames, inputVal.position(0), numInputNodes, outputNodeNames, numOutputNodes);

                                Map<String, INDArray> output = new LinkedHashMap<String, INDArray>();

                                for (int i = 0; i < numOutputNodes; i++) {
                                    Value outValue = outputVector.get(i);

                                    DataBuffer buffer = getDataBuffer(outValue);
                                    INDArray outArray = Nd4j.create(buffer);
                                    output.put((outputNodeNames.get(BytePointer.class, i)).getString(), outArray);

                                }
                                out.add(output);
                            }
                        } catch (Exception e) {
                            log.error("Error occurred doing inference", e);
                        }
		return out;
	}

        private Value getTensor(INDArray ndArray, int type, long size, LongPointer dims) {
            Pointer inputTensorValuesPtr = ndArray.data().pointer();

            long sizeInBytes;
            MemoryInfo memoryInfo = MemoryInfo.CreateCpu(OrtArenaAllocator, OrtMemTypeDefault);
            Pointer inputTensorValues = null;
            switch (type) {
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_FLOAT:
                    validateType(DataType.FLOAT, ndArray);
                    inputTensorValues = inputTensorValuesPtr;
                    sizeInBytes = size * 4;
                    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_UINT8:
                    validateType(DataType.UINT8, ndArray);
                    inputTensorValues = inputTensorValuesPtr;
                    sizeInBytes = size;
                    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_INT8:
                    validateType(DataType.INT8, ndArray);
                    inputTensorValues = inputTensorValuesPtr;
                    sizeInBytes = size;
                    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_UINT16:
                    validateType(DataType.UINT16, ndArray);
                    inputTensorValues = inputTensorValuesPtr;
                    sizeInBytes = size * 2;
                    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_INT16:
                    validateType(DataType.INT16, ndArray);
                    inputTensorValues = inputTensorValuesPtr;
                    sizeInBytes = size * 2;
                    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_INT32:
                    validateType(DataType.INT32, ndArray);
                    inputTensorValues = inputTensorValuesPtr;
                    sizeInBytes = size * 4;
                    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_INT64:
                    validateType(DataType.INT64, ndArray);
                    inputTensorValues = inputTensorValuesPtr;
                    sizeInBytes = size * 8;
                    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_STRING:
                    validateType(DataType.INT8, ndArray);
                    inputTensorValues = inputTensorValuesPtr;
                    sizeInBytes = size;
                    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_BOOL:
                    validateType(DataType.BOOL, ndArray);
                    inputTensorValues = inputTensorValuesPtr; //Casting Boolean to Bool here, sizes could different on some platforms
                    sizeInBytes = size;
                    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_FLOAT16:
                    validateType(DataType.FLOAT16, ndArray);
                    inputTensorValues = inputTensorValuesPtr;
                    sizeInBytes = size * 2;
                    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_DOUBLE:
                    validateType(DataType.DOUBLE, ndArray);
                    inputTensorValues = inputTensorValuesPtr;
                    sizeInBytes = size * 8;
                    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_UINT32:
                    validateType(DataType.UINT32, ndArray);
                    inputTensorValues = inputTensorValuesPtr;
                    sizeInBytes = size * 4;
                    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_UINT64:
                    validateType(DataType.UINT64, ndArray);
                    inputTensorValues = inputTensorValuesPtr;
                    sizeInBytes = size * 8;
                    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_BFLOAT16:
                    validateType(DataType.BFLOAT16, ndArray);
                    inputTensorValues = inputTensorValuesPtr;
                    sizeInBytes = size * 2;
                    break;
                default:
                    throw new RuntimeException("Unsupported data type encountered");
            }
            return Value.CreateTensor(memoryInfo.asOrtMemoryInfo(), inputTensorValues, sizeInBytes, dims, dims.capacity(), type);
        }

        private DataBuffer getDataBuffer(Value tens) {
            try (PointerScope scope = new PointerScope()) {
                DataBuffer buffer = null;
                int type = tens.GetTensorTypeAndShapeInfo().GetElementType();
                long size = tens.GetTensorTypeAndShapeInfo().GetElementCount();
                switch (type) {
                    case ONNX_TENSOR_ELEMENT_DATA_TYPE_FLOAT:
                        FloatPointer pFloat = tens.GetTensorMutableDataFloat().capacity(size);
                        FloatIndexer floatIndexer = FloatIndexer.create(pFloat);
                        buffer = Nd4j.createBuffer(pFloat, DataType.FLOAT, size, floatIndexer);
                        break;
                    case ONNX_TENSOR_ELEMENT_DATA_TYPE_UINT8:
                        BytePointer pUint8 = tens.GetTensorMutableDataUByte().capacity(size);
                        Indexer uint8Indexer = ByteIndexer.create(pUint8);
                        buffer = Nd4j.createBuffer(pUint8, DataType.UINT8, size, uint8Indexer);
                        break;
                    case ONNX_TENSOR_ELEMENT_DATA_TYPE_INT8:
                        BytePointer pInt8 = tens.GetTensorMutableDataByte().capacity(size);
                        Indexer int8Indexer = ByteIndexer.create(pInt8);
                        buffer = Nd4j.createBuffer(pInt8, DataType.UINT8, size, int8Indexer);
                        break;
                    case ONNX_TENSOR_ELEMENT_DATA_TYPE_UINT16:
                        ShortPointer pUint16 = tens.GetTensorMutableDataUShort().capacity(size);
                        Indexer uint16Indexer = ShortIndexer.create(pUint16);
                        buffer = Nd4j.createBuffer(pUint16, DataType.UINT16, size, uint16Indexer);
                        break;
                    case ONNX_TENSOR_ELEMENT_DATA_TYPE_INT16:
                        ShortPointer pInt16 = tens.GetTensorMutableDataShort().capacity(size);
                        Indexer int16Indexer = ShortIndexer.create(pInt16);
                        buffer = Nd4j.createBuffer(pInt16, DataType.INT16, size, int16Indexer);
                        break;
                    case ONNX_TENSOR_ELEMENT_DATA_TYPE_INT32:
                        IntPointer pInt32 = tens.GetTensorMutableDataInt().capacity(size);
                        Indexer int32Indexer = IntIndexer.create(pInt32);
                        buffer = Nd4j.createBuffer(pInt32, DataType.INT32, size, int32Indexer);
                        break;
                    case ONNX_TENSOR_ELEMENT_DATA_TYPE_INT64:
                        LongPointer pInt64 = tens.GetTensorMutableDataLong().capacity(size);
                        Indexer int64Indexer = LongIndexer.create(pInt64);
                        buffer = Nd4j.createBuffer(pInt64, DataType.INT64, size, int64Indexer);
                        break;
                    case ONNX_TENSOR_ELEMENT_DATA_TYPE_STRING:
                        BytePointer pString = tens.GetTensorMutableDataByte().capacity(size);
                        Indexer stringIndexer = ByteIndexer.create(pString);
                        buffer = Nd4j.createBuffer(pString, DataType.INT8, size, stringIndexer);
                        break;
                    case ONNX_TENSOR_ELEMENT_DATA_TYPE_BOOL:
                        BoolPointer pBool = tens.GetTensorMutableDataBool().capacity(size);
                        Indexer boolIndexer = BooleanIndexer.create(new BooleanPointer(pBool)); //Converting from JavaCPP Bool to Boolean here - C++ bool type size is not defined, could cause problems on some platforms
                        buffer = Nd4j.createBuffer(pBool, DataType.BOOL, size, boolIndexer);
                        break;
                    case ONNX_TENSOR_ELEMENT_DATA_TYPE_FLOAT16:
                        ShortPointer pFloat16 = tens.GetTensorMutableDataShort().capacity(size);
                        Indexer float16Indexer = ShortIndexer.create(pFloat16);
                        buffer = Nd4j.createBuffer(pFloat16, DataType.FLOAT16, size, float16Indexer);
                        break;
                    case ONNX_TENSOR_ELEMENT_DATA_TYPE_DOUBLE:
                        DoublePointer pDouble = tens.GetTensorMutableDataDouble().capacity(size);
                        Indexer doubleIndexer = DoubleIndexer.create(pDouble);
                        buffer = Nd4j.createBuffer(pDouble, DataType.DOUBLE, size, doubleIndexer);
                        break;
                    case ONNX_TENSOR_ELEMENT_DATA_TYPE_UINT32:
                        IntPointer pUint32 = tens.GetTensorMutableDataUInt().capacity(size);
                        Indexer uint32Indexer = IntIndexer.create(pUint32);
                        buffer = Nd4j.createBuffer(pUint32, DataType.UINT32, size, uint32Indexer);
                        break;
                    case ONNX_TENSOR_ELEMENT_DATA_TYPE_UINT64:
                        LongPointer pUint64 = tens.GetTensorMutableDataULong().capacity(size);
                        Indexer uint64Indexer = LongIndexer.create(pUint64);
                        buffer = Nd4j.createBuffer(pUint64, DataType.UINT64, size, uint64Indexer);
                        break;
                    case ONNX_TENSOR_ELEMENT_DATA_TYPE_BFLOAT16:
                        ShortPointer pBfloat16 = tens.GetTensorMutableDataShort().capacity(size);
                        Indexer bfloat16Indexer = ShortIndexer.create(pBfloat16);
                        buffer = Nd4j.createBuffer(pBfloat16, DataType.BFLOAT16, size, bfloat16Indexer);
                        break;
                    default:
                        throw new RuntimeException("Unsupported data type encountered");
                }
                return buffer;
            }
        }
    }
}
