package ai.konduit.serving.cli.data;

import ai.konduit.serving.common.test.BaseJsonCoverageTest;
import ai.konduit.serving.pipeline.util.ObjectMappers;

public class JsonCoverageTest extends BaseJsonCoverageTest {


    @Override
    public String getPackageName() {
        return "ai.konduit.serving.cli";
    }

    @Override
    public Object fromJson(Class<?> c, String json) {
        return ObjectMappers.fromJson(json, c);
    }

    @Override
    public Object fromYaml(Class<?> c, String yaml) {
        return ObjectMappers.fromYaml(yaml, c);
    }





}
