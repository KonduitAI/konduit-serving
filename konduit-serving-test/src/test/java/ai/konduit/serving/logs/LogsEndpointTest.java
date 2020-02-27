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

package ai.konduit.serving.logs;

import ai.konduit.serving.InferenceConfiguration;
import ai.konduit.serving.config.ServingConfig;
import ai.konduit.serving.configprovider.KonduitServingMain;
import ai.konduit.serving.configprovider.KonduitServingMainArgs;
import ai.konduit.serving.model.DL4JConfig;
import ai.konduit.serving.model.ModelConfig;
import ai.konduit.serving.model.ModelConfigType;
import ai.konduit.serving.pipeline.step.ModelStep;
import ai.konduit.serving.train.TrainUtils;
import ai.konduit.serving.util.SchemaTypeUtils;
import ai.konduit.serving.verticles.inference.InferenceVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.commons.io.FileUtils;
import org.datavec.api.transform.schema.Schema;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.primitives.Pair;
import org.omg.CORBA.Environment;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.Charset;
import java.util.UUID;

import static com.jayway.restassured.RestAssured.given;

@RunWith(VertxUnitRunner.class)
public class LogsEndpointTest {

    public static String CONFIG_FILE_PATH_KEY = "configFilePathKey";
    public static String SELECTED_PORT_KEY = "availablePortKey";

    @Rule
    public Timeout rule = Timeout.seconds(240);

    @ClassRule
    public static TemporaryFolder folder = new TemporaryFolder();

    @BeforeClass
    public static void beforeClass(TestContext testContext) throws Exception {
        testContext.put(SELECTED_PORT_KEY, getAvailablePort());

        File jsonConfigPath = folder.newFile("config.json");
        testContext.put(CONFIG_FILE_PATH_KEY, jsonConfigPath.getAbsolutePath());

        JsonObject config = getConfig(testContext);
        FileUtils.write(jsonConfigPath, config.encodePrettily(), Charset.defaultCharset());
    }

    /**
     * @return single available port number
     */
    public static int getAvailablePort() {
        try {
            try (ServerSocket socket = new ServerSocket(0)) {
                return socket.getLocalPort();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot find available port: " + e.getMessage(), e);
        }
    }


    @Test
    public void checkLogs(TestContext testContext) {
        Async async = testContext.async();
        KonduitServingMainArgs args = KonduitServingMainArgs.builder()
                .configStoreType("file").ha(false)
                .multiThreaded(false)
                .verticleClassName(InferenceVerticle.class.getName())
                .configPath(testContext.get(CONFIG_FILE_PATH_KEY))
                .build();

        int numberOfLinesToReadFromLogs = 10;

        Matcher<String> linesCalculatingMatcher = new BaseMatcher<String>() {

            @Override
            public boolean matches(Object logs) {
                return ((String) logs).split(System.lineSeparator()).length == numberOfLinesToReadFromLogs;
            }

            @Override
            public void describeTo(Description description) { }

            @Override
            public void describeMismatch(Object logs, Description description) {
                description.appendText("Expected number of lines were ")
                        .appendValue(numberOfLinesToReadFromLogs)
                        .appendText("was ")
                        .appendValue(((String) logs).split(System.lineSeparator()).length)
                        .appendText("logs were ")
                        .appendValue(logs);
            }
        };

        KonduitServingMain.builder()
                .onSuccess(() -> {
                    given().port(testContext.get(SELECTED_PORT_KEY))
                            .get(String.format("/logs/%s", numberOfLinesToReadFromLogs))
                            .then()
                            .assertThat()
                            .statusCode(200)
                            .and()
                            .assertThat()
                            .body(Matchers.not(Matchers.isEmptyOrNullString()),
                                    linesCalculatingMatcher); // This would mean that the log file has been found and it was read successfully

                    async.complete();
                })
                .onFailure(() -> testContext.fail("onFailure called instead of onSuccess hook"))
                .build()
                .runMain(args.toArgs());
    }

    public static JsonObject getConfig(TestContext testContext) throws Exception {
        Pair<MultiLayerNetwork, DataNormalization> multiLayerNetwork = TrainUtils.getTrainedNetwork();
        File modelSave = folder.newFile("model.zip");
        ModelSerializer.writeModel(multiLayerNetwork.getFirst(), modelSave, false);

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
                .httpPort(testContext.get(SELECTED_PORT_KEY))
                .createLoggingEndpoints(true)
                .build();

        ModelConfig modelConfig = DL4JConfig.builder()
                .modelConfigType(
                        ModelConfigType.builder().modelLoadingPath(modelSave.getAbsolutePath())
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

        return new JsonObject(inferenceConfiguration.toJson());
    }
}
