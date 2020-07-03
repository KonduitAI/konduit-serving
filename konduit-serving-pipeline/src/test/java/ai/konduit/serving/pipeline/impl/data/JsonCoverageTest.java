/*
 *  ******************************************************************************
 *  * Copyright (c) 2020 Konduit K.K.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Apache License, Version 2.0 which is available at
 *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  * License for the specific language governing permissions and limitations
 *  * under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *  *****************************************************************************
 */

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

import java.util.*;
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


    @Override
    public Set<Class<?>> ignores(){
        Set<Class<?>> set = new HashSet<>();
        set.add(CallbackStep.class);
        set.add(ai.konduit.serving.pipeline.impl.testpipelines.callback.CallbackStep.class);
        // I need PipelineProfilerTest$TestStep i.e. private static class inside
        set.add(ai.konduit.serving.pipeline.impl.pipeline.PipelineProfilerTest.class);
        set.add(FunctionStep.class);
        set.add(Input.class);
        return set;
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
        testConfigSerDe(new AsyncPipeline(new GraphPipeline(steps, "foo", "myGraphPipeline"), new SimpleLoopTrigger()));
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
        testConfigSerDe(new GraphBuilder().build(new MergeStep(new GraphBuilder(), new ArrayList<String>(), "foo")));
        seen.add(MergeStep.class);
    }


    @Test
    public void testPipelineGraphStep() {
        testConfigSerDe(new PipelineGraphStep(new GraphBuilder(), new BoundingBoxToPointStep(), "step", "name"));
    }

    @Test
    public void testSwitchOutput() {
        testConfigSerDe(new GraphBuilder().build(new SwitchOutput(new GraphBuilder(), "foo", "bar", 2)));
        seen.add(SwitchOutput.class);
    }





}
