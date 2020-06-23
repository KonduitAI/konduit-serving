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

import ai.konduit.serving.models.deeplearning4j.step.DL4JPipelineStepRunner;
import ai.konduit.serving.models.deeplearning4j.step.keras.KerasModelStep;
import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.impl.context.DefaultContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.Collections;

public class TestStepRunners {

    private ai.konduitai.serving.common.test.TestServer testServer;

    @Before
    public void setUp() throws Exception {
        testServer = new ai.konduitai.serving.common.test.TestServer("http://", "localhost", 9090);
        testServer.start();
    }

    @After
    public void tearDown() throws Exception {
        testServer.stop();
    }

    @Rule
    public TemporaryFolder testDir = new TemporaryFolder();

    @Test
    public void testDL4JStepRunner() throws Exception {
        String path = "http://localhost:9090/src/test/resources/lstm_functional_tf_keras_2.h5";

        KerasModelStep step = KerasModelStep.builder()
                .modelUri(path)
                .inputNames(Collections.singletonList("in"))
                .outputNames(Collections.singletonList("myPrediction"))
                .build();

        DL4JPipelineStepRunner runner = new DL4JPipelineStepRunner(step);

        INDArray arr = Nd4j.rand(DataType.FLOAT, 3, 4, 4);
        Data data = Data.singleton("in", NDArray.create(arr));

        runner.exec(new DefaultContext(null,null), data);
    }
}
