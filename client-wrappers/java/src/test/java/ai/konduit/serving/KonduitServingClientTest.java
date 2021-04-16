package ai.konduit.serving;

import ai.konduit.serving.client.java.invoker.ApiException;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import ai.konduit.serving.pipeline.impl.step.logging.LoggingStep;
import ai.konduit.serving.vertx.api.DeployKonduitServing;
import ai.konduit.serving.vertx.config.InferenceConfiguration;
import ai.konduit.serving.vertx.config.InferenceDeploymentResult;
import ai.konduit.serving.vertx.config.ServerProtocol;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nd4j.common.io.ClassPathResource;
import org.slf4j.event.Level;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Unit test for simple KonduitServingClient.
 */
@RunWith(VertxUnitRunner.class)
public class KonduitServingClientTest
{
    static InferenceConfiguration configuration;
    static Vertx vertx;
    static InferenceDeploymentResult inferenceDeploymentResult;

    @BeforeClass
    public static void setUp(TestContext testContext) {
        configuration = new InferenceConfiguration()
                .protocol(ServerProtocol.HTTP)
                .pipeline(SequencePipeline.builder()
                        .add(new LoggingStep().log(LoggingStep.Log.KEYS_AND_VALUES).logLevel(Level.INFO))
                        .build());

        Async async = testContext.async();

        vertx = DeployKonduitServing.deploy(new VertxOptions(),
                new DeploymentOptions(),
                configuration,
                handler -> {
                    if(handler.succeeded()) {
                        inferenceDeploymentResult = handler.result();
                        async.complete();
                    } else {
                        testContext.fail(handler.cause());
                    }
                });
    }

    /**
     * Test predictions using images with konduit-serving
     */
    @Test
    public void testBasicImagePrediction(TestContext testContext) throws ApiException, IOException {
        Map<String, Object> input = new HashMap<>();
        input.put("image", KonduitServingClient.getImage(new ClassPathResource("images/test_input_number_0.png").getFile().getAbsolutePath()));
        input.put("b", 20.0);

        KonduitServingClient konduitServingClient = KonduitServingClient.builder()
                .useSsl(false)
                .host("localhost")
                .port(inferenceDeploymentResult.getActualPort())
                .build();

        int numberOfRequests = 10;
        for (int responseNumber = 1; responseNumber <= numberOfRequests; responseNumber++) {
            Map<String, Object> output = konduitServingClient.predict(input);
            System.out.format("Response %s/%s: %s%n",
                    responseNumber,
                    numberOfRequests,
                    output);

            assertEquals(input, output);
        }
    }

    /**
     * Test predictions using arrays with konduit-serving
     */
    @Test
    public void testBasicArrayPrediction(TestContext testContext) throws ApiException {
        Map<String, Object> input = new HashMap<>();
        input.put("array", new float[][][]{{{1.0f, 2.0f, 3.0f}, {4.0f, 5.0f, 6.0f}}, {{7.0f, 8.0f, 9.0f}, {10.0f, 11.0f, 12.0f}}});
        input.put("b", 20.0f);

        KonduitServingClient konduitServingClient = KonduitServingClient.builder()
                .useSsl(false)
                .host("localhost")
                .port(inferenceDeploymentResult.getActualPort())
                .build();

        int numberOfRequests = 10;
        for (int responseNumber = 1; responseNumber <= numberOfRequests; responseNumber++) {
            Map<String, Object> output = konduitServingClient.predict(input);
            System.out.format("Response %s/%s: %s%n",
                    responseNumber,
                    numberOfRequests,
                    output);

            assertEquals(Arrays.deepToString((Object[]) input.get("array")), output.get("array").toString());
        }
    }

    /**
     * Test predictions with konduit-serving
     */
    @Test
    public void testBasicPrediction(TestContext testContext) throws ApiException {
        Map<String, Object> input = new HashMap<>();
        input.put("a", 1.0);
        input.put("b", 20.0);

        KonduitServingClient konduitServingClient = KonduitServingClient.builder()
                .useSsl(false)
                .host("localhost")
                .port(inferenceDeploymentResult.getActualPort())
                .build();

        int numberOfRequests = 10;
        for (int responseNumber = 1; responseNumber <= numberOfRequests; responseNumber++) {
            Map<String, Object> output = konduitServingClient.predict(input);
            System.out.format("Response %s/%s: %s%n",
                    responseNumber,
                    numberOfRequests,
                    output);

            assertEquals(input, output);
        }
    }

    @AfterClass
    public static void tearDown(TestContext testContext) {
        vertx.close(testContext.asyncAssertSuccess());
    }
}
