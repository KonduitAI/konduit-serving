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
import org.slf4j.event.Level;

import java.util.HashMap;
import java.util.Map;

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
                        .add(new LoggingStep().log(LoggingStep.Log.KEYS_AND_VALUES).logLevel(Level.ERROR))
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

        Map<String, Object> output = konduitServingClient.predict(input);
        System.out.println(output);

        assertEquals(input, output);
    }

    @AfterClass
    public static void tearDown(TestContext testContext) {
        vertx.close(testContext.asyncAssertSuccess());
    }
}
