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
import ai.konduit.serving.pipeline.impl.step.ml.regression.RegressionOutputStep;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;


public class TestRegressionOutputStep {

    private final double eps = 0.0000001;


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
        assertEquals(0.1, (double)out.get("a"), eps);
        assertEquals(0.3, (double)out.get("c"), eps);

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

        assertEquals(list1,out.get("a"));
        assertEquals(list2,out.get("c"));

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

        assertEquals(0.1, (double) out.get("a"), eps);
        assertEquals(0.3,(double) out.get("c"), eps);

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
        double[] expected1 = new double[]{0.1, 0.4};
        double[] expected2 = new double[]{0.3, 0.6};
        double[] actual1 = out.getListDouble("a").stream().mapToDouble(d -> d).toArray();;
        double[] actual2 = out.getListDouble("c").stream().mapToDouble(d -> d).toArray();;


        assertArrayEquals(expected1,actual1,eps);
        assertArrayEquals(expected2,actual2,eps);

        String json = p.toJson();
        String yaml = p.toYaml();

        Pipeline pj = Pipeline.fromJson(json);
        Pipeline py = Pipeline.fromYaml(yaml);

        assertEquals(p, pj);
        assertEquals(p, py);
    }




}


