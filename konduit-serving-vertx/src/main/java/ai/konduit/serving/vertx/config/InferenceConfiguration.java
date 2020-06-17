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
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "The main object that's used to configure the whole konduit serving pipeline and the server itself.")
public class InferenceConfiguration implements Serializable, TextConfig {

    @Builder.Default
    @Schema(description = "Server host", defaultValue = "localhost")
    private String host = "localhost";

    @Builder.Default
    @Schema(description = "Server port. 0 means that a random port will be selected.", defaultValue = "0")
    private int port = 0;

    @Builder.Default
    @Schema(description = "Server type.", defaultValue = "HTTP")
    private ServerProtocol protocol = ServerProtocol.HTTP;

    @Schema(description = "List of custom endpoint class names that are configured to " +
            "provide custom endpoints functionality (fully qualified Java path - for example com.mycompany.MyEndpointsClass).")
    private List<String> customEndpoints;

    @Schema(description = "The main konduit serving pipeline configuration.")
    private Pipeline pipeline;

    public static InferenceConfiguration fromJson(String json){
        return ObjectMappers.fromJson(json, InferenceConfiguration.class);
    }

    public static InferenceConfiguration fromYaml(String yaml){
        return ObjectMappers.fromYaml(yaml, InferenceConfiguration.class);
    }
}
