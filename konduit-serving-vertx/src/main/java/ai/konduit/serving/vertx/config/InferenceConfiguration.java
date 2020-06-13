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

package ai.konduit.serving.vertx.config;

import ai.konduit.serving.pipeline.api.TextConfig;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.util.ObjectMappers;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InferenceConfiguration implements Serializable, TextConfig {

    @Builder.Default
    private String host = "localhost";
    @Builder.Default
    private int port = 0;
    @Builder.Default
    private ServerProtocol protocol = ServerProtocol.HTTP;

    private List<String> customEndpoints;

    private Pipeline pipeline;

    public static InferenceConfiguration fromJson(String json){
        return ObjectMappers.fromJson(json, InferenceConfiguration.class);
    }

    public static InferenceConfiguration fromYaml(String yaml){
        return ObjectMappers.fromYaml(yaml, InferenceConfiguration.class);
    }
}
