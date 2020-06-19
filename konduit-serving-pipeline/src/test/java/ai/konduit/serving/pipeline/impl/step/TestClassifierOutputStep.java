package ai.konduit.serving.pipeline.impl.step;

import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import ai.konduit.serving.pipeline.impl.step.ml.classifier.ClassifierOutputStep;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class TestClassifierOutputStep {


    @Test
    public void testcase1() {


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


                                        NDArray preds = NDArray.create(values);
                                        Data in = Data.singleton("preds", preds);
                                        Data out = p.executor().exec(in);

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


