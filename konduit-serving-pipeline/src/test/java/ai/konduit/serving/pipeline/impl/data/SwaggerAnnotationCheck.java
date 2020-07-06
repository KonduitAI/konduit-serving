package ai.konduit.serving.pipeline.impl.data;

import ai.konduit.serving.common.test.BaseSwaggerAnnotationCheck;
import org.junit.Test;

public class SwaggerAnnotationCheck extends BaseSwaggerAnnotationCheck {
    @Override
    public String getPackageName() {
        return "ai.konduit.serving.pipeline";
    }

    @Test
    public void checkAnnotations() throws ClassNotFoundException {
        runTest();
    }
}
