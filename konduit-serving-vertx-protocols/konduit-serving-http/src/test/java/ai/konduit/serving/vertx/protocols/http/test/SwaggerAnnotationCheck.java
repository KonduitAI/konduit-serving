package ai.konduit.serving.vertx.protocols.http.test;

import ai.konduit.serving.common.test.BaseSwaggerAnnotationCheck;
import org.junit.Test;

public class SwaggerAnnotationCheck extends BaseSwaggerAnnotationCheck {
    @Override
    public String getPackageName() {
        return "ai.konduit.serving.vertx.protocols.http";
    }


    @Test
    public void checkAnnotations() throws ClassNotFoundException {
        runTest();
    }
}
