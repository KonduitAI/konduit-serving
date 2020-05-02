package ai.konduit.serving.deeplearning4j;

import ai.konduit.serving.deeplearning4j.step.DL4JModelPipelineStep;
import ai.konduit.serving.pipeline.api.Data;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;
import ai.konduit.serving.pipeline.impl.data.NDArray;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.File;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class TestDL4JModelStep {

    @Rule
    public TemporaryFolder testDir = new TemporaryFolder();

    @Test
    public void testSimpleMLN() throws Exception {

        for(boolean withNamesDefined : new boolean[]{false, true}) {

            File netFile = createIrisMLNFile();

            Pipeline p = SequencePipeline.builder()
                    .add(DL4JModelPipelineStep.builder()
                            .modelUri(netFile.toURI().toString())
                            .inputNames(withNamesDefined ? Collections.singletonList("in") : null)
                            .outputNames(withNamesDefined ? Collections.singletonList("myPrediction") : null)
                            .build())
                    .build();


            PipelineExecutor e = p.executor();
            String outName = withNamesDefined ? "myPrediction" : "default";


            INDArray arr = Nd4j.rand(DataType.FLOAT, 3, 4);
            INDArray exp = predictFromFile(netFile, arr);

            Data d = Data.singleton("in", new NDArray(arr));

            Data out = e.exec(d);
            INDArray actual = (INDArray) out.getNDArray(outName).getArrayValue();         //TODO FIX TYPE

            assertEquals(exp, actual);

            String json = p.toJson();
            System.out.println(json);
            Pipeline pJson = Pipeline.fromJson(json);
            INDArray outJson = (INDArray) pJson.executor().exec(d).getNDArray(outName).getArrayValue();
            assertEquals(exp, outJson);

            String yaml = p.toYaml();
            System.out.println(yaml);
            Pipeline pYaml = Pipeline.fromYaml(yaml);
            INDArray outYaml = (INDArray) pYaml.executor().exec(d).getNDArray(outName).getArrayValue();
            assertEquals(exp, outYaml);
        }
    }

    @Test
    public void testSimpleCompGraph() throws Exception {

        for(boolean withNamesDefined : new boolean[]{false, true}) {
            File netFile = createIrisCGFile();

            Pipeline p = SequencePipeline.builder()
                    .add(DL4JModelPipelineStep.builder()
                            .modelUri(netFile.toURI().toString())
                            .inputNames(withNamesDefined ? Collections.singletonList("in") : null)
                            .outputNames(withNamesDefined ? Collections.singletonList("myPrediction") : null)
                            .build())
                    .build();

            PipelineExecutor e = p.executor();
            String outName = withNamesDefined ? "myPrediction" : "default";


            INDArray arr = Nd4j.rand(DataType.FLOAT, 3, 4);
            INDArray exp = predictFromFileCG(netFile, arr)[0];

            Data d = Data.singleton("in", new NDArray(arr));

            Data out = e.exec(d);
            INDArray actual = (INDArray) out.getNDArray(outName).getArrayValue();         //TODO FIX TYPE/CASTING

            assertEquals(exp, actual);


            String json = p.toJson();
            Pipeline pJson = Pipeline.fromJson(json);
            INDArray outJson = (INDArray) pJson.executor().exec(d).getNDArray(outName).getArrayValue();
            assertEquals(exp, outJson);

            String yaml = p.toYaml();
            Pipeline pYaml = Pipeline.fromYaml(yaml);
            INDArray outYaml = (INDArray) pYaml.executor().exec(d).getNDArray(outName).getArrayValue();
            assertEquals(exp, outYaml);
        }
    }

    public File createIrisMLNFile() throws Exception {
        File dir = testDir.newFolder();
        File netFile = new File(dir, "testMLN.zip");
        createIrisMLN().save(netFile);
        return netFile;
    }

    public File createIrisCGFile() throws Exception {
        File dir = testDir.newFolder();
        File netFile = new File(dir, "testMLN.zip");
        createIrisMLN().toComputationGraph().save(netFile);
        return netFile;
    }

    public MultiLayerNetwork createIrisMLN(){
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .updater(new Adam(0.01))
                .list()
                .layer(new DenseLayer.Builder().nOut(5).activation(Activation.RELU).build())
                .layer(new OutputLayer.Builder().nOut(3).activation(Activation.SOFTMAX).lossFunction(LossFunctions.LossFunction.MCXENT).build())
                .setInputType(InputType.feedForward(4))
                .build();

        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.init();

        return net;
    }

    public INDArray predictFromFile(File f, INDArray in) throws Exception {
        MultiLayerNetwork net = MultiLayerNetwork.load(f, false);
        return net.output(in);
    }

    public INDArray[] predictFromFileCG(File f, INDArray in) throws Exception {
        ComputationGraph net = ComputationGraph.load(f, false);
        return net.output(in);
    }
}
