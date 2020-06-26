/* ******************************************************************************
 * Copyright (c) 2020 Konduit K.K.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/
package ai.konduit.serving.deeplearning4j;

import ai.konduit.serving.models.deeplearning4j.step.DL4JModelPipelineStep;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;
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
                    .add(new DL4JModelPipelineStep()
                            .modelUri(netFile.getAbsolutePath())
                            .inputNames(withNamesDefined ? Collections.singletonList("in") : null)
                            .outputNames(withNamesDefined ? Collections.singletonList("myPrediction") : null)
                            )
                    .build();


            PipelineExecutor e = p.executor();
            String outName = withNamesDefined ? "myPrediction" : "default";


            INDArray arr = Nd4j.rand(DataType.FLOAT, 3, 4);
            INDArray exp = predictFromFile(netFile, arr);

            Data d = Data.singleton("in", NDArray.create(arr));

            Data out = e.exec(d);
            INDArray actual = out.getNDArray(outName).getAs(INDArray.class);

            assertEquals(exp, actual);

            String json = p.toJson();
            System.out.println(json);
            Pipeline pJson = Pipeline.fromJson(json);
            INDArray outJson = pJson.executor().exec(d).getNDArray(outName).getAs(INDArray.class);
            assertEquals(exp, outJson);

            String yaml = p.toYaml();
            System.out.println(yaml);
            Pipeline pYaml = Pipeline.fromYaml(yaml);
            INDArray outYaml = pYaml.executor().exec(d).getNDArray(outName).getAs(INDArray.class);
            assertEquals(exp, outYaml);
        }
    }

    @Test
    public void testSimpleCompGraph() throws Exception {

        for(boolean withNamesDefined : new boolean[]{false, true}) {
            File netFile = createIrisCGFile();

            Pipeline p = SequencePipeline.builder()
                    .add(new DL4JModelPipelineStep()
                            .modelUri(netFile.toURI().toString())
                            .inputNames(withNamesDefined ? Collections.singletonList("in") : null)
                            .outputNames(withNamesDefined ? Collections.singletonList("myPrediction") : null)
                            )
                    .build();

            PipelineExecutor e = p.executor();
            String outName = withNamesDefined ? "myPrediction" : "default";


            INDArray arr = Nd4j.rand(DataType.FLOAT, 3, 4);
            INDArray exp = predictFromFileCG(netFile, arr)[0];

            Data d = Data.singleton("in", NDArray.create(arr));

            Data out = e.exec(d);
            INDArray actual = out.getNDArray(outName).getAs(INDArray.class);

            assertEquals(exp, actual);


            String json = p.toJson();
            Pipeline pJson = Pipeline.fromJson(json);
            INDArray outJson = pJson.executor().exec(d).getNDArray(outName).getAs(INDArray.class);
            assertEquals(exp, outJson);

            String yaml = p.toYaml();
            Pipeline pYaml = Pipeline.fromYaml(yaml);
            INDArray outYaml = pYaml.executor().exec(d).getNDArray(outName).getAs(INDArray.class);
            assertEquals(exp, outYaml);
        }
    }

    public File createIrisMLNFile() throws Exception {
        File dir = testDir.newFolder();
        return createIrisMLNFile(dir);
    }

    public static File createIrisMLNFile(File dir) throws Exception {
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

    public static MultiLayerNetwork createIrisMLN(){
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
