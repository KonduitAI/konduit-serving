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
import ai.konduit.serving.model.ModelConfig;
import ai.konduit.serving.model.ModelConfigType;
import ai.konduit.serving.output.types.ClassifierOutput;
import ai.konduit.serving.pipeline.ModelPipelineStep;
import ai.konduit.serving.config.Output;
import ai.konduit.serving.config.PubsubConfig;
import ai.konduit.serving.config.ServingConfig;
import ai.konduit.serving.util.ObjectMapperHolder;
import ai.konduit.serving.util.SchemaTypeUtils;
import ai.konduit.serving.verticles.inference.InferenceVerticle;
import com.jayway.restassured.http.ContentType;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.datavec.api.transform.schema.Schema;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.primitives.Pair;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.File;
import java.io.IOException;

import static ai.konduit.serving.train.TrainUtils.getTrainedNetwork;
import static com.jayway.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;

@RunWith(VertxUnitRunner.class)
@NotThreadSafe
@Ignore
public class ColumnarVerticlePubsubTest extends BaseDl4JVerticalTest {
    
    
    @Override
    public Class<? extends AbstractVerticle> getVerticalClazz() {
        return InferenceVerticle.class;
    }
    
    @Override
    public Handler<HttpServerRequest> getRequest() {
        
        return req -> {
            //should be json body of classification
            req.bodyHandler(body -> {
                try {
                    ClassifierOutput classifierOutput = ObjectMapperHolder.getJsonMapper().readValue(body.toString(), ClassifierOutput.class);
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
        File modelSave =  new File(temporary.getRoot(),"model.zip");
        ModelSerializer.writeModel(multiLayerNetwork.getFirst(),modelSave,true);
        Schema.Builder schemaBuilder = new Schema.Builder();
        schemaBuilder.addColumnDouble("petal_length")
                .addColumnDouble("petal_width")
                .addColumnDouble("sepal_width")
                .addColumnDouble("sepal_height");
        
        Schema schema = schemaBuilder.build();
        
        Schema.Builder outputSchemaBuilder = new Schema.Builder();
        outputSchemaBuilder.addColumnDouble("type");
        
        Schema outputSchema = outputSchemaBuilder.build();
        
        Nd4j.getRandom().setSeed(42);
        
        PubsubConfig pubsubConfig = PubsubConfig.builder()
                .httpMethod("POST")
                .pubsubUrl("http://localhost:" + pubsubPort)
                .contentType("application/json")
                .build();
        
        ServingConfig servingConfig = ServingConfig.builder()
                .predictionType(Output.PredictionType.CLASSIFICATION)
                .pubSubConfig(pubsubConfig)
                .httpPort(port).build();
        
        
        
        ModelConfig modelConfig = ModelConfig.builder()
                .modelConfigType(ModelConfigType.multiLayerNetwork(modelSave.getAbsolutePath()))
                .build();
        
        ModelPipelineStep pipelineStepConfig = ModelPipelineStep.builder()
                .inputSchema("default", SchemaTypeUtils.typesForSchema(schema))
                .outputSchema("default",SchemaTypeUtils.typesForSchema(outputSchema))
                .modelConfig(modelConfig)
                .build();
        
        InferenceConfiguration inferenceConfiguration = InferenceConfiguration.builder()
                .servingConfig(servingConfig)
                .pipelineStep(pipelineStepConfig)
                .build();
        
        return new JsonObject(inferenceConfiguration.toJson());
    }
    
    @Test
    public void testInferenceResult(TestContext context) {
        this.context = context;
        JsonArray jsonArray = new JsonArray();
        double[] vals = {5.1,3.5,1.4,0.2};
        for(int i = 0; i < 4; i++)  {
            jsonArray.add(vals[i]);
        }
        
        JsonArray wrapper = new JsonArray();
        wrapper.add(jsonArray);
        
        given().contentType(ContentType.JSON)
                .body(wrapper.toString())
                .port(port)
                .post("/classification/csvpubsub");
        context.async().complete();
        
        
    }
    
    @Override
    public boolean isPubSub() {
        return true;
    }
}