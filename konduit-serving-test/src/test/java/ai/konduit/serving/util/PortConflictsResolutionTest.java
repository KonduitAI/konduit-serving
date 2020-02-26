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
import ai.konduit.serving.verticles.inference.InferenceVerticle;
import io.vertx.core.json.JsonObject;
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
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.primitives.Pair;

import java.io.File;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicInteger;

import ai.konduit.serving.util.PortUtils;

import static com.jayway.restassured.RestAssured.given;

@Slf4j
@RunWith(VertxUnitRunner.class)
public class PortConflictsResolutionTest {

    public static String CONFIG_FILE_PATH_KEY = "configFilePathKey";
    private static int firstSelectedPort;

    @Rule
    public Timeout rule = Timeout.seconds(240);

    @ClassRule
    public static TemporaryFolder folder = new TemporaryFolder();

    @BeforeClass
    public static void beforeClass(TestContext testContext) throws Exception {
        firstSelectedPort = PortUtils.getAvailablePort(); // Getting an available port so that the first server starts without issues
        log.info("First selected port is {}", firstSelectedPort);
        JsonObject config = getConfig(firstSelectedPort);

        File jsonConfigPath = folder.newFile("config.json");
        FileUtils.write(jsonConfigPath, config.encodePrettily(), Charset.defaultCharset());

        testContext.put(CONFIG_FILE_PATH_KEY, jsonConfigPath.getAbsolutePath());
    }

    @Test
    public void testPortConflictResolution(TestContext testContext) {
        Async async1 = testContext.async();
        Async async2 = testContext.async();

        AtomicInteger server1Port = new AtomicInteger(0);
        AtomicInteger server2Port = new AtomicInteger(0);

        KonduitServingMainArgs args = KonduitServingMainArgs.builder()
                .configStoreType("file").ha(false)
                .multiThreaded(false)
                .verticleClassName(InferenceVerticle.class.getName())
                .configPath(testContext.get(CONFIG_FILE_PATH_KEY))
                .build();

        KonduitServingMain konduitServingMain = KonduitServingMain.builder()
                .onSuccess(port -> {
                    if(server1Port.get() < 1) {
                        server1Port.set(port);
                        async1.complete();
                    } else {
                        server2Port.set(port);
                        async2.complete();
                    }
                })
                .onFailure(() -> testContext.fail("unable to start server."))
                .build();

        konduitServingMain.runMain(args.toArgs());
        async1.await();

        konduitServingMain.runMain(args.toArgs()); // Trying to start the same server on the same port
        async2.await();

        // The first server should be started on the given port
        testContext.assertEquals(server1Port.get(), firstSelectedPort);
        // Due to a conflict, the second server should start on a different port number than the first one
        testContext.assertNotEquals(server2Port.get(), firstSelectedPort);
        // The two running ports should also be different
        testContext.assertNotEquals(server1Port.get(), server2Port.get());

        // Health checking server 1
        given().port(server1Port.get())
                .get("/healthcheck")
                .then()
                .statusCode(204);
        // Health checking server 2
        given().port(server2Port.get())
                .get("/healthcheck")
                .then()
                .statusCode(204);
    }

    public static JsonObject getConfig(int port) throws Exception {
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
                .httpPort(port)
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
