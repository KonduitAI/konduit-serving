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

import ai.konduit.serving.build.config.*;
import ai.konduit.serving.build.dependencies.Dependency;
import ai.konduit.serving.build.deployments.UberJarDeployment;
import ai.konduit.serving.models.deeplearning4j.DL4JConfiguration;
import ai.konduit.serving.models.deeplearning4j.step.DL4JModelPipelineStep;
import ai.konduit.serving.models.samediff.SameDiffConfig;
import ai.konduit.serving.models.samediff.step.SameDiffModelPipelineStep;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class TestResolving {

    @Rule
    public TemporaryFolder testDir = new TemporaryFolder();

    @Test
    public void testBasicResolving() throws Exception {

        for(int testNum=0; testNum <=1; testNum++ ) {
            for(Target t : new Target[]{Target.LINUX_X86, Target.LINUX_X86_AVX2, Target.WINDOWS_X86}) {

                PipelineStep step;
                if (testNum == 0) {
                    step = new DL4JModelPipelineStep("file:///some/model/path.zip", new DL4JConfiguration());
                    System.out.println("----- DL4J - " + t + " -----");
                } else {
                    step = new SameDiffModelPipelineStep("file://some/model/path.fb", new SameDiffConfig());
                    System.out.println("----- SameDiff - " + t + " -----");
                }

                Pipeline p = SequencePipeline.builder()
                        .add(step)
                        .build();

                File dir = testDir.newFolder();
                File jsonF = new File(dir, "pipeline.json");
                FileUtils.writeStringToFile(jsonF, p.toJson(), StandardCharsets.UTF_8);


                Config c = new Config()
                        .pipelinePath(jsonF.getAbsolutePath())
                        .ksVersion("LATEST")
                        .metadata(new Metadata()
                                .author("User Name")
                                .buildVersion("1.0.0")
                                .timestamp("2020/05/26 12:00:00"))
                        .target(t)
                        .serving(Serving.HTTP, Serving.GRPC)
                        .modules(Module.PIPELINE)
                        .deployments(new UberJarDeployment().outputDir("/my/output/dir").jarName("my.jar"));


                List<Dependency> deps = c.resolveDependencies();
                for (Dependency d : deps) {
                    System.out.println(d);
                }

                System.out.println("\n\n");
            }
        }

    }

}
