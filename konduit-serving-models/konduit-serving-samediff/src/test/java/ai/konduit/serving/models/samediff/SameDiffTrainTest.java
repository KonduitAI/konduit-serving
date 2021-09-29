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
package ai.konduit.serving.models.samediff;

import ai.konduit.serving.models.samediff.step.trainer.SameDiffTrainerStep;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.nd4j.autodiff.loss.LossReduce;
import org.nd4j.autodiff.samediff.SDVariable;
import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.common.config.ND4JClassLoading;
import org.nd4j.common.io.ClassPathResource;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;

import java.io.File;
import java.util.Arrays;

import static org.junit.Assert.assertTrue;

public class SameDiffTrainTest {


    @Rule
    public TemporaryFolder testDir = new TemporaryFolder();

    @Test
    public void testSamediffFit() throws Exception {
        SameDiff sameDiff = getModel();
        ND4JClassLoading.setNd4jClassloader(Thread.currentThread().getContextClassLoader());
        File testFile = new File(testDir.getRoot(),"testmodel.fb");
        for(String s : new String[]{"functions.properties","onnx.pbtxt","onnxops.json","op-ir.proto","ops.proto","nd4j-op-def.pbtxt"}) {
            ClassPathResource classPathResource = new ClassPathResource(s);
            assertTrue("Resource " + s + " does not exist!",classPathResource.exists());
        }


        sameDiff.save(testFile,true);
        assertTrue(testFile.exists());
        File outputFile = new File(testDir.getRoot(),"testmodel2.fb");
        SameDiffTrainerStep sameDiffTrainerStep = new SameDiffTrainerStep()
                .modelUri(testFile.getAbsolutePath())
                .updater(new Adam(1e-3))
                .lossVariables(Arrays.asList("loss"))
                .labels(Arrays.asList("labels"))
                .initialLossType(DataType.DOUBLE)
                .modelSaveOutputPath(outputFile.getAbsolutePath())
                .numEpochs(1);

        String json = sameDiffTrainerStep.toJson();

        Pipeline pipeline = SequencePipeline.builder()
                .add(sameDiffTrainerStep).build();
        PipelineExecutor executor = pipeline.executor();
        Data data = Data.empty();
        data.put("in", NDArray.create(Nd4j.ones(DataType.DOUBLE,1,4)));
        data.put("labels",NDArray.create(Nd4j.ones(DataType.DOUBLE,1,3)));

        for(int i = 0; i < 5; i++) {
            executor.exec(data);
        }

        assertTrue(outputFile.exists());



    }

    public static SameDiff getModel(){
        Nd4j.getRandom().setSeed(12345);
        SameDiff sd = SameDiff.create();
        SDVariable in = sd.placeHolder("in", DataType.DOUBLE, -1, 4);
        SDVariable label = sd.placeHolder("labels", DataType.DOUBLE, -1, 3);
        SDVariable w = sd.var("w", Nd4j.rand(DataType.DOUBLE, 4, 3));
        SDVariable z = in.mmul(w);
        SDVariable out = sd.nn.softmax("softmax", z);
        SDVariable loss = sd.loss.logLoss("loss", label, out);
        return sd;
    }

}
