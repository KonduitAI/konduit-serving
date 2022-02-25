/*
 *  ******************************************************************************
 *  * Copyright (c) 2022 Konduit K.K.
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

package ai.konduit.serving.models.samediff;

import ai.konduit.serving.models.samediff.step.SameDiffStep;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.nd4j.autodiff.samediff.SDVariable;
import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.io.File;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class TestSameDiffServing {

    @Rule
    public TemporaryFolder testDir = new TemporaryFolder();

    @Test
    public void testSameDiff() throws Exception {

        Nd4j.getRandom().setSeed(12345);
        INDArray inArr = Nd4j.rand(DataType.FLOAT, 3, 784);

        SameDiff sd = getModel();

        INDArray outExp = sd.outputSingle(Collections.singletonMap("in", inArr), "out");

        File dir = testDir.newFolder();
        File f = new File(dir, "samediff.bin");
        sd.save(f, false);

        Pipeline p = SequencePipeline.builder()
                .add(SameDiffStep.builder()
                        .modelUri(f.toURI().toString())
                        .outputNames(Collections.singletonList("out"))
                        .build())
                .build();

        PipelineExecutor exec = p.executor();

        Data d = Data.singleton("in", NDArray.create(inArr));

        Data dOut = exec.exec(d);

        INDArray outArr = dOut.getNDArray("out").getAs(INDArray.class);
        assertEquals(outExp, outArr);

        String json = p.toJson();
        Pipeline p2 = Pipeline.fromJson(json);
        Data dOut2 = p2.executor().exec(d);
        INDArray outArr2 = dOut2.getNDArray("out").getAs(INDArray.class);
        assertEquals(outExp, outArr2);
    }

    public static SameDiff getModel(){
        Nd4j.getRandom().setSeed(12345);
        SameDiff sd = SameDiff.create();
        SDVariable in = sd.placeHolder("in", DataType.FLOAT, -1, 784);

        SDVariable w1 = sd.var("w1", Nd4j.rand(DataType.FLOAT, 784, 100));
        SDVariable b1 = sd.var("b1", Nd4j.rand(DataType.FLOAT, 100));
        SDVariable a1 = sd.nn.tanh(in.mmul(w1).add(b1));

        SDVariable w2 = sd.var("w2", Nd4j.rand(DataType.FLOAT, 100, 10));
        SDVariable b2 = sd.var("b2", Nd4j.rand(DataType.FLOAT, 10));
        SDVariable out = sd.nn.softmax("out", a1.mmul(w2).add(b2));
        return sd;
    }

}
