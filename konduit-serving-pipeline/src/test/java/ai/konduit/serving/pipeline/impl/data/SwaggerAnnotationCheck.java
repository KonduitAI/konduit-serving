package ai.konduit.serving.pipeline.impl.data;

import ai.konduit.serving.common.test.BaseSwaggerAnnotationCheck;
import ai.konduit.serving.pipeline.impl.pipeline.PipelineProfilerTest;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class SwaggerAnnotationCheck extends BaseSwaggerAnnotationCheck {
    @Override
    public String getPackageName() {
        return "ai.konduit.serving.pipeline";
    }

    @Override
    public Set<Class<?>> ignores() {
        Set<Class<?>> set = new HashSet<>();
        set.add(PipelineProfilerTest.TestStep.class);
        return set;
    }


    @Test
    public void checkAnnotations() throws ClassNotFoundException {
        runTest();
    }
}
