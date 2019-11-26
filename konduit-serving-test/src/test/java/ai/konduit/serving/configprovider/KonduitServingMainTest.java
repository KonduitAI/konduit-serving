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

package ai.konduit.serving.configprovider;

import ai.konduit.serving.InferenceConfiguration;
import ai.konduit.serving.model.ModelConfig;
import ai.konduit.serving.model.ModelConfigType;
import ai.konduit.serving.pipeline.step.ModelStep;
import ai.konduit.serving.config.Input;
import ai.konduit.serving.config.Output;
import ai.konduit.serving.config.ServingConfig;
import ai.konduit.serving.train.TrainUtils;
import ai.konduit.serving.util.SchemaTypeUtils;
import ai.konduit.serving.verticles.inference.InferenceVerticle;
import io.vertx.core.json.JsonObject;
import org.apache.commons.io.FileUtils;
import org.datavec.api.transform.schema.Schema;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.junit.Test;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.primitives.Pair;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.Charset;

public class KonduitServingMainTest {


    @Test
    public void testFile() throws Exception {
        KonduitServingMain konduitServingMain = new KonduitServingMain();
        JsonObject config = getConfig();
        File jsonConfigPath = new File(System.getProperty("java.io.tmpdir"),"config.json");
        FileUtils.write(jsonConfigPath,config.encodePrettily(), Charset.defaultCharset());
        int port = getAvailablePort();

        KonduitServingMainArgs args = KonduitServingMainArgs.builder()
                .configStoreType("file").ha(false)
                .multiThreaded(false).configPort(port)
                .verticleClassName(InferenceVerticle.class.getName())
                .configPath(jsonConfigPath.getAbsolutePath())
                .build();
        konduitServingMain.runMain(args.toArgs());

        Thread.sleep(10000);
    }


    public JsonObject getConfig() throws Exception {
        Pair<MultiLayerNetwork, DataNormalization> multiLayerNetwork = TrainUtils.getTrainedNetwork();
        File modelSave =  new File(System.getProperty("java.io.tmpdir"),"model.zip");
        ModelSerializer.writeModel(multiLayerNetwork.getFirst(), modelSave,false);


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

        ServingConfig servingConfig = ServingConfig.builder()
                .inputDataType(Input.DataType.JSON)
                .httpPort(getAvailablePort())
                .predictionType(Output.PredictionType.CLASSIFICATION)
                .build();

        
        
        ModelConfig modelConfig = ModelConfig.builder()
                .modelConfigType(
                        ModelConfigType.builder().modelLoadingPath(modelSave.getAbsolutePath())
                                .modelType(ModelConfig.ModelType.MULTI_LAYER_NETWORK)
                                .build()
                ).build();

        ModelStep modelPipelineStep = ModelStep.builder()
                .inputName("default")
                .inputColumnName("default", SchemaTypeUtils.columnNames(inputSchema))
                .inputSchema("default", SchemaTypeUtils.typesForSchema(inputSchema))
                .outputSchema("default",SchemaTypeUtils.typesForSchema(outputSchema))
                .modelConfig(modelConfig)
                .outputColumnName("default", SchemaTypeUtils.columnNames(outputSchema))
                .build();
        
        
        InferenceConfiguration inferenceConfiguration = InferenceConfiguration.builder()
                .servingConfig(servingConfig)
                .step(modelPipelineStep)
                .build();


        return new JsonObject(inferenceConfiguration.toJson());
    }


    /**
     * @return single available port number
     */
    public static int getAvailablePort() {
        try {
            ServerSocket socket = new ServerSocket(0);
            try {
                return socket.getLocalPort();
            } finally {
                socket.close();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot find available port: " + e.getMessage(), e);
        }
    }
}
