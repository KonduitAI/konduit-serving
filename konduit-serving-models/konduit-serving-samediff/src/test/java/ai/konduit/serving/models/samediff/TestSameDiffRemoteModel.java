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

import ai.konduit.serving.models.samediff.step.SameDiffModelPipelineStep;
import ai.konduit.serving.models.samediff.step.SameDiffPipelineStepRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.Collections;

public class TestSameDiffRemoteModel {

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
    public void testSDStepRunner() throws Exception {
        String path = "http://localhost:9090/src/test/resources/models/frozen_model.pb";

        SameDiffModelPipelineStep step = SameDiffModelPipelineStep.builder()
                .modelUri(path)
                .outputNames(Collections.singletonList("myPrediction"))
                .build();

        SameDiffPipelineStepRunner runner = new SameDiffPipelineStepRunner(step);
    }
}
