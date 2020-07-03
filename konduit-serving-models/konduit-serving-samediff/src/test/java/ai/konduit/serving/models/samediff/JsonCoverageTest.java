package ai.konduit.serving.models.samediff;

import ai.konduit.serving.common.test.BaseJsonCoverageTest;
import ai.konduit.serving.models.samediff.step.SameDiffStep;
import ai.konduit.serving.pipeline.util.ObjectMappers;
import org.junit.Test;

public class JsonCoverageTest extends BaseJsonCoverageTest {

    @Override
    public String getPackageName() {
        return "ai.konduit.serving.models.samediff";
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
    public void testSameDiffStep() {
        testConfigSerDe(new SameDiffStep(null, null));
    }




}
