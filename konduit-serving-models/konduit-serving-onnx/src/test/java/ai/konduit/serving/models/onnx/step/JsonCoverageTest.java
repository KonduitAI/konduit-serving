package ai.konduit.serving.models.onnx.step;

import ai.konduit.serving.common.test.BaseJsonCoverageTest;
import ai.konduit.serving.pipeline.util.ObjectMappers;
import org.junit.Test;

public class JsonCoverageTest extends BaseJsonCoverageTest {

    @Override
    public String getPackageName() {
        return "ai.konduit.serving.models.onnx";
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
    public void testONNXStep() {
        testConfigSerDe(new ONNXStep()
                .inputNames("inputNames").loaderClass("loaderClass")
                .modelUri("modelUri").outputNames("outputNames"));
    }



}
