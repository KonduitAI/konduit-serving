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
import ai.konduit.serving.build.config.Target;
import ai.konduit.serving.build.dependencies.Dependency;
import ai.konduit.serving.build.deployments.UberJarDeployment;
import ai.konduit.serving.build.steps.RunnerInfo;
import ai.konduit.serving.build.util.ModuleUtils;
import ai.konduit.serving.models.deeplearning4j.step.DL4JModelPipelineStep;
import ai.konduit.serving.models.samediff.step.SameDiffModelPipelineStep;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import ai.konduit.serving.pipeline.impl.pipeline.graph.GraphBuilder;
import ai.konduit.serving.pipeline.impl.pipeline.graph.GraphStep;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class TestResolving {

    @Rule
    public TemporaryFolder testDir = new TemporaryFolder();

    @Test
    public void testRunnerInfoLoading(){
        Map<String, RunnerInfo> runners = ModuleUtils.pipelineClassToRunnerClass();
        assertTrue(runners.containsKey("ai.konduit.serving.data.image.step.ndarray.ImageToNDArrayStep"));
        assertEquals("konduit-serving-image", runners.get("ai.konduit.serving.data.image.step.ndarray.ImageToNDArrayStep").module().name());
        assertEquals("konduit-serving-deeplearning4j", runners.get("ai.konduit.serving.models.deeplearning4j.step.DL4JModelPipelineStep").module().name());
    }

    @Test
    public void testJsonRunnerMappingLoading(){
        Map<String, List<RunnerInfo>> runners = ModuleUtils.jsonNameToRunnerClass();
        assertTrue(runners.containsKey("DEEPLEARNING4J"));
        assertEquals(1, runners.get("DEEPLEARNING4J").size());
        RunnerInfo ri = runners.get("DEEPLEARNING4J").get(0);
        assertEquals("konduit-serving-deeplearning4j", ri.module().name());
        assertTrue(runners.containsKey("IMAGE_TO_NDARRAY"));
    }

    @Test
    public void testResolvingDL4JSameDiff() throws Exception {
        //Specify the target - (OS + arch + device) and work out what dependencies we should include

        for(boolean graph : new boolean[]{false, true}) {
            for (int testNum = 0; testNum <= 1; testNum++) {
                for (Target t : new Target[]{Target.LINUX_X86, Target.LINUX_X86_AVX2, Target.LINUX_X86_AVX512, Target.WINDOWS_X86, Target.WINDOWS_X86_AVX2,
                        Target.LINUX_CUDA_10_2, Target.LINUX_CUDA_10_1, Target.LINUX_CUDA_10_0, Target.WINDOWS_CUDA_10_2, Target.WINDOWS_CUDA_10_1, Target.WINDOWS_CUDA_10_0}) {

                    PipelineStep step;
                    if (testNum == 0) {
                        step = new DL4JModelPipelineStep("file:///some/model/path.zip", null, null);
                        System.out.println("----- DL4J - " + t + " -----");
                    } else {
                        step = new SameDiffModelPipelineStep("file://some/model/path.fb", null);
                        //System.out.println("----- SameDiff - " + t + " -----");
                    }

//                    Pipeline p = SequencePipeline.builder()
//                            .add(step)
//                            .build();
                    Pipeline p;
                    if(graph){
                        GraphBuilder gb = new GraphBuilder();
                        GraphStep input = gb.input();
                        p = gb.build(input.then("step", step));
                    } else {
                        p = SequencePipeline.builder()
                                .add(step)
                                .build();
                    }

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
                            .deployments(new UberJarDeployment().outputDir("/my/output/dir").jarName("my.jar"));


                    List<Dependency> resolvedDeps = c.resolveDependencies();
                    if (testNum == 0) {
                        for (Dependency d : resolvedDeps) {
                            System.out.println(d);
                        }
                    }

                    String ksVersion = "0.1.0-SNAPSHOT";
                    List<Dependency> expectedDeps = new ArrayList<>();
                    expectedDeps.add(new Dependency("ai.konduit.serving", "konduit-serving-pipeline", ksVersion));
                    expectedDeps.add(new Dependency("ai.konduit.serving", "konduit-serving-vertx", ksVersion));
                    expectedDeps.add(new Dependency("ai.konduit.serving", "konduit-serving-cli", ksVersion));
                    expectedDeps.add(new Dependency("ai.konduit.serving", "konduit-serving-http", ksVersion));
                    expectedDeps.add(new Dependency("ai.konduit.serving", "konduit-serving-grpc", ksVersion));
                    if (testNum == 0) {
                        //DL4J
                        expectedDeps.add(new Dependency("ai.konduit.serving", "konduit-serving-deeplearning4j", ksVersion));
                    } else {
                        //SameDiff
                        expectedDeps.add(new Dependency("ai.konduit.serving", "konduit-serving-samediff", ksVersion));
                    }

                    String ossVer = "1.0.0-beta7";
                    if (t.equals(Target.LINUX_X86)) {
                        expectedDeps.add(new Dependency("org.nd4j", "nd4j-native", ossVer));
                        expectedDeps.add(new Dependency("org.nd4j", "nd4j-native", ossVer, "linux-x86_64"));
                    } else if (t.equals(Target.LINUX_X86_AVX2)) {
                        expectedDeps.add(new Dependency("org.nd4j", "nd4j-native", ossVer));
                        expectedDeps.add(new Dependency("org.nd4j", "nd4j-native", ossVer, "linux-x86_64-avx2"));
                    } else if (t.equals(Target.LINUX_X86_AVX512)) {
                        expectedDeps.add(new Dependency("org.nd4j", "nd4j-native", ossVer));
                        expectedDeps.add(new Dependency("org.nd4j", "nd4j-native", ossVer, "linux-x86_64-avx512"));
                    } else if (t.equals(Target.WINDOWS_X86)) {
                        expectedDeps.add(new Dependency("org.nd4j", "nd4j-native", ossVer));
                        expectedDeps.add(new Dependency("org.nd4j", "nd4j-native", ossVer, "windows-x86_64"));
                    } else if (t.equals(Target.WINDOWS_X86_AVX2)) {
                        expectedDeps.add(new Dependency("org.nd4j", "nd4j-native", ossVer));
                        expectedDeps.add(new Dependency("org.nd4j", "nd4j-native", ossVer, "windows-x86_64-avx2"));
                    } else if (t.equals(Target.LINUX_CUDA_10_2)) {
                        expectedDeps.add(new Dependency("org.nd4j", "nd4j-cuda-10.2", ossVer));
                        expectedDeps.add(new Dependency("org.nd4j", "nd4j-cuda-10.2", ossVer, "linux-x86_64"));
                    } else if (t.equals(Target.LINUX_CUDA_10_1)) {
                        expectedDeps.add(new Dependency("org.nd4j", "nd4j-cuda-10.1", ossVer));
                        expectedDeps.add(new Dependency("org.nd4j", "nd4j-cuda-10.1", ossVer, "linux-x86_64"));
                    } else if (t.equals(Target.LINUX_CUDA_10_0)) {
                        expectedDeps.add(new Dependency("org.nd4j", "nd4j-cuda-10.0", ossVer));
                        expectedDeps.add(new Dependency("org.nd4j", "nd4j-cuda-10.0", ossVer, "linux-x86_64"));
                    } else if (t.equals(Target.WINDOWS_CUDA_10_2)) {
                        expectedDeps.add(new Dependency("org.nd4j", "nd4j-cuda-10.2", ossVer));
                        expectedDeps.add(new Dependency("org.nd4j", "nd4j-cuda-10.2", ossVer, "windows-x86_64"));
                    } else if (t.equals(Target.WINDOWS_CUDA_10_1)) {
                        expectedDeps.add(new Dependency("org.nd4j", "nd4j-cuda-10.1", ossVer));
                        expectedDeps.add(new Dependency("org.nd4j", "nd4j-cuda-10.1", ossVer, "windows-x86_64"));
                    } else if (t.equals(Target.WINDOWS_CUDA_10_0)) {
                        expectedDeps.add(new Dependency("org.nd4j", "nd4j-cuda-10.0", ossVer));
                        expectedDeps.add(new Dependency("org.nd4j", "nd4j-cuda-10.0", ossVer, "windows-x86_64"));
                    } else {
                        throw new UnsupportedOperationException(t.toString());
                    }

                    assertEquals(expectedDeps, resolvedDeps);

                    if (testNum == 0) {
                        System.out.println();
                    }
                }
            }
        }
    }

}
