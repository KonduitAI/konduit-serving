/*
 *
 *  * ******************************************************************************
 *  *  * Copyright (c) 2015-2019 Skymind Inc.
 *  *  * Copyright (c) 2019 Konduit AI.
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

package ai.konduit.serving.threadpool.tensorflow;


import ai.konduit.serving.model.loader.ModelLoader;
import ai.konduit.serving.model.loader.tensorflow.TensorflowGraphHolder;
import ai.konduit.serving.threadpool.tensorflow.conversion.TensorflowConversion;
import ai.konduit.serving.threadpool.tensorflow.conversion.graphrunner.GraphRunner;
import ai.konduit.serving.threadpool.tensorflow.observables.BasicTensorflowInferenceObservable;
import ai.konduit.serving.threadpool.tensorflow.observables.BatchedTensorflowInferenceObservable;
import ai.konduit.serving.threadpool.tensorflow.observables.TensorflowObservable;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.tensorflow.TF_Tensor;
import org.deeplearning4j.parallelism.inference.InferenceMode;
import org.deeplearning4j.parallelism.inference.observers.BasicInferenceObserver;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Observer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.bytedeco.tensorflow.global.tensorflow.TF_DeleteTensor;

/**
 * This class is simple wrapper for
 * PMMLThreadPool using batched input
 * Adapted from {@link org.deeplearning4j.parallelism.ParallelInference}
 * @author Adam Gibson
 */
@Slf4j
public class TensorFlowThreadPool {
    private ModelLoader<TensorflowGraphHolder> tensorFlowModelLoader;
    private long nanos;
    private int workers;
    private int batchLimit;
    private InferenceMode inferenceMode;
    private int queueLimit;
    // this queue
    private BlockingQueue<TensorflowObservable> observables;
    private GraphRunner replicatedModel;
    private InferenceWorker[] zoo;
    private ObservablesProvider provider;

    private String[] inputNames,outputNames;

    public final static int DEFAULT_NUM_WORKERS = Nd4j.getAffinityManager().getNumberOfDevices();
    public final static int DEFAULT_BATCH_LIMIT = 32;
    public final static InferenceMode DEFAULT_INFERENCE_MODE = InferenceMode.BATCHED;
    public final static int DEFAULT_QUEUE_LIMIT = 64;



    protected TensorFlowThreadPool() {
        //
    }

    protected void init() throws Exception {
        observables = new LinkedBlockingQueue<>(queueLimit);
        TensorflowGraphHolder graphHolder = tensorFlowModelLoader.loadModel();
        log.debug("Initializing graph holder with inputs " + graphHolder.getInputNames() + " and outputSchema " + graphHolder.getOutputNames());
        this.replicatedModel = graphHolder.createRunner();

        inputNames = replicatedModel.getInputOrder().toArray(new String[0]);
        outputNames = replicatedModel.getOutputOrder().toArray(new String[0]);
        if(graphHolder.getSavedModelConfig() == null && (replicatedModel.getOutputOrder() == null || replicatedModel.getInputOrder() == null || replicatedModel.getInputOrder().isEmpty() || replicatedModel.getOutputOrder().isEmpty())) {
            throw new IllegalStateException("Unable to run graph runner. Inputs and outputs are empty. Please check the graph initialization.");
        }

        if(replicatedModel.getInputOrder().isEmpty()) {
            throw new IllegalStateException("Inputs not resolved!");
        }

        if(replicatedModel.getOutputOrder().isEmpty()) {
            throw new IllegalStateException("Outputs not resolved!");
        }

        int numDevices = Nd4j.getAffinityManager().getNumberOfDevices();
        int currentDevice = Nd4j.getAffinityManager().getDeviceForCurrentThread();
        AtomicBoolean assignedRoot = new AtomicBoolean(false);

        zoo = new InferenceWorker[workers];
        for (int i = 0; i < workers; i++) {
            int cDevice = i % numDevices;
            boolean cRoot = !assignedRoot.get() && cDevice == currentDevice;
            assignedRoot.compareAndSet(false, cRoot);

            zoo[i] = new InferenceWorker(i,observables,true, tensorFlowModelLoader);
            zoo[i].setUncaughtExceptionHandler((handler,e) -> {
                log.error("Exception in thread",e);
            });


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

    public String[] inputNames() {
        return inputNames;
    }

    public String[] outputNames() {
        return  outputNames;
    }



    /**
     * This method gracefully shuts down TensorFlowThreadPool instance
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
     * @param input      Input to the network
     * @return Output from the network
     */
    public INDArray[] output(INDArray[] input) {
        // basically, depending on model type we either throw stuff to specific model, or wait for batch
        if(input == null) {
            throw new IllegalArgumentException("No null input allowed.");
        }

        BasicInferenceObserver observer = new BasicInferenceObserver();
        TensorflowObservable observable;


        if (inferenceMode == InferenceMode.SEQUENTIAL) {
            observable = new BasicTensorflowInferenceObservable(input);
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
        private ModelLoader<TensorflowGraphHolder> tensorflowModelLoader;
        private int workers = DEFAULT_NUM_WORKERS;
        private int batchLimit = DEFAULT_BATCH_LIMIT;
        private InferenceMode inferenceMode = DEFAULT_INFERENCE_MODE;
        private int queueLimit = DEFAULT_QUEUE_LIMIT;

        public Builder(@NonNull ModelLoader<TensorflowGraphHolder> tensorflowModelLoader) {
            this.tensorflowModelLoader = tensorflowModelLoader;
        }



        /**
         * This method allows you to define mode that'll be used during inference. Options are:
         *
         * SEQUENTIAL: Input will be sent to last-used worker unmodified.
         * BATCHED: Multiple inputs will be packed into single batch, and
         * sent to last-used device.
         *
         * @param inferenceMode the inference mode
         * @return the builder
         */
        public Builder inferenceMode(@NonNull InferenceMode inferenceMode) {
            this.inferenceMode = inferenceMode;
            return this;
        }



        /**
         * This method defines, how many model copies will be used for inference.
         *
         * PLEASE NOTE: This method primarily suited for multi-GPU systems
         *
         * @param workers the number of workers
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
         *
         * PLEASE NOTE: This value has no effect in
         * SEQUENTIAL inference mode
         *
         * @param limit the limit of input samples
         *              to be queued up at a time
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
         *
         * Default value: 64
         *
         * @param limit th elimit of the buffer queue size
         * @return the builder
         */
        public Builder queueLimit(int limit) {
            if (limit < 1)
                throw new IllegalStateException("Queue limit should be positive value");

            this.queueLimit = limit;
            return this;
        }

        /**
         * This method builds new TensorFlowThreadPool instance
         *
         * @return the build thread pool
         */
        public TensorFlowThreadPool build() {
            TensorFlowThreadPool inference = new TensorFlowThreadPool();
            inference.batchLimit = this.batchLimit;
            inference.queueLimit = this.queueLimit;
            inference.inferenceMode = this.inferenceMode;
            inference.workers = this.workers;
            inference.tensorFlowModelLoader = this.tensorflowModelLoader;
            try {
                inference.init();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return inference;
        }
    }


    /**
     * This class actually does inference with respect to device affinity
     *
     */
    private class InferenceWorker extends Thread implements Runnable {
        private BlockingQueue<TensorflowObservable> inputQueue;
        private AtomicBoolean shouldWork = new AtomicBoolean(true);
        private AtomicBoolean isStopped = new AtomicBoolean(false);
        private AtomicLong counter = new AtomicLong(0);
        private boolean rootDevice;


        private InferenceWorker(int id,@NonNull BlockingQueue inputQueue, boolean rootDevice, @NonNull ModelLoader<TensorflowGraphHolder> modelLoader) {
            this.inputQueue = inputQueue;
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
                while (shouldWork.get()) {
                    TensorflowObservable request = inputQueue.take();

                    if (request != null) {
                        counter.incrementAndGet();

                        INDArray[] batches = request.getInputBatches();
                        if(batches == null) {
                            request.setOutputException(new IllegalArgumentException("No batches found!"));
                            return;
                        }

                        log.debug("Received batches");

                        try {
                            if(replicatedModel.getInputOrder() == null || replicatedModel.getInputOrder().size() != batches.length) {
                                request.setOutputException(new IllegalArgumentException("Inputs did not match input order!"));
                                return;
                            }

                            List<String> inputNames = replicatedModel.getInputOrder();
                            List<String> outputNames = replicatedModel.getOutputOrder();
                            Map<String, TF_Tensor> inputs = new LinkedHashMap<>(batches.length);

                            for (int i = 0; i < replicatedModel.getInputOrder().size(); i++) {
                                inputs.put(inputNames.get(i), TensorflowConversion.getInstance().tensorFromNDArray(batches[i]));
                            }

                            //ensure inputs are recast in case there's a disconnect between the original inputs and the target
                            //input type in the graph
                            long start = System.nanoTime();
                            inputs = replicatedModel.recastInputs(inputs);
                            long end = System.nanoTime();
                            long diff = TimeUnit.NANOSECONDS.toMillis((end - start));
                            log.info("Recast timing in ms " + diff + " and input shape " + batches[0].shapeInfoToString());

                            log.debug("Running graph with inputs "  + inputNames + " and outputSchema " + outputNames);
                            start = System.nanoTime();
                            Map<String, TF_Tensor> outputs = replicatedModel.runTfTensor(inputs);
                            outputs = replicatedModel.recastOutputs(outputs);
                            end = System.nanoTime();
                            diff = TimeUnit.NANOSECONDS.toMillis((end - start));
                            log.info("Raw TF execution  timing in ms " + diff);

                            log.debug("Ran graph with outputSchema " + outputNames);
                            INDArray[] outputsArr = new INDArray[outputs.size()];
                            log.info("Creating new ndarrays from tensor output.");
                            start = System.nanoTime();
                            for (int i = 0; i < outputsArr.length; i++) {
                                outputsArr[i] = TensorflowConversion.getInstance().ndArrayFromTensor(outputs.get(outputNames.get(i)));
                            }

                            end = System.nanoTime();
                            diff = TimeUnit.NANOSECONDS.toMillis((end - start));
                            log.info("NDArray from tensor timing in ms " + diff);

                            request.setOutputBatches(outputsArr);

                            //delete after the batches are done allowing cleanup to happen
                            //while the next execution can begin
                            for(Map.Entry<String,TF_Tensor> entry : inputs.entrySet()) {
                                TF_DeleteTensor(entry.getValue());
                            }


                        }catch (Exception e) {
                            log.error("Exception found",e);
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


    protected static class ObservablesProvider {
        private BlockingQueue<TensorflowObservable> targetQueue;
        private long nanos;
        private int batchLimit;

        private volatile BatchedTensorflowInferenceObservable currentObservable;
        private final Object locker = new Object();

        protected ObservablesProvider(long nanos, int batchLimit, @NonNull BlockingQueue<TensorflowObservable> queue) {
            this.targetQueue = queue;
            this.nanos = nanos;
            this.batchLimit = batchLimit;
        }

        protected BatchedTensorflowInferenceObservable setInput(@NonNull Observer observer, INDArray[] input) {
            synchronized (locker) {
                boolean isNew = false;
                if (currentObservable == null || currentObservable.getCounter() >= batchLimit
                        || currentObservable.isLocked()) {
                    isNew = true;
                    currentObservable = new BatchedTensorflowInferenceObservable();
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


}
