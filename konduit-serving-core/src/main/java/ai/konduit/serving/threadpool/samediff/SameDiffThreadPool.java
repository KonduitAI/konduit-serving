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

package ai.konduit.serving.threadpool.samediff;


import ai.konduit.serving.model.loader.ModelLoader;
import ai.konduit.serving.threadpool.samediff.observables.BatchedSameDiffInferenceObservable;
import ai.konduit.serving.threadpool.samediff.observables.SameDiffObservable;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.deeplearning4j.parallelism.inference.InferenceMode;
import org.deeplearning4j.parallelism.inference.observers.BasicInferenceObserver;
import org.nd4j.autodiff.execution.NativeGraphExecutioner;
import org.nd4j.autodiff.execution.conf.ExecutionMode;
import org.nd4j.autodiff.execution.conf.ExecutorConfiguration;
import org.nd4j.autodiff.execution.conf.OutputMode;
import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.executioner.OpExecutioner;
import org.nd4j.linalg.factory.Nd4j;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Observer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class is simple wrapper for
 * PMMLThreadPool using batched input
 * Adapted from {@link org.deeplearning4j.parallelism.ParallelInference}
 *
 * @author Adam Gibson
 */
@Slf4j
public class SameDiffThreadPool {
    public final static int DEFAULT_NUM_WORKERS = Nd4j.getAffinityManager().getNumberOfDevices();
    public final static int DEFAULT_BATCH_LIMIT = 32;
    public final static InferenceMode DEFAULT_INFERENCE_MODE = InferenceMode.BATCHED;
    public final static int DEFAULT_QUEUE_LIMIT = 64;
    private static ExecutorConfiguration configuration = ExecutorConfiguration.builder()
            .executionMode(ExecutionMode.SEQUENTIAL)
            .profilingMode(OpExecutioner.ProfilingMode.DISABLED)
            .gatherTimings(true)
            .outputMode(OutputMode.IMPLICIT)
            .build();
    private ModelLoader<SameDiff> sameDiffModelLoader;
    private long nanos;
    private int workers;
    private int batchLimit;
    private InferenceMode inferenceMode;
    private int queueLimit;
    // this queue
    private BlockingQueue<SameDiffObservable> observables;
    private SameDiff replicatedModel;
    private NativeGraphExecutioner nativeGraphExecutioner;
    private List<String> inputNames, outputNames;
    private InferenceWorker[] zoo;
    private ObservablesProvider provider;


    protected SameDiffThreadPool() {
        //
    }

    protected void init() throws Exception {
        nativeGraphExecutioner = new NativeGraphExecutioner();
        observables = new LinkedBlockingQueue<>(queueLimit);
        SameDiff graphHolder = sameDiffModelLoader.loadModel();
        this.replicatedModel = graphHolder;

        int numDevices = Nd4j.getAffinityManager().getNumberOfDevices();
        int currentDevice = Nd4j.getAffinityManager().getDeviceForCurrentThread();
        AtomicBoolean assignedRoot = new AtomicBoolean(false);

        zoo = new InferenceWorker[workers];
        for (int i = 0; i < workers; i++) {
            int cDevice = i % numDevices;
            boolean cRoot = !assignedRoot.get() && cDevice == currentDevice;
            assignedRoot.compareAndSet(false, cRoot);

            zoo[i] = new InferenceWorker(i, observables, true, sameDiffModelLoader);


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
        return replicatedModel.inputs().toArray(new String[0]);
    }

    public String[] outputNames() {
        return replicatedModel.outputs().toArray(new String[0]);
    }

    protected long getWorkerCounter(int workerIdx) {
        return zoo[workerIdx].getCounterValue();
    }

    /**
     * This method gracefully shuts down PMMLThreadPool instance
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
    public INDArray[] output(INDArray[] input) {
        // basically, depending on model type we either throw stuff to specific model, or wait for batch

        BasicInferenceObserver observer = new BasicInferenceObserver();
        BatchedSameDiffInferenceObservable observable;


        if (inferenceMode == InferenceMode.SEQUENTIAL) {
            observable = new BatchedSameDiffInferenceObservable(input);
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
        private ModelLoader<SameDiff> tensorflowModelLoader;
        private int workers = DEFAULT_NUM_WORKERS;
        private int batchLimit = DEFAULT_BATCH_LIMIT;
        private InferenceMode inferenceMode = DEFAULT_INFERENCE_MODE;
        private int queueLimit = DEFAULT_QUEUE_LIMIT;
        private List<String> inputNames, outputNames;

        public Builder(@NonNull ModelLoader<SameDiff> tensorflowModelLoader) {
            this.tensorflowModelLoader = tensorflowModelLoader;
        }


        /**
         * Specify the input names for the graph.
         *
         * @param inputNames teh input names to use
         * @return the builder
         */
        public Builder inputNames(List<String> inputNames) {
            this.inputNames = inputNames;
            return this;
        }

        /**
         * Specify the output names to use
         * for the graph
         *
         * @param outputNames the output names to use
         * @return the builder
         */
        public Builder outputNames(List<String> outputNames) {
            this.outputNames = outputNames;
            return this;
        }

        /**
         * This method allows you to define mode that'll be used during inference. Options are:
         * <p>
         * SEQUENTIAL: Input will be sent to last-used worker unmodified.
         * BATCHED: Multiple inputs will be packed into single batch, and
         * sent to last-used device.
         *
         * @param inferenceMode the inference mdoe
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
         * @param workers the number of workers to run
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
         * @param limit the limit for the number of in queued up
         *              input samples
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
         * @param limit the limit for the buffer queue size
         * @return the builder
         */
        public Builder queueLimit(int limit) {
            if (limit < 1)
                throw new IllegalStateException("Queue limit should be positive value");

            this.queueLimit = limit;
            return this;
        }

        /**
         * This method builds new PMMLThreadPool instance
         *
         * @return the built thread pool
         */
        public SameDiffThreadPool build() {
            SameDiffThreadPool inference = new SameDiffThreadPool();
            inference.batchLimit = this.batchLimit;
            inference.queueLimit = this.queueLimit;
            inference.inferenceMode = this.inferenceMode;
            inference.workers = this.workers;
            inference.sameDiffModelLoader = this.tensorflowModelLoader;
            inference.inputNames = inputNames;
            inference.outputNames = outputNames;
            try {
                inference.init();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return inference;
        }
    }

    protected static class ObservablesProvider {
        private final Object locker = new Object();
        private BlockingQueue<SameDiffObservable> targetQueue;
        private long nanos;
        private int batchLimit;
        private volatile BatchedSameDiffInferenceObservable currentObservable;

        protected ObservablesProvider(long nanos, int batchLimit, @NonNull BlockingQueue<SameDiffObservable> queue) {
            this.targetQueue = queue;
            this.nanos = nanos;
            this.batchLimit = batchLimit;
        }

        protected BatchedSameDiffInferenceObservable setInput(@NonNull Observer observer, INDArray[] input) {
            synchronized (locker) {
                boolean isNew = false;
                if (currentObservable == null || currentObservable.getCounter() >= batchLimit
                        || currentObservable.isLocked()) {
                    isNew = true;
                    currentObservable = new BatchedSameDiffInferenceObservable();
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
        private BlockingQueue<SameDiffObservable> inputQueue;
        private AtomicBoolean shouldWork = new AtomicBoolean(true);
        private AtomicBoolean isStopped = new AtomicBoolean(false);
        private AtomicLong counter = new AtomicLong(0);
        private boolean rootDevice;
        private SameDiff replicatedModel;

        private InferenceWorker(int id, @NonNull BlockingQueue inputQueue, boolean rootDevice, @NonNull ModelLoader<SameDiff> modelLoader) {
            this.inputQueue = inputQueue;
            this.rootDevice = rootDevice;
            this.setDaemon(true);
            try {
                replicatedModel = modelLoader.loadModel();
            } catch (Exception e) {
                log.error("Unable to load samediff model", e);
            }

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
                    SameDiffObservable request = inputQueue.take();

                    if (request != null) {
                        counter.incrementAndGet();


                        INDArray[] batches = request.getInputBatches();
                        if (batches == null || batches.length < 1) {
                            log.warn("Batch length was zero. Skipping.");
                            continue;
                        }
                        log.debug("Received batches");
                        try {
                            Map<String, INDArray> inputs = new LinkedHashMap<>(batches.length);
                            for (int i = 0; i < inputNames.size(); i++) {
                                inputs.put(inputNames.get(i), batches[i]);
                                replicatedModel.associateArrayWithVariable(batches[i], inputNames.get(i));
                            }

                            INDArray[] indArrays1 = nativeGraphExecutioner.executeGraph(replicatedModel, configuration);

                            log.debug("Running graph with inputs " + inputNames);
                            request.setOutputBatches(indArrays1);

                        } catch (Exception e) {
                            log.error("Exception found", e);
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
