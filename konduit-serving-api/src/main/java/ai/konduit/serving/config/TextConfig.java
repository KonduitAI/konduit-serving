/* ******************************************************************************
 * Copyright (c) 2020 Konduit K.K.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/
package ai.konduit.serving.config;

import ai.konduit.serving.config.metrics.impl.ClassificationMetricsConfig;
import ai.konduit.serving.util.ObjectMappers;
import org.nd4j.shade.jackson.annotation.JsonSubTypes;
import org.nd4j.shade.jackson.annotation.JsonTypeInfo;

import static org.nd4j.shade.jackson.annotation.JsonTypeInfo.As.PROPERTY;
import static org.nd4j.shade.jackson.annotation.JsonTypeInfo.Id.NAME;

/**
 * TextConfig is an interface for any configuration in Konduit Serving that should be convertable to/from JSON and YAML
 * This interface does two things:
 * (a) Adds default toJson() and toYaml() methods to the class, using Jackson
 * (b) Is used in testing to provide coverage tracking for to/from JSON/YAML testing
 *
 * @author Alex Black
 */
@JsonSubTypes({
        @JsonSubTypes.Type(value = ClassificationMetricsConfig.class, name = "ClassificationMetricsConfig"),
})
@JsonTypeInfo(use = NAME, include = PROPERTY)
public interface TextConfig {

    /**
     * Convert a configuration to a JSON string
     *
     * @return convert this object to a string
     */
    default String toJson() {
        return ObjectMappers.toJson(this);
    }

    /**
     * Convert a configuration to a YAML string
     *
     * @return the yaml representation of this configuration
     */
    default String toYaml() {
        return ObjectMappers.toYaml(this);
    }

}
