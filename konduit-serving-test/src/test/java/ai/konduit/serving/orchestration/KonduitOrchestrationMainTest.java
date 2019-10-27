package ai.konduit.serving.orchestration;

import static com.jayway.restassured.RestAssured.given;

public class KonduitOrchestrationMainTest {

    @org.junit.Test
    public void testClusterRun() throws Exception {
        com.hazelcast.core.HazelcastInstance hzInstance = com.hazelcast.core.Hazelcast.newHazelcastInstance();
        Thread.sleep(10000);

        KonduitOrchestrationMain konduitOrchestrationMain = new KonduitOrchestrationMain();
        int port = getRandomPort();

        ai.konduit.serving.config.ServingConfig servingConfig = ai.konduit.serving.config.ServingConfig.builder()
                .httpPort(port)
                .build();
        ai.konduit.serving.InferenceConfiguration inferenceConfiguration = ai.konduit.serving.InferenceConfiguration.builder()
                .servingConfig(servingConfig)
                .build();
        java.io.File tmpFile = new java.io.File("file.json");
        org.apache.commons.io.FileUtils.writeStringToFile(tmpFile,inferenceConfiguration.toJson(), java.nio.charset.Charset.defaultCharset());
        tmpFile.deleteOnExit();
        /**
         * Need to work out what a "node" is: eg, what happens when you deploy 2 instances on vertx?
         * What happens when you have 2 separate verticles?
         *
         * What should "nodes" return?
         */

        ai.konduit.serving.configprovider.KonduitServingNodeConfigurer configurer  = ai.konduit.serving.configprovider.KonduitServingNodeConfigurer.builder()
                .configPath(tmpFile.getAbsolutePath())
                .build();
        konduitOrchestrationMain.runMain(configurer);

        Thread.sleep(10000);

        com.jayway.restassured.response.Response response = given().port(port)
                .get("/nodes")
                .then()
                .statusCode(200).and()
                .contentType("application/json")
                .extract().response();
        System.out.println(response.body().prettyPrint());

    }


    public int getRandomPort() throws java.io.IOException {
        java.net.ServerSocket pubSubSocket = new java.net.ServerSocket(0);
        int ret  = pubSubSocket.getLocalPort();
        pubSubSocket.close();
        return ret;
    }



}
