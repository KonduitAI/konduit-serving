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

package ai.konduit.serving.pipeline.impl.pipeline;

import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.ValueType;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;
import ai.konduit.serving.pipeline.api.serde.JsonSubType;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.impl.pipeline.graph.GraphBuilder;
import ai.konduit.serving.pipeline.impl.pipeline.graph.GraphStep;
import ai.konduit.serving.pipeline.impl.pipeline.graph.SwitchFn;
import ai.konduit.serving.pipeline.impl.pipeline.graph.switchfn.DataIntSwitchFn;
import ai.konduit.serving.pipeline.impl.pipeline.graph.switchfn.DataStringSwitchFn;
import ai.konduit.serving.pipeline.impl.testpipelines.callback.CallbackStep;
import ai.konduit.serving.pipeline.impl.testpipelines.count.CountStep;
import ai.konduit.serving.pipeline.impl.testpipelines.fn.FunctionStep;
import ai.konduit.serving.pipeline.impl.testpipelines.switchfn.TestSwitchFn;
import ai.konduit.serving.pipeline.util.ObjectMappers;
import org.junit.Test;
import org.nd4j.shade.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GraphPipelineTest {

    @Test
    public void testSimple(){

        GraphBuilder b = new GraphBuilder();

        GraphStep input = b.input();

        Pipeline p = b.build(input);

        Data in = Data.singleton("in", "value");

        PipelineExecutor exec = p.executor();
        Data out = exec.exec(in);
        assertEquals(in, out);
    }

    @Test
    public void testSimple2(){

        GraphBuilder b = new GraphBuilder();
        GraphStep input = b.input();

        List<String> execOrder = new ArrayList<>();
        GraphStep step1 = input.then("step1", new CallbackStep(d -> execOrder.add("step1")));
        GraphStep step2 = step1.then("step2", new CallbackStep(d -> execOrder.add("step2")));
        GraphStep step3 = step2.then("step3", new CallbackStep(d -> execOrder.add("step3")));

        Pipeline p = b.build(step3);

        Data in = Data.singleton("in", "value");

        PipelineExecutor exec = p.executor();
        Data out = exec.exec(in);
        assertEquals(in, out);

        assertEquals(Arrays.asList("step1", "step2", "step3"), execOrder);
    }

    @Test
    public void testMergeSimple(){

        GraphBuilder b = new GraphBuilder();
        GraphStep input = b.input();

        List<String> execOrder = new ArrayList<>();
        GraphStep step1 = input.then("step1", new FunctionStep(d -> {
            assertEquals(1, d.size());
            assertEquals(ValueType.STRING, d.type("input"));
            return Data.singleton("outputStep1", "outputStep1Value");
        }));

        GraphStep step2 = input.then("step2", new FunctionStep(d -> {
            assertEquals(1, d.size());
            assertEquals(ValueType.STRING, d.type("input"));
            return Data.singleton("outputStep2", "outputStep2Value");
        }));

        GraphStep merge = step1.mergeWith("merge", step2);


        Pipeline p = b.build(merge);
        GraphPipeline gp = (GraphPipeline)p;

        Map<String,GraphStep> s = gp.steps();
        assertTrue(s.containsKey("step1"));
        assertTrue(s.containsKey("step2"));
        assertTrue(s.containsKey("merge"));


        PipelineExecutor exec = p.executor();


        Data in = Data.singleton("input", "inputValue");
        Data out = exec.exec(in);

        Data exp = Data.singleton("outputStep1", "outputStep1Value");
        exp.put("outputStep2", "outputStep2Value");


        assertEquals(exp, out);
    }

    @Test
    public void testSwitchAny(){
        ObjectMappers.registerSubtypes(Collections.singletonList(new JsonSubType("TEST_SWITCH_FN", TestSwitchFn.class, SwitchFn.class)));

        GraphBuilder b = new GraphBuilder();
        GraphStep input = b.input();

        TestSwitchFn fn = new TestSwitchFn();
        GraphStep[] sw = b.switchOp("switch", fn, input);
        assertEquals(2, sw.length);
        GraphStep left = sw[0];
        GraphStep right = sw[1];

        CountStep leftCount = new CountStep();
        CountStep rightCount = new CountStep();
        GraphStep lOut = left.then("testLeft", leftCount);
        GraphStep rOut = right.then("testRight", rightCount);

        GraphStep any = b.any("any", lOut, rOut);

        Pipeline p = b.build(any);
        p.id();

        Data in = Data.singleton("k", "v");

        PipelineExecutor exec = p.executor();

        Data outLeft = exec.exec(in);
        assertEquals(in, outLeft);
        assertEquals(1, leftCount.count);
        assertEquals(0, rightCount.count);

        fn.branch = 1;
        leftCount.count = 0;
        Data outRight = exec.exec(in);
        assertEquals(in, outRight);
        assertEquals(0, leftCount.count);
        assertEquals(1, rightCount.count);

        String json = p.toJson();
        System.out.println(json);
        Pipeline fromJson = Pipeline.fromJson(json);

        assertEquals(p, fromJson);

        PipelineExecutor exec2 = fromJson.executor();
        Data outLeft2 = exec2.exec(in);
        assertEquals(outLeft, outLeft2);

        Data outRight2 = exec2.exec(in);
        assertEquals(outRight, outRight2);
    }

    @Test
    public void testSwitchFunctions(){
        for(boolean str : new boolean[]{false, true}) {

            GraphBuilder b = new GraphBuilder();
            GraphStep input = b.input();

            SwitchFn fn;
            if(str){
                Map<String,Integer> m = new HashMap<>();
                m.put("first", 0);
                m.put("second", 1);
                fn = new DataStringSwitchFn(2, "string", m);
            } else {
                fn = new DataIntSwitchFn(2, "int");
            }
            GraphStep[] sw = b.switchOp("switch", fn, input);
            assertEquals(2, sw.length);
            GraphStep left = sw[0];
            GraphStep right = sw[1];

            CountStep leftCount = new CountStep();
            CountStep rightCount = new CountStep();
            GraphStep lOut = left.then("testLeft", leftCount);
            GraphStep rOut = right.then("testRight", rightCount);

            GraphStep any = b.any("any", lOut, rOut);

            Pipeline p = b.build(any);
            p.id();


            PipelineExecutor exec = p.executor();

            Data in = Data.singleton("k", "v");

            //Test left branch
            if(str){
                in.put("string", "first");
            } else {
                in.put("int", 0);
            }
            Data outLeft = exec.exec(in);
            assertEquals(in, outLeft);
            assertEquals(1, leftCount.count);
            assertEquals(0, rightCount.count);

            //Test right branch
            if(str){
                in.put("string", "second");
            } else {
                in.put("int", 1);
            }
            leftCount.count = 0;
            Data outRight = exec.exec(in);
            assertEquals(in, outRight);
            assertEquals(0, leftCount.count);
            assertEquals(1, rightCount.count);

            String json = p.toJson();
            System.out.println(json);
            Pipeline fromJson = Pipeline.fromJson(json);

            assertEquals(p, fromJson);

            PipelineExecutor exec2 = fromJson.executor();
            Data outLeft2 = exec2.exec(in);
            assertEquals(outLeft, outLeft2);

            Data outRight2 = exec2.exec(in);
            assertEquals(outRight, outRight2);
        }
    }
}
