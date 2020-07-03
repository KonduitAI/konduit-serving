package ai.konduit.serving.models.tensorflow;

import ai.konduit.serving.common.test.BaseJsonCoverageTest;
import ai.konduit.serving.models.tensorflow.step.TensorFlowStep;
import ai.konduit.serving.pipeline.util.ObjectMappers;
import org.junit.Test;

public class JsonCoverageTest extends BaseJsonCoverageTest {

    @Override
    public String getPackageName() {
        return "ai.konduit.serving.models.tensorflow";
    }

    @Override
    public Object fromJson(Class<?> c, String json) {
        return ObjectMappers.fromJson(json, c);
    }

    @Override
    public Object fromYaml(Class<?> c, String yaml) {
        return ObjectMappers.fromYaml(yaml, c);
    }

    @Test
    public void testTensorFlowStepStep() {
        testConfigSerDe(new TensorFlowStep(null, null));
    }








}
