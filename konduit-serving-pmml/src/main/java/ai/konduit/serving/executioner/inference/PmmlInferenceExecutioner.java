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

package ai.konduit.serving.executioner.inference;

import ai.konduit.serving.model.loader.ModelLoader;
import ai.konduit.serving.config.ParallelInferenceConfig;
import ai.konduit.serving.threadpool.pmml.PMMLThreadPool;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dmg.pmml.FieldName;
import org.jpmml.evaluator.Evaluator;

import java.util.List;
import java.util.Map;

/**
 * An {@link InferenceExecutioner}
 * for use with the {@link PMMLThreadPool}
 *
 * @author Adam Gibson
 */
@Slf4j
public class PmmlInferenceExecutioner implements
        InferenceExecutioner<ModelLoader<Evaluator>,List<Map<FieldName,Object>>,List<Map<FieldName,Object>>,
                ParallelInferenceConfig,Evaluator> {

    @Getter
    private PMMLThreadPool inference;
    @Getter
    private ModelLoader<Evaluator> modelLoader;


    @Override
    public ModelLoader<Evaluator> modelLoader() {
        return modelLoader;
    }

    @Override
    public Evaluator model() {
        try {
            return modelLoader.loadModel();
        } catch (Exception e) {
            log.error("Unable to load model in model() call for pmml inference executioner",e);
            return null;
        }
    }


    @Override
    public void initialize(ModelLoader<Evaluator> model, ParallelInferenceConfig config) {
        this.inference = new PMMLThreadPool.Builder(model)
                .batchLimit(config.getBatchLimit())
                .queueLimit(config.getQueueLimit())
                .inferenceMode(config.getInferenceMode())
                .workers(config.getWorkers())
                .build();
        this.modelLoader = model;


    }

    @Override
    public List<Map<FieldName, Object>> execute(List<Map<FieldName, Object>> input) {
        if(inference == null) {
            throw new IllegalStateException("Initialize not called. No ParallelInference found. Please call " +
                    "inferenceExecutioner.initialize(..)");
        }
        return inference.output(input);
    }

    @Override
    public void stop() {
        if(inference != null) {
            inference.shutdown();
        }
    }
}
