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

package ai.konduit.serving.threadpool.onnx.observables;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import org.nd4j.linalg.api.ndarray.INDArray;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Data
@NoArgsConstructor
public class BatchedOnnxInferenceObservable extends Observable implements OnnxObservable {

    private final Object locker = new Object();
    protected Exception exception;
    private List<Map<String, INDArray>> input;
    @Getter
    private long id;
    private List<Map<String, INDArray>> output;
    private AtomicInteger counter = new AtomicInteger(0);
    private ThreadLocal<Integer> position = new ThreadLocal<>();
    private List<int[]> outputBatchInputArrays = new ArrayList<>();
    private ReentrantReadWriteLock realLocker = new ReentrantReadWriteLock();
    private AtomicBoolean isLocked = new AtomicBoolean(false);
    private AtomicBoolean isReadLocked = new AtomicBoolean(false);


    public BatchedOnnxInferenceObservable(List<Map<String, INDArray>> inputs) {
        this.input = inputs;
    }


    @Override
    public void addInput(@NonNull List<Map<String, INDArray>> input) {
        synchronized (locker) {
            if (this.input == null)
                this.input = new ArrayList<>();
            this.input.addAll(input);
            position.set(counter.getAndIncrement());

            if (isReadLocked.get())
                realLocker.readLock().unlock();
        }
        this.input = input;
    }


    @Override
    public List<Map<String, INDArray>> getInputBatches() {
        realLocker.writeLock().lock();
        isLocked.set(true);

        outputBatchInputArrays.clear();

        // this method should pile individual examples into single batch

        if (counter.get() > 1) {

            int pos = 0;
            List<Map<String, INDArray>> out = new ArrayList<>(input);
            realLocker.writeLock().unlock();
            return out;
        } else {
            outputBatchInputArrays.add(new int[]{0, 0});
            realLocker.writeLock().unlock();
            return Collections.singletonList(input.get(0));
        }
    }


    @Override
    public void setOutputBatches(List<Map<String, INDArray>> output) {
        //this method should split batched output INDArray[] into multiple separate INDArrays
        int countNumInputBatches = 0;   //Counter for total number of input batches processed
        this.output = output;
        this.setChanged();
        notifyObservers();
    }

    @Override
    public Exception getOutputException() {
        return exception;
    }

    @Override
    public void setOutputException(Exception e) {
        this.exception = e;
    }

    /**
     * PLEASE NOTE: This method is for tests only
     *
     * @return the outputs of the observable
     */
    protected List<Map<String, INDArray>> getOutputs() {
        return output;
    }

    public void setPosition(int pos) {
        position.set(pos);
    }

    public int getCounter() {
        return counter.get();
    }

    protected void setCounter(int value) {
        counter.set(value);
    }

    public boolean isLocked() {
        boolean lck = !realLocker.readLock().tryLock();

        boolean result = lck || isLocked.get();

        if (!result)
            isReadLocked.set(true);

        return result;
    }


}
