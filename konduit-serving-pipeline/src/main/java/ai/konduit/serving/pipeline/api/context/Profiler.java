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

package ai.konduit.serving.pipeline.api.context;

import java.io.IOException;

/**
 * The Profiler interface is used within each PipelineStep (technically, within each {@link ai.konduit.serving.pipeline.api.step.PipelineStepRunner})
 * for performance analysis and debugging purposes.
 * Specifically, for each event within a PipelineStepRunner such as "load data", "convert input array", etc - developers
 * can profile events based on their start and end times.<br>
 * Events are identified by a String key, and have both start and end times.
 *
 * <p>
 * For example:
 * <pre>
 * {@code
 * @Override
 * public Data exec(Context ctx, Data data){
 *     Profiler p = ctx.profiler();
 *     p.eventStart("DataConversion");
 *     data.getNDArray("myArray").getAs(float[][].class);
 *     p.eventEnd("DataConversion");
 *     ...
 * }}</pre>
 * <p>
 * If the profiler is not enabled globally for the Pipeline execution, profiler calls are a no-op hence have virtually
 * no overhead.
 * <p>
 * Profiler eventStart/End calls can be nested and events need not end before earlier ones start - for example, both
 * {@code eventStart("X"), eventStart("Y"), eventEnd("Y"), eventEnd("X")} and
 * {@code eventStart("X"), eventStart("Y"), eventEnd("X"), eventEnd("Y")} are valid. However,
 * {@code eventEnd("X"), eventStart("X")} is not valid (end before start), which wil log a warning.
 * <p>
 * Note that every eventStart call should have a corresponding eventEnd call some time before the PipelineStepRunner.exec
 * method returns. Any profiler eventStart calls without a corresponding eventEnd call will have eventEnd called once
 * the exec method returns.
 * <p>
 * Event keys are usually the same on all calls of a given PipelineStepRunner's exec method, but this need not be the case
 * in general
 *
 * @author Alex Black
 */
public interface Profiler {

    /**
     * Returns true if the profiler is enabled globally, or false otherwise
     */
    boolean profilerEnabled();

    /**
     * Start the timer for the event with the specified key.
     */
    void eventStart(String key) throws IOException;

    /**
     * End the timer for the event with the specified key.
     * Should be called some time after {@link #eventStart(String)}
     */
    void eventEnd(String key) throws IOException;

}
