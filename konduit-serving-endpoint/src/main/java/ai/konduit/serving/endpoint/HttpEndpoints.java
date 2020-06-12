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

package ai.konduit.serving.endpoint;

import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;

import java.util.List;

/**
 * HttpEndpoint represents one or more custom HTTP endpoints, specified via InferenceConfiguration.
 * Returns a (possibly null/empty) list of endpoints for the given {@link Pipeline} and {@link PipelineExecutor}
 *
 * @author Alex Black
 */
public interface HttpEndpoints {

    /**
     * @param p  Pipeline for this server
     * @param pe Pipeline executor for this server
     * @return Return the list of custom endpoints for the given Pipeline/PipelineExecutor. May return null/empty
     */
    List<Endpoint> endpoints(Pipeline p, PipelineExecutor pe);

}
