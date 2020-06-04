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

package ai.konduit.serving.build;

import ai.konduit.serving.build.cli.BuildCLI;
import ai.konduit.serving.models.deeplearning4j.step.DL4JModelPipelineStep;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;

@Ignore //To be run manually, not as part of CI
public class TestCLI {

    @Rule
    public TemporaryFolder testDir = new TemporaryFolder();

    @Test
    public void test() throws Exception {

        Pipeline p = SequencePipeline.builder()
                .add(new DL4JModelPipelineStep("file:///some/model/file.bin", null, null))
                .build();

        File dir = testDir.newFolder();
        File f = new File(dir, "pipeline.json");
        FileUtils.writeStringToFile(f, p.toJson(), StandardCharsets.UTF_8);

        BuildCLI.main(
                "--os", "windows",
                "--arch", "x86_avx2",
                "-p", f.getAbsolutePath()
        );

    }

}
