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

package ai.konduit.serving.util;

import ai.konduit.serving.InferenceConfiguration;
import ai.konduit.serving.config.ServingConfig;
import ai.konduit.serving.deploy.DeployKonduitServing;
import ai.konduit.serving.pipeline.step.ModelStep;
import ai.konduit.serving.pipeline.step.model.Dl4jStep;
import ai.konduit.serving.train.TrainUtils;
import ai.konduit.serving.verticles.VerticleConstants;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import lombok.extern.slf4j.Slf4j;
import org.datavec.api.transform.schema.Schema;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.primitives.Pair;

import java.io.File;
import java.net.BindException;

import static com.jayway.restassured.RestAssured.given;

@Slf4j
@RunWith(VertxUnitRunner.class)
public class PortsTest {

    public static String SAVED_MODEL_PATH = "savedModelPathKey";

    @Rule
    public EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Rule
    public Timeout rule = Timeout.seconds(240);

    @ClassRule
    public static TemporaryFolder folder = new TemporaryFolder();

    @BeforeClass
    public static void beforeClass(TestContext testContext) throws Exception {
        testContext.put(SAVED_MODEL_PATH, trainAndSaveModel());
    }

    @Test
    public void testRandomPort(TestContext testContext) {
        Async async = testContext.async();

        DeployKonduitServing.deployInference(getConfig(testContext, 0),
                handler -> {
                    if(handler.succeeded()) {
                        int port = handler.result().getServingConfig().getHttpPort();

                        testContext.assertTrue(port > 0 && port <= 0xFFFF);

                        // Health checking
                        given().port(port)
                                .get("/healthcheck")
                                .then()
                                .statusCode(204);

                        async.complete();
                    } else {
                        testContext.fail(handler.cause());
                    }
                });
    }

    @Test
    public void testConflictedPort(TestContext testContext) {
        Async async = testContext.async(2);

        InferenceConfiguration inferenceConfiguration = getConfig(testContext, PortUtils.getAvailablePort());

        DeployKonduitServing.deployInference(inferenceConfiguration, outerHandler -> {
            if (outerHandler.succeeded()) {
                int port = outerHandler.result().getServingConfig().getHttpPort();

                testContext.assertTrue(port > 0 && port <= 0xFFFF);

                // Health checking
                given().port(port)
                        .get("/healthcheck")
                        .then()
                        .statusCode(204);

                DeployKonduitServing.deployInference(inferenceConfiguration, innerHandler -> {
                    if (innerHandler.failed()) {
                        Throwable throwable = innerHandler.cause();

                        if (throwable instanceof BindException &&
                                throwable.getMessage().contains("Address already in use")) {
                            async.countDown();
                        } else {
                            testContext.fail(throwable);
                        }
                    } else {
                        testContext.fail("The second deployment should fail here due to conflicted port.");
                    }
                });

                async.countDown();
            } else {
                testContext.fail(outerHandler.cause());
            }
        });
    }

    @Test
    public void testPortFromEnvironment(TestContext testContext) {
        int selectedPort = PortUtils.getAvailablePort();  // Making sure it's an available port
        environmentVariables.set(VerticleConstants.KONDUIT_SERVING_PORT, String.valueOf(selectedPort));

        Async async = testContext.async();

        DeployKonduitServing.deployInference(
                getConfig(testContext, -1), // this port number will be ignored in this case because the port value will be taken from the environment VerticleConstants#KONDUIT_SERVING_PORT
                handler -> {
                    if(handler.succeeded()) {
                        int port = handler.result().getServingConfig().getHttpPort();

                        testContext.assertEquals(port, selectedPort);

                        // Health checking
                        given().port(selectedPort)
                                .get("/healthcheck")
                                .then()
                                .statusCode(204);

                        async.complete();
                    } else {
                        testContext.fail(handler.cause());
                    }
                });
    }

    @Test
    public void testPortFromBadEnvironment(TestContext testContext) {
        Object[] invalidPorts = new Object[] { "stringValue", -400, 100000};

        Async[] asyncs = new Async[invalidPorts.length];

        for (int i = 0; i < invalidPorts.length; i++) {
            asyncs[i] = testContext.async();

            int finalI = i;
            environmentVariables.set(VerticleConstants.KONDUIT_SERVING_PORT, String.valueOf(invalidPorts[finalI]));

            DeployKonduitServing.deployInference(
                    getConfig(testContext, -1), // this port number will be ignored in this case because the port value will be taken from the environment VerticleConstants#KONDUIT_SERVING_PORT)
                    handler -> {
                        if(handler.succeeded()) {
                            testContext.fail(String.format("Success event called with port value: %s", invalidPorts[finalI]));
                        } else {
                            Throwable throwable = handler.cause();

                            String throwableMessage = throwable.getMessage();
                            if(throwableMessage.contains(String.format("Valid port range is 0 <= port <= 65535. The given port was %s", invalidPorts[finalI])) ||
                                    throwableMessage.contains(String.format("For input string: \"%s\"", invalidPorts[finalI]))) {
                                asyncs[finalI].complete();
                            } else {
                                testContext.fail(throwable);
                            }
                        }
                    });

            asyncs[i].await();
        }
    }

    @Test
    public void testSpecifiedPortWithoutEnvironment(TestContext testContext) {
        int selectedPort = PortUtils.getAvailablePort();  // Making sure it's an available port

        Async async = testContext.async();

        DeployKonduitServing.deployInference(getConfig(testContext, selectedPort),
                handler -> {
                    if(handler.succeeded()) {
                        int port = handler.result().getServingConfig().getHttpPort();

                        testContext.assertEquals(port, selectedPort);

                        // Health checking
                        given().port(selectedPort)
                                .get("/healthcheck")
                                .then()
                                .statusCode(204);

                        async.complete();
                    } else {
                        testContext.fail(handler.cause());
                    }
                });
    }

    private static String trainAndSaveModel() throws Exception {
        Pair<MultiLayerNetwork, DataNormalization> multiLayerNetwork = TrainUtils.getTrainedNetwork();
        File modelSave = folder.newFile("model.zip");
        ModelSerializer.writeModel(multiLayerNetwork.getFirst(), modelSave, false);

        return modelSave.getAbsolutePath();
    }

    public static InferenceConfiguration getConfig(TestContext testContext, int port) {
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
                .httpPort(port)
                .build();

        Dl4jStep modelPipelineStep = Dl4jStep.builder()
                .inputName("default")
                .inputColumnName("default", SchemaTypeUtils.columnNames(inputSchema))
                .inputSchema("default", SchemaTypeUtils.typesForSchema(inputSchema))
                .outputSchema("default", SchemaTypeUtils.typesForSchema(outputSchema))
                .path(testContext.get(SAVED_MODEL_PATH))
                .outputColumnName("default", SchemaTypeUtils.columnNames(outputSchema))
                .build();

        return InferenceConfiguration.builder()
                .servingConfig(servingConfig)
                .step(modelPipelineStep)
                .build();
    }
}
