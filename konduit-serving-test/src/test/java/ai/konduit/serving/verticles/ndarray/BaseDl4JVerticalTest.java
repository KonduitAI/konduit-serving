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

package ai.konduit.serving.verticles.ndarray;

import ai.konduit.serving.InferenceConfiguration;
import ai.konduit.serving.config.ServingConfig;
import ai.konduit.serving.model.DL4JConfig;
import ai.konduit.serving.model.ModelConfig;
import ai.konduit.serving.model.ModelConfigType;
import ai.konduit.serving.output.types.ClassifierOutput;
import ai.konduit.serving.pipeline.PipelineStep;
import ai.konduit.serving.pipeline.step.ModelStep;
import ai.konduit.serving.util.ObjectMappers;
import ai.konduit.serving.verticles.BaseVerticleTest;
import ai.konduit.serving.verticles.inference.InferenceVerticle;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.datavec.api.transform.schema.Schema;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.primitives.Pair;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.File;
import java.io.IOException;

import static ai.konduit.serving.train.TrainUtils.getTrainedNetwork;
import static org.junit.Assert.assertEquals;

@NotThreadSafe
public abstract class BaseDl4JVerticalTest extends BaseVerticleTest {

    @Override
    public Handler<HttpServerRequest> getRequest() {

        return req -> {
            req.bodyHandler(body -> {
                try {
                    ClassifierOutput classifierOutput = ObjectMappers.json().readValue(body.toString(),
                            ClassifierOutput.class);
                    assertEquals(1, classifierOutput.getDecisions()[0]);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("Finish body" + body);
            });
            req.exceptionHandler(Throwable::printStackTrace);
        };
    }


    @Override
    public JsonObject getConfigObject() throws Exception {
        Pair<MultiLayerNetwork, DataNormalization> multiLayerNetwork = getTrainedNetwork();
        File modelSave = new File(temporary.getRoot(), "model.zip");
        ModelSerializer.writeModel(multiLayerNetwork.getFirst(), modelSave, true);

        Schema.Builder schemaBuilder = new Schema.Builder();
        schemaBuilder.addColumnDouble("petal_length")
                .addColumnDouble("petal_width")
                .addColumnDouble("sepal_width")
                .addColumnDouble("sepal_height");
        Schema inputSchema = schemaBuilder.build();

        Schema.Builder outputSchemaBuilder = new Schema.Builder();
        outputSchemaBuilder.addColumnDouble("setosa");
        outputSchemaBuilder.addColumnDouble("versicolor");
        outputSchemaBuilder.addColumnDouble("virginica");
        Schema outputSchema = outputSchemaBuilder.build();


        Nd4j.getRandom().setSeed(42);

        ModelConfig modelConfig = DL4JConfig.builder()
                .modelConfigType(ModelConfigType.multiLayerNetwork(modelSave.getAbsolutePath()))
                .build();

        ServingConfig servingConfig = ServingConfig.builder()
                .httpPort(port)
                .build();

        PipelineStep modelPipelineStep = new ModelStep(modelConfig)
                .setInput(inputSchema).setOutput(outputSchema);

        InferenceConfiguration inferenceConfiguration = InferenceConfiguration.builder()
                .servingConfig(servingConfig)
                .step(modelPipelineStep)
                .build();
        return new JsonObject(inferenceConfiguration.toJson());
    }


    @Override
    public Class<? extends AbstractVerticle> getVerticalClazz() {
        return InferenceVerticle.class;
    }


}
