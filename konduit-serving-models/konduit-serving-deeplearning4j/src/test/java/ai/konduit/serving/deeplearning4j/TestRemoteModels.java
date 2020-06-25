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

import ai.konduit.serving.common.test.BaseHttpUriTest;
import ai.konduit.serving.models.deeplearning4j.step.DL4JModelStep;
import ai.konduit.serving.models.deeplearning4j.step.DL4JRunner;
import ai.konduit.serving.models.deeplearning4j.step.keras.KerasModelStep;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;
import ai.konduit.serving.pipeline.api.protocol.URIResolver;
import ai.konduit.serving.pipeline.api.protocol.handlers.KSStreamHandlerFactory;
import ai.konduit.serving.pipeline.impl.context.DefaultContext;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.nd4j.common.resources.Resources;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.io.File;
import java.net.URLStreamHandlerFactory;
import java.util.Collections;

public class TestRemoteModels extends BaseHttpUriTest {
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
    public void testDL4JRemoteURI() throws Exception {
        File dir = new File(httpDir, "tests");
        dir.mkdirs();
        File f = TestDL4JModelStep.createIrisMLNFile(dir);
        String filename = "tests/" + f.getName();

        String uri = uriFor(filename);

        Pipeline p = SequencePipeline.builder()
                .add(DL4JModelStep.builder()
                        .modelUri(uri)
                        .build())
                .build();

        PipelineExecutor e = p.executor();
        INDArray arr = Nd4j.rand(DataType.FLOAT, 3, 4);
        Data d = Data.singleton("in", NDArray.create(arr));
        Data out = e.exec(d);
        System.out.println(out.toJson());
    }

    @Test
    public void testDL4JStepRunner() throws Exception {
        String filename = "lstm_functional_tf_keras_2.h5";
        String relativePath = "tests/" + filename;
        FileUtils.copyFile(Resources.asFile(filename), new File(httpDir, relativePath));
        String uri = uriFor(relativePath);

        KerasModelStep step = KerasModelStep.builder()
                .modelUri(uri)
                .inputNames(Collections.singletonList("in"))
                .outputNames(Collections.singletonList("myPrediction"))
                .build();

        DL4JRunner runner = new DL4JRunner(step);

        INDArray arr = Nd4j.rand(DataType.FLOAT, 3, 4, 4);
        Data data = Data.singleton("in", NDArray.create(arr));

        runner.exec(new DefaultContext(null,null), data);
    }
}
