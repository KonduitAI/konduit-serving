package ai.konduit.serving.vertx.protocols.grpc.test;

import ai.konduit.serving.common.test.BaseSwaggerAnnotationCheck;
import org.junit.Test;

public class SwaggerAnnotationCheck extends BaseSwaggerAnnotationCheck {
    @Override
    public String getPackageName() {
        return "ai.konduit.serving.vertx.protocols.grpc";
    }


    @Test
    public void checkAnnotations() throws ClassNotFoundException {
        runTest();
    }
}
