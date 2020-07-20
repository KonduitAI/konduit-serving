package ai.konduit.serving.deeplearning4j;

import ai.konduit.serving.common.test.BaseJsonCoverageTest;
import ai.konduit.serving.models.deeplearning4j.step.DL4JStep;
import ai.konduit.serving.models.deeplearning4j.step.keras.KerasStep;
import ai.konduit.serving.pipeline.util.ObjectMappers;
import org.junit.Test;

public class JsonCoverageTest extends BaseJsonCoverageTest {

    @Override
    public String getPackageName() {
        return "ai.konduit.serving.models.deeplearning4j";
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
    public void testDL4JStep() {
        testConfigSerDe(new DL4JStep()
                .inputNames("inputNames").loaderClass("loaderClass")
                .modelUri("modelUri").outputNames("outputNames"));
    }

    @Test
    public void testKerasStep() {
        testConfigSerDe(new KerasStep().inputNames("inputNames").outputNames("outputNames")
                .modelUri("modelUri"));
    }


}
