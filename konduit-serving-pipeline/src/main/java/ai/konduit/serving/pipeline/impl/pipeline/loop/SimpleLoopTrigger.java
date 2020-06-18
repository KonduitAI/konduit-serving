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

import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.pipeline.Trigger;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Function;

@Slf4j
public class SimpleLoopTrigger implements Trigger {

    private final Long frequencyMs;
    private Thread thread;

    private Data last;
    private Function<Data,Data> callbackFn;

    public SimpleLoopTrigger(){
        this(null);
    }

    public SimpleLoopTrigger(Long frequencyMs){
        this.frequencyMs = frequencyMs;

        //Start up a new thread for performing inference
        thread = new Thread(new InferenceRunner());
        thread.setDaemon(true); //TODO should this be a daemon thread or not?
        thread.start();
    }

    @Override
    public Data query(Data data) {
        return last;
    }

    @Override
    public void setCallback(Function<Data, Data> callbackFn) {

    }

    private static class InferenceRunner implements Runnable {

        @Override
        public void run() {
            try{
                runHelper();
            } catch (Throwable t){
                log.error("Uncaught exception in SimpleLoopTrigger.InferenceRunner", t);
            }
        }

        public void runHelper(){
            long last = 0;
            while (true){
                long start = System.currentTimeMillis();

                last = System.currentTimeMillis();
            }
        }
    }
}
