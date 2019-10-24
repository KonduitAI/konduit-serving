package ai.konduit.serving.orchestration;

import  ai.konduit.serving.configprovider.KonduitServingNodeConfigurer;

public class KonduitOrchestrationMainTest {

    @org.junit.Test
    public void testClusterRun() throws Exception {
        KonduitOrchestrationMain konduitOrchestrationMain = new KonduitOrchestrationMain();

        ai.konduit.serving.InferenceConfiguration inferenceConfiguration = ai.konduit.serving.InferenceConfiguration.builder().build();
        java.io.File tmpFile = new java.io.File("file.json");
        org.apache.commons.io.FileUtils.writeStringToFile(tmpFile,inferenceConfiguration.toJson(), java.nio.charset.Charset.defaultCharset());
        tmpFile.deleteOnExit();
        KonduitServingNodeConfigurer configurer  = KonduitServingNodeConfigurer.builder()
                .configPath(tmpFile.getAbsolutePath())
                .build();
        konduitOrchestrationMain.runMain(configurer);
    }

}
