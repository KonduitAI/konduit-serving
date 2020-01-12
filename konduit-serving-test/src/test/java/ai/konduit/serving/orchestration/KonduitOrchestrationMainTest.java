package ai.konduit.serving.orchestration;

import ai.konduit.serving.InferenceConfiguration;
import ai.konduit.serving.config.ServingConfig;
import ai.konduit.serving.configprovider.KonduitServingNodeConfigurer;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.jayway.restassured.response.Response;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.nio.charset.Charset;

import static com.jayway.restassured.RestAssured.given;

@RunWith(VertxUnitRunner.class)
public class KonduitOrchestrationMainTest {

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
        File tmpFile = new File("file.json");
        FileUtils.writeStringToFile(tmpFile, inferenceConfiguration.toJson(), Charset.defaultCharset());
        tmpFile.deleteOnExit();
        /*
         * Need to work out what a "node" is: eg, what happens when you deploy 2 instances on vertx?
         * What happens when you have 2 separate verticles?
         *
         * What should "nodes" return?
         */

        KonduitServingNodeConfigurer configurer = KonduitServingNodeConfigurer.builder()
                .configPath(tmpFile.getAbsolutePath())
                .build();
        KonduitOrchestrationMain.builder()
                .onSuccess(() -> {
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
                })
                .onFailure(() -> {
                    testContext.fail("Orchestration main server failed to start.");
                })
                .build()
                .runMain(configurer);
    }

    public int getRandomPort() throws java.io.IOException {
        java.net.ServerSocket pubSubSocket = new java.net.ServerSocket(0);
        int ret = pubSubSocket.getLocalPort();
        pubSubSocket.close();
        return ret;
    }
}
