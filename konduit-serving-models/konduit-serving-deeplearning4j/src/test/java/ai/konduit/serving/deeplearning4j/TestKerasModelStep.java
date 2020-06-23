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

package ai.konduit.serving.deeplearning4j;

import ai.konduit.serving.models.deeplearning4j.step.keras.KerasModelStep;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;
import ai.konduit.serving.pipeline.api.protocol.URIResolver;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.nd4j.common.resources.Resources;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

@Slf4j
public class TestKerasModelStep {

    @Rule
    public TemporaryFolder testDir = new TemporaryFolder();

    @Test
    public void testSequential() throws IOException {
        File f = Resources.asFile("conv1d_k2_s1_d1_cf_same_model.h5");
        File dir = testDir.newFolder();
        File modelFile = new File(dir, "model.h5");
        FileUtils.copyFile(f, modelFile);;
        String path = modelFile.toURI().toString();



        INDArray arr = Nd4j.rand(DataType.FLOAT, 1, 3, 10);

        for(boolean withNamesDefined : new boolean[]{false, true}) {
            Pipeline p = SequencePipeline.builder()
                    .add(KerasModelStep.builder()
                            .modelUri(path)
                            .inputNames(withNamesDefined ? Collections.singletonList("in") : null)
                            .outputNames(withNamesDefined ? Collections.singletonList("myPrediction") : null)
                            .build())
                    .build();

            PipelineExecutor e = p.executor();
            String outName = withNamesDefined ? "myPrediction" : "default";

            Data d = Data.singleton("in", NDArray.create(arr));

            Data out = e.exec(d);

            String json = p.toJson();
            Pipeline pJson = Pipeline.fromJson(json);
            pJson.executor().exec(d).getNDArray(outName).getAs(INDArray.class);

            String yaml = p.toYaml();
            Pipeline pYaml = Pipeline.fromYaml(yaml);
            pYaml.executor().exec(d).getNDArray(outName).getAs(INDArray.class);
        }
    }


    @Test
    public void testFunctional() throws Exception {
        File f = Resources.asFile("lstm_functional_tf_keras_2.h5");
        File dir = testDir.newFolder();
        File modelFile = new File(dir, "model.h5");
        FileUtils.copyFile(f, modelFile);
        String path = modelFile.toURI().toString();

        INDArray arr = Nd4j.rand(DataType.FLOAT, 1, 10, 4);

        for(boolean withNamesDefined : new boolean[]{false, true}) {
            Pipeline p = SequencePipeline.builder()
                    .add(KerasModelStep.builder()
                            .modelUri(path)
                            .inputNames(withNamesDefined ? Collections.singletonList("in") : null)
                            .outputNames(withNamesDefined ? Collections.singletonList("myPrediction") : null)
                            .build())
                    .build();

            PipelineExecutor e = p.executor();
            String outName = withNamesDefined ? "myPrediction" : "default";

            Data d = Data.singleton("in", NDArray.create(arr));

            Data out = e.exec(d);

            String json = p.toJson();
            Pipeline pJson = Pipeline.fromJson(json);
            pJson.executor().exec(d).getNDArray(outName).getAs(INDArray.class);

            String yaml = p.toYaml();
            Pipeline pYaml = Pipeline.fromYaml(yaml);
            pYaml.executor().exec(d).getNDArray(outName).getAs(INDArray.class);
        }
    }

}
