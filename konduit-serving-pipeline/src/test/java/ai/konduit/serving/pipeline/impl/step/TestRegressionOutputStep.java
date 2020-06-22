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
package ai.konduit.serving.pipeline.impl.step;

import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import ai.konduit.serving.pipeline.impl.step.ml.ssd.regression.RegressionOutputStep;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class TestRegressionOutputStep {


    @Test
    public void testCaseNonBatchDouble() {
        Map<String, Integer> hashMap = new HashMap<String, Integer>();
        hashMap.put("a",0);
        hashMap.put("c",2);
        double[][] values = new double[][]{{0.1, 0.2, 0.3}};


        Pipeline p = SequencePipeline.builder()
                .add(new RegressionOutputStep()
                        .inputName("preds")
                        .names(hashMap))

                .build();

        // double values input
        NDArray preds = NDArray.create(values);
        Data in = Data.singleton("preds", preds);
        Data out = p.executor().exec(in);
        assertTrue((double)out.get("a") == 0.1);
        assertTrue((double)out.get("c") == 0.3);

        String json = p.toJson();
        String yaml = p.toYaml();

        Pipeline pj = Pipeline.fromJson(json);
        Pipeline py = Pipeline.fromYaml(yaml);

        assertEquals(p, pj);
        assertEquals(p, py);
    }


    @Test
    public void testCaseBatchDouble() {
        Map<String, Integer> hashMap = new HashMap<String, Integer>();
        hashMap.put("a",0);
        hashMap.put("c",2);
        double[][] values = new double[][]{{0.1, 0.2, 0.3}, {0.4, 0.5, 0.6}};


        Pipeline p = SequencePipeline.builder()
                .add(new RegressionOutputStep()
                        .inputName("preds")
                        .names(hashMap))

                .build();

        // double values input
        NDArray preds = NDArray.create(values);
        Data in = Data.singleton("preds", preds);
        Data out = p.executor().exec(in);

        List<Double> list1 = Arrays.asList(0.1, 0.4);
        List<Double> list2 = Arrays.asList(0.3, 0.6);

        assertTrue(out.get("a").equals(list1));
        assertTrue(out.get("c").equals(list2));

        String json = p.toJson();
        String yaml = p.toYaml();

        Pipeline pj = Pipeline.fromJson(json);
        Pipeline py = Pipeline.fromYaml(yaml);

        assertEquals(p, pj);
        assertEquals(p, py);
    }

    @Test
    public void testCaseNonBatchFloat() {
        Map<String, Integer> hashMap = new HashMap<String, Integer>();
        hashMap.put("a",0);
        hashMap.put("c",2);
        float[][] values = new float[][]{{(float) 0.1, (float) 0.2, (float) 0.3}};


        Pipeline p = SequencePipeline.builder()
                .add(new RegressionOutputStep()
                        .inputName("preds")
                        .names(hashMap))

                .build();

        // float values input
        NDArray preds = NDArray.create(values);
        Data in = Data.singleton("preds", preds);
        Data out = p.executor().exec(in);

        assertTrue(out.get("a").equals(0.1));
        assertTrue(out.get("c").equals(0.3));

        String json = p.toJson();
        String yaml = p.toYaml();

        Pipeline pj = Pipeline.fromJson(json);
        Pipeline py = Pipeline.fromYaml(yaml);

        assertEquals(p, pj);
        assertEquals(p, py);
    }


    @Test
    public void testCaseBatchFloat() {
        Map<String, Integer> hashMap = new HashMap<String, Integer>();
        hashMap.put("a",0);
        hashMap.put("c",2);
        float[][] values = new float[][]{{(float) 0.1, (float) 0.2, (float) 0.3}, {(float) 0.4, (float) 0.5, (float) 0.6}};


        Pipeline p = SequencePipeline.builder()
                .add(new RegressionOutputStep()
                        .inputName("preds")
                        .names(hashMap))

                .build();

        // float values input
        NDArray preds = NDArray.create(values);
        Data in = Data.singleton("preds", preds);
        Data out = p.executor().exec(in);
        List<Double> list1 = Arrays.asList(0.1, 0.4);
        List<Double> list2 = Arrays.asList(0.3, 0.6);

        assertTrue(out.get("a").equals(list1));
        assertTrue(out.get("c").equals(list2));

        String json = p.toJson();
        String yaml = p.toYaml();

        Pipeline pj = Pipeline.fromJson(json);
        Pipeline py = Pipeline.fromYaml(yaml);

        assertEquals(p, pj);
        assertEquals(p, py);
    }




}


