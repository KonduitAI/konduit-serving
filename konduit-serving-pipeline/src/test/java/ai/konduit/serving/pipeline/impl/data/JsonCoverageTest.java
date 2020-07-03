package ai.konduit.serving.pipeline.impl.data;

import ai.konduit.serving.common.test.BaseJsonCoverageTest;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.impl.pipeline.AsyncPipeline;
import ai.konduit.serving.pipeline.impl.pipeline.GraphPipeline;
import ai.konduit.serving.pipeline.impl.pipeline.PipelineProfilerTest;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import ai.konduit.serving.pipeline.impl.pipeline.graph.*;
import ai.konduit.serving.pipeline.impl.pipeline.graph.switchfn.DataIntSwitchFn;
import ai.konduit.serving.pipeline.impl.pipeline.graph.switchfn.DataStringSwitchFn;
import ai.konduit.serving.pipeline.impl.pipeline.loop.SimpleLoopTrigger;
import ai.konduit.serving.pipeline.impl.pipeline.loop.TimeLoopTrigger;
import ai.konduit.serving.pipeline.impl.step.bbox.filter.BoundingBoxFilterStep;
import ai.konduit.serving.pipeline.impl.step.bbox.point.BoundingBoxToPointStep;
import ai.konduit.serving.pipeline.impl.step.bbox.yolo.YoloToBoundingBoxStep;
import ai.konduit.serving.pipeline.impl.step.logging.LoggingStep;
import ai.konduit.serving.pipeline.impl.step.ml.classifier.ClassifierOutputStep;
import ai.konduit.serving.pipeline.impl.step.ml.regression.RegressionOutputStep;
import ai.konduit.serving.pipeline.impl.step.ml.ssd.SSDToBoundingBoxStep;
import ai.konduit.serving.pipeline.impl.testpipelines.count.CountStep;
import ai.konduit.serving.pipeline.impl.testpipelines.fn.FunctionStep;
import ai.konduit.serving.pipeline.impl.testpipelines.switchfn.TestSwitchFn;
import ai.konduit.serving.pipeline.impl.testpipelines.time.TimeStep;
import ai.konduit.serving.pipeline.impl.util.CallbackStep;
import ai.konduit.serving.pipeline.util.ObjectMappers;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.event.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class JsonCoverageTest extends BaseJsonCoverageTest {


    @Override
    public String getPackageName() {
        return "ai.konduit.serving.pipeline";
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
    public void testBoundingBoxFilterStep() {
        testConfigSerDe(new BoundingBoxFilterStep()
                .classesToKeep(new String[]{"x", "y"})
                .inputName("foo")
                .outputName("bar"));
    }

    @Test
    public void testLoggingStep() {
        testConfigSerDe(new LoggingStep().log(LoggingStep.Log.KEYS_AND_VALUES).logLevel(Level.INFO));
    }


    @Test
    public void testBoundingBoxToPointStep() {
        testConfigSerDe(new BoundingBoxToPointStep()
                .bboxName("x")
                .outputName("y"));
    }

    @Test
    public void testSSDToBoundingBoxStep() {
        testConfigSerDe(new SSDToBoundingBoxStep()
                .outputName("y"));
    }


    @Test
    public void testRegressionOutputStep() {

        Map<String, Integer> hashMap = new HashMap<String, Integer>();
        hashMap.put("a", 0);
        hashMap.put("c", 2);

        testConfigSerDe(new RegressionOutputStep()
                .inputName("in")
                .names(hashMap));
    }

    @Test
    public void testYoloStep() {
        testConfigSerDe(new YoloToBoundingBoxStep());
    }



    @Test
    public void testClassifierOutputStep() {

        testConfigSerDe(new ClassifierOutputStep()
                .inputName("preds")
                .returnIndex(true)
                .returnLabel(true)
                .returnProb(true)
                .allProbabilities(true)
                .labelName("label")
                .topN(2)
                .indexName("index")
                .probName("prob"));
    }


    @Test
    public void testSimpleLoopTriggerT() {
        testConfigSerDe(new SimpleLoopTrigger(500L));
    }

    @Test
    public void testTimeLoopTrigger() {
        testConfigSerDe(new TimeLoopTrigger(1, TimeUnit.MINUTES));
    }


    @Test
    public void testTimeStepTest() {
        testConfigSerDe(new TimeStep());
    }


    @Test
    public void testCountStepTest() {
        testConfigSerDe(new CountStep(1));
    }

    @Test
    public void testSwitchFn() {
        testConfigSerDe(new TestSwitchFn().branch(1));
    }

    @Test
    public void testDataStringSwitchFn() {
        Map<String, Integer> hashMap = new HashMap<String, Integer>();
        hashMap.put("a", 0);
        testConfigSerDe(new DataStringSwitchFn(1, "foo", hashMap));
    }

    @Test
    public void testDataIntSwitchFn() {
        testConfigSerDe(new DataIntSwitchFn(1, "foo"));
    }

    @Test
    public void testSwitchStep() {
        testConfigSerDe(new SwitchStep("foo", null));
    }

    @Test
    public void testGraphPipeline() {
        Map<String, GraphStep> steps = new HashMap<String, GraphStep>();
        testConfigSerDe(new GraphPipeline(steps, "foo", "myGraphPipeline"));
    }

    @Test
    public void testAsyncPipeline() {
        Map<String, GraphStep> steps = new HashMap<String, GraphStep>();
        testConfigSerDe(new ai.konduit.serving.pipeline.impl.pipeline.AsyncPipeline(new GraphPipeline(steps, "foo", "myGraphPipeline"), new SimpleLoopTrigger()) {
        });
    }


    @Test
    public void testSequencePipeline() {
        testConfigSerDe(new SequencePipeline(new ArrayList<PipelineStep>(), "foo"));
    }


    @Test
    public void testAnyStep() {
        testConfigSerDe(new AnyStep(new GraphBuilder(), new ArrayList<String>(), "foo"));
    }


    @Test
    public void testMergeStep() {
        testConfigSerDe(new MergeStep(new GraphBuilder(), new ArrayList<String>(), "foo"));
    }


    @Test
    public void testPipelineGraphStep() {
        testConfigSerDe(new PipelineGraphStep(new GraphBuilder(), new BoundingBoxToPointStep(), "step", "name"));
    }

    @Test
    public void testSwitchOutput() {
        testConfigSerDe(new SwitchOutput(new GraphBuilder(), "foo", "bar", 2));
    }


    // Failed tests

    // fails with https://gist.github.com/atuzhykov/666a99c9dd540a57437750ac31114528
    @Test
    @Ignore
    public void testInput() {
        testConfigSerDe(new Input(new GraphBuilder().id("foo")));
    }

    // fails with https://gist.github.com/atuzhykov/fc87f4064ad00300da34403857d15d55
    @Test
    @Ignore
    public void testPipelineProfilerTest() {
        testConfigSerDe(new PipelineProfilerTest());
    }


    // fails with https://gist.github.com/atuzhykov/e276977b848c210daaea52737557f78d
    @Test
    @Ignore
    public void testFunctionStepTest() {
        testConfigSerDe(new FunctionStep(d ->
                Data.singleton("outputStep1", "outputStep1Value")
        ));
    }

    // fails with https://gist.github.com/atuzhykov/1043a4a236f00573589c73eaedd0349e
    @Test
    @Ignore
    public void testCallbackStepFromUtilTest() {
        List<String> execOrder = new ArrayList<>();
        testConfigSerDe(new ai.konduit.serving.pipeline.impl.util.CallbackStep(d -> execOrder.add("step")));
    }


    // fails with https://gist.github.com/atuzhykov/c3ae1409bbf18a4e635e4336127de091
    @Test
    @Ignore
    public void testCallbackStepFromTestPipelinesTest() {
        List<String> execOrder = new ArrayList<>();
        testConfigSerDe(new ai.konduit.serving.pipeline.impl.testpipelines.callback.CallbackStep(d -> execOrder.add("step")));
    }


}
