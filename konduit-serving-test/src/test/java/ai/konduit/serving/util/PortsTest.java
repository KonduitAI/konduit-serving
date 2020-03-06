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
import ai.konduit.serving.configprovider.KonduitServingMain;
import ai.konduit.serving.configprovider.KonduitServingMainArgs;
import ai.konduit.serving.model.DL4JConfig;
import ai.konduit.serving.model.ModelConfig;
import ai.konduit.serving.model.ModelConfigType;
import ai.konduit.serving.pipeline.step.ModelStep;
import ai.konduit.serving.train.TrainUtils;
import ai.konduit.serving.verticles.VerticleConstants;
import ai.konduit.serving.verticles.inference.InferenceVerticle;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
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
import java.io.IOException;
import java.net.BindException;
import java.nio.charset.Charset;

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
    public void testRandomPort(TestContext testContext) throws IOException {
        Async async = testContext.async();

        KonduitServingMainArgs args = KonduitServingMainArgs.builder()
                .configStoreType("file").ha(false)
                .multiThreaded(false)
                .verticleClassName(InferenceVerticle.class.getName())
                .configPath(getConfig(testContext, 0))
                .build();

        KonduitServingMain.builder()
                .eventHandler(handler -> {
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
                })
                .build()
                .runMain(args.toArgs());
    }

    @Test
    public void testConflictedPort(TestContext testContext) throws IOException {
        Async async = testContext.async(2);

        KonduitServingMainArgs args = KonduitServingMainArgs.builder()
                .configStoreType("file").ha(false)
                .multiThreaded(false)
                .verticleClassName(InferenceVerticle.class.getName())
                .configPath(getConfig(testContext, PortUtils.getAvailablePort()))
                .build();

        KonduitServingMain konduitServingMain = KonduitServingMain.builder().build();
        konduitServingMain.setEventHandler(handler -> {
            if (handler.succeeded()) {
                int port = handler.result().getServingConfig().getHttpPort();

                testContext.assertTrue(port > 0 && port <= 0xFFFF);

                // Health checking
                given().port(port)
                        .get("/healthcheck")
                        .then()
                        .statusCode(204);

                konduitServingMain.runMain(args.toArgs());
                async.countDown();
            } else {
                Throwable throwable = handler.cause();
                if(throwable instanceof BindException &&
                        throwable.getMessage().contains("Address already in use")) {
                    async.countDown();
                } else {
                    testContext.fail(throwable);
                }
            }
        });

        konduitServingMain.runMain(args.toArgs());
    }

    @Test
    public void testPortFromEnvironment(TestContext testContext) throws IOException {
        int selectedPort = PortUtils.getAvailablePort();  // Making sure it's an available port

        environmentVariables.set(VerticleConstants.KONDUIT_SERVING_PORT, String.valueOf(selectedPort));

        Async async = testContext.async();

        KonduitServingMainArgs args = KonduitServingMainArgs.builder()
                .configStoreType("file").ha(false)
                .multiThreaded(false)
                .verticleClassName(InferenceVerticle.class.getName())
                .configPath(getConfig(testContext, -1))  // this port number will be ignored in this case because the port value will be taken from the environment VerticleConstants#KONDUIT_SERVING_PORT
                .build();

        KonduitServingMain.builder()
                .eventHandler(handler -> {
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
                })
                .build()
                .runMain(args.toArgs());
    }

    @Test
    public void testPortFromBadEnvironment(TestContext testContext) throws IOException {
        Object[] invalidPorts = new Object[] { "stringValue", -400, 100000};

        Async[] asyncs = new Async[invalidPorts.length];

        for (int i = 0; i < invalidPorts.length; i++) {
            asyncs[i] = testContext.async();

            int finalI = i;
            environmentVariables.set(VerticleConstants.KONDUIT_SERVING_PORT, String.valueOf(invalidPorts[finalI]));

            KonduitServingMainArgs args = KonduitServingMainArgs.builder()
                    .configStoreType("file").ha(false)
                    .multiThreaded(false)
                    .verticleClassName(InferenceVerticle.class.getName())
                    .configPath(getConfig(testContext, -1)) // this port number will be ignored in this case because the port value will be taken from the environment VerticleConstants#KONDUIT_SERVING_PORT
                    .build();

            KonduitServingMain.builder()
                    .eventHandler(handler -> {
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
                    })
                    .build()
                    .runMain(args.toArgs());

            asyncs[i].await();
        }
    }

    @Test
    public void testSpecifiedPortWithoutEnvironment(TestContext testContext) throws Exception {
        int selectedPort = PortUtils.getAvailablePort();  // Making sure it's an available port

        Async async = testContext.async();

        KonduitServingMainArgs args = KonduitServingMainArgs.builder()
                .configStoreType("file").ha(false)
                .multiThreaded(false)
                .verticleClassName(InferenceVerticle.class.getName())
                .configPath(getConfig(testContext, selectedPort))
                .build();

        KonduitServingMain.builder()
                .eventHandler(handler -> {
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
                })
                .build()
                .runMain(args.toArgs());
    }

    private static String trainAndSaveModel() throws Exception {
        Pair<MultiLayerNetwork, DataNormalization> multiLayerNetwork = TrainUtils.getTrainedNetwork();
        File modelSave = folder.newFile("model.zip");
        ModelSerializer.writeModel(multiLayerNetwork.getFirst(), modelSave, false);

        return modelSave.getAbsolutePath();
    }

    public static String getConfig(TestContext testContext, int port) throws IOException {
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

        ModelConfig modelConfig = DL4JConfig.builder()
                .modelConfigType(
                        ModelConfigType.builder().modelLoadingPath(testContext.get(SAVED_MODEL_PATH))
                                .modelType(ModelConfig.ModelType.DL4J)
                                .build()
                ).build();

        ModelStep modelPipelineStep = ModelStep.builder()
                .inputName("default")
                .inputColumnName("default", SchemaTypeUtils.columnNames(inputSchema))
                .inputSchema("default", SchemaTypeUtils.typesForSchema(inputSchema))
                .outputSchema("default", SchemaTypeUtils.typesForSchema(outputSchema))
                .modelConfig(modelConfig)
                .outputColumnName("default", SchemaTypeUtils.columnNames(outputSchema))
                .build();

        InferenceConfiguration inferenceConfiguration = InferenceConfiguration.builder()
                .servingConfig(servingConfig)
                .step(modelPipelineStep)
                .build();

        File configSavePath = folder.newFile();
        FileUtils.writeStringToFile(configSavePath, inferenceConfiguration.toJson(), Charset.defaultCharset());
        return configSavePath.getAbsolutePath();
    }
}
