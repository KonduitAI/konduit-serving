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

import org.bytedeco.javacpp.*;
import org.bytedeco.javacpp.indexer.*;
import org.bytedeco.onnxruntime.*;
import static org.bytedeco.onnxruntime.global.onnxruntime.*;
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
    static final OrtApi g_ort = OrtGetApiBase().GetApi().call(ORT_API_VERSION);

    protected ONNXThreadPool() {
        //
    }

    protected static void CheckStatus(OrtStatus status) {

        if (status != null && !status.isNull()) {
            String msg = g_ort.GetErrorMessage().call(status).getString();

	    log.error("Error occurred doing inference", msg);

            g_ort.ReleaseStatus().call(status);
	    throw new RuntimeException(msg);
          }
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
    public Map<String, INDArray> output(Map<String, INDArray> input) {
        // basically, depending on model type we either throw stuff to specific model, or wait for batch

        BasicInferenceObserver observer = new BasicInferenceObserver();
        OnnxObservable observable;


	//Batch of 1
	List<Map<String, INDArray>> inputs = new ArrayList<Map<String, INDArray>>(1);
	inputs.add(input);

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
            //observer.wait();

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
//        private Session replicatedModel;
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
 try (PointerScope scope = new PointerScope()) {
                // model should be replicated & initialized here
                Session replicatedModel = onnxModelLoader.loadModel();

                AllocatorWithDefaultOptions allocator = new AllocatorWithDefaultOptions();	

		Long num_input_nodes = replicatedModel.GetInputCount();
		Long num_output_nodes = replicatedModel.GetOutputCount();
                PointerPointer<BytePointer> input_node_names = new PointerPointer(num_input_nodes);
                PointerPointer<BytePointer> output_node_names = new PointerPointer(num_output_nodes);

		LongPointer[] input_node_dims = new LongPointer[num_input_nodes.intValue()];

		for (int i = 0; i < num_output_nodes; i++) {
	            BytePointer output_name = replicatedModel.GetOutputName(i, allocator.asOrtAllocator());
                    output_node_names.put(i, output_name);
		}
		long[] inputSizes = new long[num_input_nodes.intValue()];
		int[] inputTypes = new int[num_input_nodes.intValue()];

                for (int i = 0; i < num_input_nodes; i++) {
		    BytePointer input_name = replicatedModel.GetInputName(i, allocator.asOrtAllocator());

		    input_node_names.put(i, input_name);

                    TypeInfo typeInfo = replicatedModel.GetInputTypeInfo(i);
		    inputTypes[i] = typeInfo.GetONNXType();

		    TensorTypeAndShapeInfo tensor_info = typeInfo.GetTensorTypeAndShapeInfo();
		    input_node_dims[i] = tensor_info.GetShape();

	            int acc = 1;
		    for (long j = 0; j < input_node_dims[i].capacity(); j++)
	                acc *= input_node_dims[i].get(j);

		    inputSizes[i] = acc;
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

//			        System.out.println("First input: " + inputArray.getFloat(0));
			        Value[] inputTensors = new Value[num_input_nodes.intValue()];

                                for (int i = 0; i < num_input_nodes; i++) {
		                  BytePointer input_name = input_node_names.get(BytePointer.class, i);

				  Value inputTensor = getTensor(inBatch.get(input_name.getString()), inputTypes[i], inputSizes[i], input_node_dims[i]);
//				  System.out.println("Input element count: " + inputTensor.GetTensorTypeAndShapeInfo().GetElementCount());
				  inputTensors[i] = inputTensor;

				}
				//TODO: Pass ValueVector here when possible
				PointerPointer inputTensorsPP = new PointerPointer(inputTensors);
				ValueVector outputVector = replicatedModel.Run(new RunOptions(), input_node_names, inputTensors[0], num_input_nodes, output_node_names, num_output_nodes);

				Map<String, INDArray> output = new HashMap<String, INDArray>();

                                for (int i = 0; i < num_output_nodes; i++) {
					Value outValue = outputVector.get(i);
					DataBuffer buffer = getDataBuffer(outValue);
					INDArray outArray = Nd4j.create(buffer);
//					System.out.println("Output element count " + outValue.GetTensorTypeAndShapeInfo().GetElementCount());
					output.put((output_node_names.get(BytePointer.class, i)).getString(), outArray);
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

	private Value getTensor(INDArray ndArray, int type, long size, LongPointer dims){
            Pointer inputTensorValuesPtr = ndArray.data().pointer();

	    long sizeInBytes = size;
            MemoryInfo memory_info = MemoryInfo.CreateCpu(OrtArenaAllocator, OrtMemTypeDefault);
	    Pointer input_tensor_values = null;
            switch (type) {
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_FLOAT:
                    if(!ndArray.dataType().equals(DataType.FLOAT)) throw new RuntimeException("INDArray data type does not match ONNX data type");
		    input_tensor_values = (FloatPointer)inputTensorValuesPtr;
		    sizeInBytes = size * 4;
		    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_UINT8:
                    if(!ndArray.dataType().equals(DataType.UINT8)) throw new RuntimeException("INDArray data type does not match ONNX data type");
		    input_tensor_values = (BytePointer)inputTensorValuesPtr;
		    sizeInBytes = size;
		    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_INT8:
                    if(!ndArray.dataType().equals(DataType.INT8)) throw new RuntimeException("INDArray data type does not match ONNX data type");
		    input_tensor_values = (BytePointer)inputTensorValuesPtr;
		    sizeInBytes = size;
		    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_UINT16:
                    if(!ndArray.dataType().equals(DataType.UINT16)) throw new RuntimeException("INDArray data type does not match ONNX data type");
		    input_tensor_values = (ShortPointer)inputTensorValuesPtr;
		    sizeInBytes = size * 2;
		    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_INT16:
                    if(!ndArray.dataType().equals(DataType.INT16)) throw new RuntimeException("INDArray data type does not match ONNX data type");
		    input_tensor_values = (ShortPointer)inputTensorValuesPtr;
		    sizeInBytes = size * 2;
		    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_INT32:
                    if(!ndArray.dataType().equals(DataType.INT32)) throw new RuntimeException("INDArray data type does not match ONNX data type");
		    input_tensor_values = (IntPointer)inputTensorValuesPtr;
		    sizeInBytes = size * 4;
		    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_INT64:
                    if(!ndArray.dataType().equals(DataType.INT64)) throw new RuntimeException("INDArray data type does not match ONNX data type");
		    input_tensor_values = (LongPointer)inputTensorValuesPtr;
		    sizeInBytes = size * 8;
		    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_STRING:
                    if(!ndArray.dataType().equals(DataType.INT8)) throw new RuntimeException("INDArray data type does not match ONNX data type");
		    input_tensor_values = (BytePointer)inputTensorValuesPtr;
		    sizeInBytes = size;
		    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_BOOL:
                    if(!ndArray.dataType().equals(DataType.BOOL)) throw new RuntimeException("INDArray data type does not match ONNX data type");
		    input_tensor_values = (BoolPointer)inputTensorValuesPtr; //Casting Boolean to Bool here, sizes could different on some platforms
		    sizeInBytes = size;
		    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_FLOAT16:
                    if(!ndArray.dataType().equals(DataType.HALF)) throw new RuntimeException("INDArray data type does not match ONNX data type");
		    input_tensor_values = (ShortPointer)inputTensorValuesPtr;
		    sizeInBytes = size * 2;
		    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_DOUBLE:
                    if(!ndArray.dataType().equals(DataType.DOUBLE)) throw new RuntimeException("INDArray data type does not match ONNX data type");
		    input_tensor_values = (DoublePointer)inputTensorValuesPtr;
		    sizeInBytes = size * 8;
		    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_UINT32:
                    if(!ndArray.dataType().equals(DataType.UINT32)) throw new RuntimeException("INDArray data type does not match ONNX data type");
		    input_tensor_values = (IntPointer)inputTensorValuesPtr;
		    sizeInBytes = size * 4;
		    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_UINT64:
                    if(!ndArray.dataType().equals(DataType.UINT64)) throw new RuntimeException("INDArray data type does not match ONNX data type");
		    input_tensor_values = (LongPointer)inputTensorValuesPtr;
		    sizeInBytes = size * 8;
		    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_BFLOAT16:
                    if(!ndArray.dataType().equals(DataType.BFLOAT16)) throw new RuntimeException("INDArray data type does not match ONNX data type");
		    input_tensor_values = (ShortPointer)inputTensorValuesPtr;
		    sizeInBytes = size * 2;
		    break;
		default:
		    throw new RuntimeException("Unsupported data type encountered");
	    }
	    Value inputTensor = Value.CreateTensor(memory_info.asOrtMemoryInfo(), input_tensor_values, sizeInBytes, dims, dims.capacity(), type);
            return inputTensor;
	}

	private DataBuffer getDataBuffer(Value tens){

	    DataBuffer buffer = null;
	    int type = tens.GetTensorTypeAndShapeInfo().GetElementType();
	    long size = tens.GetTensorTypeAndShapeInfo().GetElementCount();
            switch (type) {
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_FLOAT:
		    FloatPointer origPFloat = tens.GetTensorMutableDataFloat();

		    //TODO: avoid using a second float pointer here
		    FloatPointer pFloat = new FloatPointer(size);
                    for(int i = 0; i < size; i++){

                      pFloat.put(i, origPFloat.get(i));
                    }
		    FloatIndexer floatIndexer = FloatIndexer.create(pFloat);
		    buffer = Nd4j.createBuffer(pFloat, DataType.FLOAT, size, floatIndexer);
		    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_UINT8:
		    BytePointer pUint8 = tens.GetTensorMutableDataUByte();
		    Indexer uint8Indexer = ByteIndexer.create(pUint8);
		    buffer = Nd4j.createBuffer(pUint8, DataType.UINT8, pUint8.capacity(), uint8Indexer);
		    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_INT8:
		    BytePointer pInt8 = tens.GetTensorMutableDataByte();
		    Indexer int8Indexer = ByteIndexer.create(pInt8);
		    buffer = Nd4j.createBuffer(pInt8, DataType.UINT8, pInt8.capacity(), int8Indexer);
		    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_UINT16:
		    ShortPointer pUint16 = tens.GetTensorMutableDataUShort();
		    Indexer uint16Indexer = ShortIndexer.create(pUint16);
		    buffer = Nd4j.createBuffer(pUint16, DataType.UINT16, pUint16.capacity(), uint16Indexer);
		    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_INT16:
		    ShortPointer pInt16 = tens.GetTensorMutableDataShort();
		    Indexer int16Indexer = ShortIndexer.create(pInt16);
		    buffer = Nd4j.createBuffer(pInt16, DataType.INT16, pInt16.capacity(), int16Indexer);
		    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_INT32:
		    IntPointer pInt32 = tens.GetTensorMutableDataInt();
		    Indexer int32Indexer = IntIndexer.create(pInt32);
		    buffer = Nd4j.createBuffer(pInt32, DataType.INT32, pInt32.capacity(), int32Indexer);
		    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_INT64:
		    LongPointer pInt64 = tens.GetTensorMutableDataLong();
		    Indexer int64Indexer = LongIndexer.create(pInt64);
		    buffer = Nd4j.createBuffer(pInt64, DataType.INT64, pInt64.capacity(), int64Indexer);
		    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_STRING:
		    BytePointer pString = tens.GetTensorMutableDataByte();
		    Indexer stringIndexer = ByteIndexer.create(pString);
		    buffer = Nd4j.createBuffer(pString, DataType.INT8, pString.capacity(), stringIndexer);
		    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_BOOL:
		    BoolPointer pBool = tens.GetTensorMutableDataBool();
		    Indexer boolIndexer = BooleanIndexer.create(new BooleanPointer(pBool)); //Converting from JavaCPP Bool to Boolean here - C++ bool type size is not defined, could cause problems on some platforms
		    buffer = Nd4j.createBuffer(pBool, DataType.BOOL, pBool.capacity(), boolIndexer);
		    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_FLOAT16:
		    ShortPointer pFloat16 = tens.GetTensorMutableDataShort();
		    Indexer float16Indexer = ShortIndexer.create(pFloat16);
		    buffer = Nd4j.createBuffer(pFloat16, DataType.FLOAT16, pFloat16.capacity(), float16Indexer);
		    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_DOUBLE:
		    DoublePointer pDouble = tens.GetTensorMutableDataDouble();
		    Indexer doubleIndexer = DoubleIndexer.create(pDouble);
		    buffer = Nd4j.createBuffer(pDouble, DataType.DOUBLE, pDouble.capacity(), doubleIndexer);
		    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_UINT32:
		    IntPointer pUint32 = tens.GetTensorMutableDataUInt();
		    Indexer uint32Indexer = IntIndexer.create(pUint32);
		    buffer = Nd4j.createBuffer(pUint32, DataType.UINT32, pUint32.capacity(), uint32Indexer);
		    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_UINT64:
		    LongPointer pUint64 = tens.GetTensorMutableDataULong();
		    Indexer uint64Indexer = LongIndexer.create(pUint64);
		    buffer = Nd4j.createBuffer(pUint64, DataType.UINT64, pUint64.capacity(), uint64Indexer);
		    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_BFLOAT16:
		    ShortPointer pBfloat16 = tens.GetTensorMutableDataShort();
		    Indexer bfloat16Indexer = ShortIndexer.create(pBfloat16);
		    buffer = Nd4j.createBuffer(pBfloat16, DataType.BFLOAT16, pBfloat16.capacity(), bfloat16Indexer);
		    break;
		default:
		    throw new RuntimeException("Unsupported data type encountered");
	    }
            return buffer;
	}
    }
}
