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

package ai.konduit.serving.model;

import ai.konduit.serving.util.ObjectMappers;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;


/**
 * Configuration for models in PMML format.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@AllArgsConstructor
public class PmmlConfig extends ModelConfig {

    public final static String DEFAULT_EVALUATOR_FACTORY = "org.jpmml.evaluator.ModelEvaluatorFactory";
    @Builder.Default
    private String evaluatorFactoryName = DEFAULT_EVALUATOR_FACTORY;

    public PmmlConfig(){
        this.evaluatorFactoryName = DEFAULT_EVALUATOR_FACTORY;
    }

    /**
     * Default PMML config
     *
     * @return default
     */
    public static PmmlConfig defaultConfig() {
        return PmmlConfig.builder()
                .evaluatorFactoryName(DEFAULT_EVALUATOR_FACTORY)
                .build();

    }

    public String evaluatorFactoryName() {
        return evaluatorFactoryName == null ? DEFAULT_EVALUATOR_FACTORY : evaluatorFactoryName;
    }

    public static PmmlConfig fromJson(String json){
        return ObjectMappers.fromJson(json, PmmlConfig.class);
    }

    public static PmmlConfig fromYaml(String yaml){
        return ObjectMappers.fromYaml(yaml, PmmlConfig.class);
    }
}
