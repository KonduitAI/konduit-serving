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

import ai.konduit.serving.pipeline.PipelineStep;
import ai.konduit.serving.config.ServingConfig;
import lombok.*;
import org.datavec.api.transform.serde.JsonMappers;
import org.nd4j.shade.jackson.core.JsonProcessingException;
import org.nd4j.shade.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InferenceConfiguration implements Serializable  {

    @Singular
    private List<PipelineStep> pipelineSteps;
    private ServingConfig servingConfig;

    /**
     * Returns a serving configuration if one is defined
     * or creaes a new one if it doesn't exist
     * @return the initialized or cached {@link ServingConfig}
     */
    public ServingConfig serving() {
        if(servingConfig == null)
            servingConfig = ServingConfig.builder().build();
        return servingConfig;
    }

    /**
     * Convert a configuration to a json string
     * @return convert this object to a string
     * @throws JsonProcessingException JSON processing exception
     */
    public String toJson() throws JsonProcessingException {
        ObjectMapper objectMapper = JsonMappers.getMapper();
        return objectMapper.writeValueAsString(this);
    }

    /**
     * Convert a configuration to a yaml string
     * @return the yaml representation of this configuration
     * @throws JsonProcessingException if jackson throws an exception
     * serializing this configuration
     */
    public String toYaml() throws JsonProcessingException {
        ObjectMapper mapper = JsonMappers.getMapperYaml();
        return mapper.writeValueAsString(this);
    }

    /**
     * Create a configuration from  a yaml string
     * @param yaml the yaml to create from
     * @return the initialized object from the yaml content
     * @throws IOException if an error occurs while reading/parsing the yaml
     */
    public static InferenceConfiguration fromYaml(String yaml)throws  IOException {
        ObjectMapper mapper = JsonMappers.getMapperYaml();
        return mapper.readValue(yaml,InferenceConfiguration.class);
    }


    /**
     * Create a {@link InferenceConfiguration}
     * from a json string
     * @param json the json to create the configuration from
     * @return InferenceConfiguration
     * @throws IOException I/O exception
     */
    public static InferenceConfiguration fromJson(String json) throws IOException {
        return JsonMappers.getMapper().readValue(json, InferenceConfiguration.class);
    }


}
