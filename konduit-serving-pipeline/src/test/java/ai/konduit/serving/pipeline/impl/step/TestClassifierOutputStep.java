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


        int bS = 1;
        int numClasses = 3;

        double values[][] = new double[bS][numClasses];
        for (int i = 0; i < values.length; i++) {
            for (int j = 0; j < values[i].length; j++) {
                values[i][j] = (Math.random() * 10);
            }

            List<String> labelsList = new ArrayList<String>();
            labelsList.add("apple");
            labelsList.add("banana");
            labelsList.add("orange");

            Pipeline p = SequencePipeline.builder()
                    .add(new ClassifierOutputStep()
                            .inputName("preds")
                            .returnIndex(true)
                            .returnLabel(true)
                            .returnProb(true)
                            .allProbabilities(true)
                            .labelName("label")
                            .indexName("index")
                            .probName("prob")
                            .Labels(labelsList))
                    .build();


            NDArray preds = NDArray.create(values);
            Data in = Data.singleton("preds", preds);
            Data out = p.executor().exec(in);

        }


    }
}
