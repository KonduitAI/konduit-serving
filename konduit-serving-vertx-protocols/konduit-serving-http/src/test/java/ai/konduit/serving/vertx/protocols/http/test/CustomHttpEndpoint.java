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

package ai.konduit.serving.vertx.protocols.http.test;

import ai.konduit.serving.endpoint.Endpoint;
import ai.konduit.serving.endpoint.HttpEndpoints;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;

import java.util.Arrays;
import java.util.List;

public class CustomHttpEndpoint implements HttpEndpoints {
    @Override
    public List<Endpoint> endpoints(Pipeline p, PipelineExecutor pe) {
        return Arrays.asList(new CustomPostEndpoint(), new CustomGetEndpoint(pe));
    }
}
