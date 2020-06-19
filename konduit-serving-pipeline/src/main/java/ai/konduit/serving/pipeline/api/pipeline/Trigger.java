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

package ai.konduit.serving.pipeline.api.pipeline;

import ai.konduit.serving.pipeline.api.TextConfig;
import ai.konduit.serving.pipeline.api.data.Data;
import org.nd4j.shade.jackson.annotation.JsonTypeInfo;

import java.util.function.Function;

import static org.nd4j.shade.jackson.annotation.JsonTypeInfo.Id.NAME;

/**
 * Trigger is used with {@link ai.konduit.serving.pipeline.impl.pipeline.AsyncPipeline}, and determines when/how
 * the underlying pipeline will be called, which may be independent
 * For example, a Trigger could be used to implement behaviour such as: "execute 5 times every second irrespective of
 * whether the pipeline is queried by the user" (for example, for processing data from a webcam or IP camera)<br>
 * <p>
 * Trigger can be used to implement behaviour such as:
 * (a) Execute the underlying pipeline every N milliseconds, and return the last Data output when actually queried<br>
 * (b) Execute the pipeline at most N times per second (and return the cached value if not)<br>
 *
 * @author Alex Black
 */
@JsonTypeInfo(use = NAME, property = "@type")
public interface Trigger extends TextConfig {

    /**
     * This method is called whenever the AsyncPipeline's exec(Data) method is called
     *
     * @param data The input data
     * @return The output (possibly cached from async execution)
     */
    Data query(Data data);

    /**
     * The callback function to use. This method is called when the AsyncPipeline is set up for execution;
     * the function is used to execute the underlying pipeline.
     *
     * @param callbackFn Function to use to perform execution of the underlying pipeline
     */
    void setCallback(Function<Data, Data> callbackFn);

    /**
     * Stop the underlying execution, if any
     */
    void stop();

}
