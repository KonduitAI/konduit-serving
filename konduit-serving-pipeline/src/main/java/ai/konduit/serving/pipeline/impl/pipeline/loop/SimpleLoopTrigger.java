/*
 *  ******************************************************************************
 *  * Copyright (c) 2020 Konduit K.K.
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

package ai.konduit.serving.pipeline.impl.pipeline.loop;

import ai.konduit.serving.annotation.json.JsonName;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.pipeline.Trigger;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.nd4j.shade.jackson.annotation.JsonIgnoreProperties;
import org.nd4j.shade.jackson.annotation.JsonProperty;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

@lombok.Data
@Slf4j
@JsonIgnoreProperties({"stop", "thread", "exception", "first", "current", "callbackFn"})
@JsonName("SIMPLE_LOOP_TRIGGER")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class SimpleLoopTrigger implements Trigger {

    @EqualsAndHashCode.Include
    @ToString.Include
    protected final Long frequencyMs;

    protected AtomicBoolean stop = new AtomicBoolean();
    protected Thread thread;
    protected Throwable exception;

    protected CountDownLatch first = new CountDownLatch(1);
    protected volatile Data current;
    protected Function<Data,Data> callbackFn;

    public SimpleLoopTrigger(){
        this((Long)null);
    }

    public SimpleLoopTrigger(Integer frequencyMs){
        this(frequencyMs == null ? null : frequencyMs.longValue());
    }

    public SimpleLoopTrigger(@JsonProperty("frequencyMs") Long frequencyMs){
        this.frequencyMs = frequencyMs;
    }

    @Override
    public Data query(Data data) {
        if(stop.get())
            throw new IllegalStateException("Unable to get output after trigger has been stopped");

        if(current == null){
            if(exception != null){
                throw new RuntimeException("Error in Async execution thread", exception);
            } else {
                try {
                    first.await();
                } catch (InterruptedException e){
                    log.error("Error while waiting for first async result", e);
                }
            }
            //Latch was count down. We need to check again for an exception, as an exception could have occurred
            // after the last exception check
            if(current != null){
                return current;
            } else if(exception != null){
                throw new RuntimeException("Error in Async execution thread", exception);
            } else {
                throw new RuntimeException("Unknown error occurred: current Data is null but no exception was thrown by async executioner");
            }
        }


        return current;
    }

    @Override
    public void setCallback(@NonNull Function<Data, Data> callbackFn) {
        this.callbackFn = callbackFn;
        if(thread != null){
            stop.set(true);
            thread.interrupt();
        }

        stop = new AtomicBoolean();
        current = null;
        first = new CountDownLatch(1);
        //Start up a new thread for performing inference
        thread = new Thread(new InferenceRunner(stop, first));
        thread.setDaemon(true); //TODO should this be a daemon thread or not?
        thread.start();
    }

    @Override
    public void stop() {
        stop.set(true);
        if(thread != null){
            thread.interrupt();
        }
    }

    protected long firstRunDelay(){
        return 0;
    }

    protected long nextStart(long lastStart){
        return lastStart + frequencyMs;
    }

    private class InferenceRunner implements Runnable {

        private final AtomicBoolean stop;
        private final CountDownLatch first;

        protected InferenceRunner(AtomicBoolean stop, CountDownLatch first){
            this.stop = stop;
            this.first = first;
        }

        @Override
        public void run() {
            try{
                runHelper();
            } catch (Throwable t){
                log.error("Uncaught exception in SimpleLoopTrigger.InferenceRunner", t);
                exception = t;
                current = null;
            } finally {
                //Count down in case the external thread is waiting at query, and we have an exception at the first iteration
                if(current == null){
                    first.countDown();
                }
            }
        }

        public void runHelper(){
            boolean delay = frequencyMs != null;
            Data empty = Data.empty();
            boolean firstExec = true;
            long firstRunDelay = firstRunDelay();
            while (!stop.get()){
                if(firstExec && firstRunDelay > 0){
                    //For TimeLoopTrigger, which has an offset
                    try {
                        Thread.sleep(firstRunDelay);
                    } catch (InterruptedException e){
                        log.error("Received InterruptedException in " + getClass().getName() + " - stopping thread", e);
                        break;
                    }
                }

                long start = delay ? System.currentTimeMillis() : 0L;
                current = callbackFn.apply(empty);
                if(firstExec) {
                    first.countDown();
                    firstExec = false;
                }

                if(delay && !stop.get()) {
                    long nextStart = nextStart(start);
                    long now = System.currentTimeMillis();
                    if(nextStart > now){
                        long sleep = nextStart - now;
                        try {
                            Thread.sleep(sleep);
                        } catch (InterruptedException e){
                            if(!stop.get()) {
                                log.error("Received InterruptedException in SimpleLoopTrigger - stopping thread", e);
                            }
                            break;
                        }
                    }
                }
            }
        }
    }
}
