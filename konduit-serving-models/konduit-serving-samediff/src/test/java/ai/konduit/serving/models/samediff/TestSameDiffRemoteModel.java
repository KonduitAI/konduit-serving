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

import ai.konduit.serving.common.test.BaseHttpUriTest;
import ai.konduit.serving.common.test.TestServer;
import ai.konduit.serving.models.samediff.step.SameDiffStep;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;
import ai.konduit.serving.pipeline.api.protocol.URIResolver;
import ai.konduit.serving.pipeline.api.protocol.handlers.KSStreamHandlerFactory;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import org.junit.Before;
import org.junit.Test;
import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.io.File;
import java.net.URLStreamHandlerFactory;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class TestSameDiffRemoteModel extends BaseHttpUriTest {

    private TestServer testServer;

    @Override
    public URLStreamHandlerFactory streamHandler() {
        return new KSStreamHandlerFactory();
    }

    @Before
    public void before(){
        //Needed until we proprely implement the full path, not just the filename, for the cache
        URIResolver.clearCache();
    }

    @Test
    public void testRemote(){
        String filename = "tests/samediff_model.fb";
        SameDiff sd = TestSameDiffServing.getModel();
        new File(httpDir, "tests").mkdirs();
        File f = new File(httpDir, filename);
        sd.save(f, true);
        String uri = uriFor(filename);


        INDArray inArr = Nd4j.rand(DataType.FLOAT, 3, 784);
        INDArray outExp = sd.outputSingle(Collections.singletonMap("in", inArr), "out");

        Pipeline p = SequencePipeline.builder()
                .add(SameDiffStep.builder()
                        .modelUri(uri)
                        .outputNames(Collections.singletonList("out"))
                        .build())
                .build();

        PipelineExecutor exec = p.executor();
        Data d = Data.singleton("in", NDArray.create(inArr));
        Data dOut = exec.exec(d);
        INDArray outArr = dOut.getNDArray("out").getAs(INDArray.class);
        assertEquals(outExp, outArr);
    }
}
