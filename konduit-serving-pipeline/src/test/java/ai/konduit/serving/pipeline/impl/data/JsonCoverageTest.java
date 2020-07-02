package ai.konduit.serving.pipeline.impl.data;

import ai.konduit.serving.common.test.BaseJsonCoverageTest;
import ai.konduit.serving.pipeline.impl.step.bbox.filter.BoundingBoxFilterStep;
import ai.konduit.serving.pipeline.impl.step.bbox.point.BoundingBoxToPointStep;
import ai.konduit.serving.pipeline.impl.step.logging.LoggingStep;
import ai.konduit.serving.pipeline.impl.step.ml.regression.RegressionOutputStep;
import ai.konduit.serving.pipeline.impl.step.ml.ssd.SSDToBoundingBoxStep;
import org.junit.Test;
import org.slf4j.event.Level;

import java.util.HashMap;
import java.util.Map;

public class JsonCoverageTest  extends BaseJsonCoverageTest {



    @Override
    public String getPackageName() {
        return "ai.konduit.serving.pipeline";
    }


    @Test
    public void testBoundingBoxFilterStep(){
        testConfigSerDe(new BoundingBoxFilterStep()
                .classesToKeep(new String[]{"x","y"})
                .inputName("foo")
                .outputName("bar"));
    }

    @Test
    public void testLoggingStep(){
        testConfigSerDe(new LoggingStep().log(LoggingStep.Log.KEYS_AND_VALUES).logLevel(Level.INFO));
    }


    @Test
    public void testBoundingBoxToPointStep(){
        testConfigSerDe(new BoundingBoxToPointStep()
                .bboxName("x")
                .outputName("y"));
    }

    @Test
    public void testSSDToBoundingBoxStep(){
        testConfigSerDe(new SSDToBoundingBoxStep()
                .outputName("y"));
    }



    @Test
    public void testRegressionOutputStep(){

        Map<String, Integer> hashMap = new HashMap<String, Integer>();
        hashMap.put("a",0);
        hashMap.put("c",2);

        testConfigSerDe(new RegressionOutputStep()
                .inputName("in")
                .names(hashMap))
    }



}
