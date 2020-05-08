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

package ai.konduit.serving.routers;

import ai.konduit.serving.InferenceConfiguration;
import ai.konduit.serving.config.ServingConfig;
import ai.konduit.serving.deploy.DeployKonduitServing;
import ai.konduit.serving.pipeline.step.ModelStep;
import ai.konduit.serving.pipeline.step.model.Dl4jStep;
import ai.konduit.serving.train.TrainUtils;
import ai.konduit.serving.util.PortUtils;
import ai.konduit.serving.util.SchemaTypeUtils;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.datavec.api.transform.schema.Schema;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.nd4j.common.primitives.Pair;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;

import java.io.File;

@RunWith(VertxUnitRunner.class)
public class KonduitServingMainTest {

    @Rule
    public Timeout rule = Timeout.seconds(100000);

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testSuccess(TestContext testContext) throws Exception {
        Async async = testContext.async();

        DeployKonduitServing.deployInference(getConfig(false),
                handler -> {
                    if (handler.succeeded()) {
                        async.complete();
                    } else {
                        testContext.fail("Failure event called instead of a success event");
                    }
                });
    }

    @Test
    public void testFailure(TestContext testContext) throws Exception {
        Async async = testContext.async();

        DeployKonduitServing.deployInference(getConfig(true),
                handler -> {
                    if(handler.succeeded()) {
                        testContext.fail("Success event called instead of a failure event");
                    } else {
                        testContext.assertTrue(handler.cause() instanceof IllegalStateException);
                        async.complete();
                    }
                });
    }

    /**
     * Returns an inference configuration
     * @param fail If true, the network file won't be trained and saved on the file path.
     *             This is useful for deliberately failing the deployment for {@link KonduitServingMainTest#testFailure(TestContext)}
     * @return An {@link InferenceConfiguration} object.
     * @throws Exception throws if there's an exception training a test network.
     */
    public InferenceConfiguration getConfig(boolean fail) throws Exception {
        File modelSave = folder.newFile("model.zip");

        if(!fail) {
            Pair<MultiLayerNetwork, DataNormalization> multiLayerNetwork = TrainUtils.getTrainedNetwork();
            ModelSerializer.writeModel(multiLayerNetwork.getFirst(), modelSave, false);
        }

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
                .httpPort(PortUtils.getAvailablePort())
                .build();

        Dl4jStep modelPipelineStep = Dl4jStep.builder()
                .inputName("default")
                .inputColumnName("default", SchemaTypeUtils.columnNames(inputSchema))
                .inputSchema("default", SchemaTypeUtils.typesForSchema(inputSchema))
                .outputSchema("default", SchemaTypeUtils.typesForSchema(outputSchema))
                .path(modelSave.getAbsolutePath())
                .outputColumnName("default", SchemaTypeUtils.columnNames(outputSchema))
                .build();

        return InferenceConfiguration.builder()
                .servingConfig(servingConfig)
                .step(modelPipelineStep)
                .build();
    }
}
