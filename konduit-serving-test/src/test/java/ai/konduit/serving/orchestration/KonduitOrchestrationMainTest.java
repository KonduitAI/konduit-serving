package ai.konduit.serving.orchestration;

import ai.konduit.serving.InferenceConfiguration;
import ai.konduit.serving.config.ServingConfig;
import com.jayway.restassured.response.Response;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import static com.jayway.restassured.RestAssured.given;

@RunWith(VertxUnitRunner.class)
public class KonduitOrchestrationMainTest {

    @Rule
    public TemporaryFolder testDir = new TemporaryFolder();

    @Test
    public void testClusterRun(TestContext testContext) throws Exception {
        Async async = testContext.async();

        //HazelcastInstance hzInstance = Hazelcast.newHazelcastInstance();
        //Thread.sleep(10000);

        int port = getRandomPort();

        ServingConfig servingConfig = ServingConfig.builder()
                .httpPort(port)
                .build();
        InferenceConfiguration inferenceConfiguration = InferenceConfiguration.builder()
                .servingConfig(servingConfig)
                .build();
        /*
         * Need to work out what a "node" is: eg, what happens when you ai.konduit.serving.deploy 2 instances on vertx?
         * What happens when you have 2 separate verticles?
         *
         * What should "nodes" return?
         */

        DeployKonduitOrchestration.deployInferenceClustered(inferenceConfiguration,
                handler -> {
                    if(handler.succeeded()) {
                        try {
                            Response response = given().port(port)
                                    .get("/nodes")
                                    .then()
                                    .statusCode(200).and()
                                    .contentType("application/json")
                                    .extract().response();
                            System.out.println(response.body().prettyPrint());

                            async.complete();
                        } catch (Exception e) {
                            testContext.fail("Orchestration main server failed to start." + e);
                        }
                    } else {
                        testContext.fail("Orchestration main server failed to start.");
                    }
                });
    }

    public int getRandomPort() throws java.io.IOException {
        java.net.ServerSocket pubSubSocket = new java.net.ServerSocket(0);
        int ret = pubSubSocket.getLocalPort();
        pubSubSocket.close();
        return ret;
    }
}
