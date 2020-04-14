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

package ai.konduit.serving;

import ai.konduit.serving.config.MemMapConfig;
import ai.konduit.serving.config.ServingConfig;
import ai.konduit.serving.config.TextConfig;
import ai.konduit.serving.pipeline.PipelineStep;
import ai.konduit.serving.util.ObjectMappers;
import lombok.*;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InferenceConfiguration implements Serializable, TextConfig {

    @Singular
    private List<PipelineStep> steps;
    private ServingConfig servingConfig;
    private MemMapConfig memMapConfig;

    /**
     * Create a configuration from  a yaml string
     *
     * @param yaml the yaml to create from
     * @return the initialized object from the yaml content
     */
    public static InferenceConfiguration fromYaml(String yaml) {
        return ObjectMappers.fromYaml(yaml, InferenceConfiguration.class);
    }

    /**
     * Create a {@link InferenceConfiguration}
     * from a json string
     *
     * @param json the json to create the configuration from
     * @return InferenceConfiguration
     */
    public static InferenceConfiguration fromJson(String json) {
        return ObjectMappers.fromJson(json, InferenceConfiguration.class);
    }
}
