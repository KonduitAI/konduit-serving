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
import ai.konduit.serving.pipeline.api.data.ValueType;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import ai.konduit.serving.pipeline.impl.step.ml.classifier.ClassifierOutputStep;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class TestClassifierOutputStep {


    @Test
    public void testCaseWithDouble() {


        int numClasses = 3;

        //some labels
        List<String> labelsList1 = new ArrayList<String>();
        labelsList1.add("apple");
        labelsList1.add("banana");
        labelsList1.add("orange");

        //just empty
        List<String> labelsList2 = new ArrayList<String>();

        // list with labels, empty and null
        List<List<String>> labelsLists = new ArrayList<List<String>>();
        labelsLists.add(labelsList1);
        labelsLists.add(labelsList2);
        labelsLists.add(null);


        for (boolean retIndex : new boolean[]{false, true}) {
            for (boolean retProb : new boolean[]{false, true}) {
                for (boolean retLabel : new boolean[]{false, true}) {
                    for (boolean retAllProb : new boolean[]{false, true}) {
                        for (int bS : new int[]{1, 3}) {
                            for (Integer topN : new Integer[]{null, 1, 3}) {
                                for (List<String> labels : labelsLists) {

                                    double values[][] = new double[bS][numClasses];
                                    for (int i = 0; i < values.length; i++) {
                                        for (int j = 0; j < values[i].length; j++) {
                                            values[i][j] = (Math.random() * 10);
                                        }
                                    }


                                    Pipeline p = SequencePipeline.builder()
                                            .add(new ClassifierOutputStep()
                                                    .inputName("preds")
                                                    .returnIndex(retIndex)
                                                    .returnLabel(retLabel)
                                                    .returnProb(retProb)
                                                    .allProbabilities(retAllProb)
                                                    .labelName("label")
                                                    .topN(topN)
                                                    .indexName("index")
                                                    .probName("prob")
                                                    .Labels(labels))
                                            .build();

                                    // double values input
                                    NDArray preds = NDArray.create(values);
                                    Data in = Data.singleton("preds", preds);
                                    Data out = p.executor().exec(in);

                                    if (retProb ) {
                                        assertTrue(out.has("prob") && (out.type("prob") == ValueType.DOUBLE||out.type("prob") == ValueType.LIST));
                                    }

                                    if (retIndex ) {
                                        assertTrue(out.has("index") && (out.type("index") == ValueType.INT64||out.type("index") == ValueType.LIST));
                                    }

                                    if (retLabel) {
                                        assertTrue(out.has("label") && (out.type("label") == ValueType.STRING||out.type("label") == ValueType.LIST));
                                    }

                                    if (retAllProb) {
                                        assertTrue(out.has("allProbabilities") && (out.type("allProbabilities") == ValueType.LIST));
                                    }


                                }


                            }
                        }
                    }
                }
            }
        }
    }


    @Test
    public void testCaseWithFloat() {


        int numClasses = 3;

        //some labels
        List<String> labelsList1 = new ArrayList<String>();
        labelsList1.add("apple");
        labelsList1.add("banana");
        labelsList1.add("orange");

        //just empty
        List<String> labelsList2 = new ArrayList<String>();

        // list with labels, empty and null
        List<List<String>> labelsLists = new ArrayList<List<String>>();
        labelsLists.add(labelsList1);
        labelsLists.add(labelsList2);
        labelsLists.add(null);


        for (boolean retIndex : new boolean[]{false, true}) {
            for (boolean retProb : new boolean[]{false, true}) {
                for (boolean retLabel : new boolean[]{false, true}) {
                    for (boolean retAllProb : new boolean[]{false, true}) {
                        for (int bS : new int[]{1, 3}) {
                            for (Integer topN : new Integer[]{null, 1, 3}) {
                                for (List<String> labels : labelsLists) {


                                    float values[][] = new float[bS][numClasses];
                                    for (int i = 0; i < values.length; i++) {
                                        for (int j = 0; j < values[i].length; j++) {
                                            values[i][j] = (float) (Math.random() * 10);
                                        }
                                    }


                                    Pipeline p = SequencePipeline.builder()
                                            .add(new ClassifierOutputStep()
                                                    .inputName("preds")
                                                    .returnIndex(retIndex)
                                                    .returnLabel(retLabel)
                                                    .returnProb(retProb)
                                                    .allProbabilities(retAllProb)
                                                    .labelName("label")
                                                    .topN(topN)
                                                    .indexName("index")
                                                    .probName("prob")
                                                    .Labels(labels))
                                            .build();

                                    // double values input
                                    NDArray preds = NDArray.create(values);
                                    Data in = Data.singleton("preds", preds);
                                    Data out = p.executor().exec(in);

                                    if (retProb ) {
                                        assertTrue(out.has("prob") && (out.type("prob") == ValueType.DOUBLE||out.type("prob") == ValueType.LIST));
                                    }

                                    if (retIndex ) {
                                        assertTrue(out.has("index") && (out.type("index") == ValueType.INT64||out.type("index") == ValueType.LIST));
                                    }

                                    if (retLabel) {
                                        assertTrue(out.has("label") && (out.type("label") == ValueType.STRING||out.type("label") == ValueType.LIST));
                                    }

                                    if (retAllProb) {
                                        assertTrue(out.has("allProbabilities") && (out.type("allProbabilities") == ValueType.LIST));
                                    }


                                }


                            }
                        }
                    }
                }
            }
        }
    }

}


