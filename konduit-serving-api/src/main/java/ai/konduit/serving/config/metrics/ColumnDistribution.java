/*
 *
 *  * ******************************************************************************
 *  *
 *  *  * Copyright (c) 2022 Konduit K.K.
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
package ai.konduit.serving.config.metrics;

import ai.konduit.serving.config.TextConfig;
import ai.konduit.serving.util.ObjectMappers;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nd4j.linalg.dataset.api.preprocessor.serializer.NormalizerType;
import org.nd4j.shade.jackson.annotation.JsonTypeInfo;

import static org.nd4j.shade.jackson.annotation.JsonTypeInfo.As.PROPERTY;
import static org.nd4j.shade.jackson.annotation.JsonTypeInfo.Id.NAME;

/**
 * Column distribution represents statistics and normalizer
 * information for how to transform or denormalize values
 * based on distribution information.
 *
 * @author Adam Gibson
 */
@Data
@Builder
@JsonTypeInfo(use = NAME, include = PROPERTY)
@AllArgsConstructor
@NoArgsConstructor
public class ColumnDistribution implements TextConfig {
    private double mean,min,max,standardDeviation;
    private NormalizerType normalizerType;

    public static ColumnDistribution fromJson(String json) {
        return ObjectMappers.fromJson(json, ColumnDistribution.class);
    }

    public static ColumnDistribution fromYaml(String yaml) {
        return ObjectMappers.fromYaml(yaml, ColumnDistribution.class);
    }

}
