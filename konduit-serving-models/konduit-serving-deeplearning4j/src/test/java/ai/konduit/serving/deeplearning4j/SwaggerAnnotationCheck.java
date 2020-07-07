package ai.konduit.serving.deeplearning4j;

import ai.konduit.serving.common.test.BaseSwaggerAnnotationCheck;
import org.junit.Test;

public class SwaggerAnnotationCheck extends BaseSwaggerAnnotationCheck {
    @Override
    public String getPackageName() {
        return "ai.konduit.serving.models.deeplearning4j";
    }


    @Test
    public void checkAnnotations() throws ClassNotFoundException {
        runTest();
    }
}
