/*
 * *****************************************************************************
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
 * ****************************************************************************
 */

package ai.konduit.serving.verticles.python;

import ai.konduit.serving.InferenceConfiguration;
import ai.konduit.serving.config.Input;
import ai.konduit.serving.config.Output;
import ai.konduit.serving.config.Output.PredictionType;
import ai.konduit.serving.config.ServingConfig;
import ai.konduit.serving.miscutils.PythonPathUtils;
import ai.konduit.serving.model.PythonConfig;
import ai.konduit.serving.pipeline.PipelineStep;
import ai.konduit.serving.pipeline.step.ImageLoadingStep;
import ai.konduit.serving.pipeline.step.PythonStep;
import ai.konduit.serving.util.PortUtils;
import ai.konduit.serving.verticles.inference.InferenceVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunnerWithParametersFactory;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Sets;
import org.datavec.python.PythonType.TypeName;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.nd4j.linalg.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.jayway.restassured.RestAssured.given;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import static org.apache.http.entity.ContentType.MULTIPART_FORM_DATA;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(VertxUnitRunnerWithParametersFactory.class)
@RequiredArgsConstructor
public class PythonStepPermutationsTest {

    // -------------------------------- TEST PARAMETERS ---------------------------------
    @NonNull String modelType;

    @NonNull Object data;
    @NonNull Input.DataFormat inputDataFormat;
    @NonNull Output.DataFormat outputDataFormat;
    @NonNull Output.PredictionType predictionType;
    @NonNull Map<String, String> inputs;
    @NonNull Map<String, String> outputs;
    @NonNull String script;
    // ----------------------------------------------------------------------------------

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private Vertx vertx;

    @Parameterized.Parameters(name = " {index} | {0}: {2} -> {3} | {4} ")
    public static Set<Object []> input() throws IOException {
        return Sets.newLinkedHashSet(new Object[][] {
                // Tensorflow model types
                { "Tensorflow", file("data/TensorFlowImageTest.png"),
                        Input.DataFormat.IMAGE,
                        Output.DataFormat.JSON,
                        PredictionType.RAW,
                        map("img", TypeName.NDARRAY.name()),
                        map("prediction", TypeName.NDARRAY.name()),
                        script("scripts/tensorFlow/TensorFlowImageTest.py")
                }

                // Keras
                // Scikit-learn
                // Deeplearning4j
                // Pytorch
                // Custom (Only Python)
        });
    }

    private static File file(String path) throws IOException {
        return new ClassPathResource(path).getFile();
    }

    private static String script(String path) throws IOException {
        return FileUtils.readFileToString(new ClassPathResource(path).getFile(), StandardCharsets.UTF_8);
    }

    private String mime() {
        if(data instanceof File) {
            return MULTIPART_FORM_DATA.getMimeType();
        } else {
            return APPLICATION_JSON.getMimeType();
        }
    }

    private String url() {
        return String.format("%s/%s", predictionType.name(), inputDataFormat.name());
    }

    private static Map<String, String> map(String... values) {
        if(values.length % 2 == 0) {
            int numberOfElements = values.length / 2;
            List<String> inputNames = IntStream.range(0, numberOfElements).mapToObj(i -> values[i * 2]).collect(Collectors.toList());
            if(new HashSet<>(inputNames).size() != numberOfElements) {
                throw new IllegalArgumentException(String.format("The map keys %s have duplicate values.", inputNames.toString()));
            }

            return IntStream.range(0, numberOfElements).boxed().collect(Collectors.toMap(i -> values[i * 2], i -> values[i * 2 + 1]));
        } else {
            throw new IllegalArgumentException("Values to be mapped should have an even number of elements");
        }
    }

    @Before
    public void before() {
        this.vertx = Vertx.vertx(new VertxOptions()
                .setMaxEventLoopExecuteTime(60)
                .setMaxEventLoopExecuteTimeUnit(TimeUnit.SECONDS)
        );
    }

    @Test(timeout = 60000)
    public void test(TestContext testContext) throws Exception {
        int port = PortUtils.getAvailablePort();

        List<PipelineStep> steps = new ArrayList<>();

        if(inputDataFormat.equals(Input.DataFormat.IMAGE)) {
            steps.add(ImageLoadingStep.builder()
                    .inputNames(new ArrayList<>(inputs.keySet()))
                    .build());
        }

        steps.add(new PythonStep(PythonConfig.builder()
                .pythonCode(script)
                .pythonPath(PythonPathUtils.getPythonPath())
                .pythonInputs(inputs)
                .pythonOutputs(outputs)
                .build()));

        InferenceConfiguration inferenceConfiguration = InferenceConfiguration.builder()
                .servingConfig(ServingConfig.builder().outputDataFormat(outputDataFormat).httpPort(port).build())
                .steps(steps)
                .build();

        Async async = testContext.async();

        vertx.deployVerticle(InferenceVerticle.class, new DeploymentOptions().setConfig(inferenceConfiguration.toJsonObject()), handler -> {
            if(handler.succeeded()) {
                try {
                    String output = given().port(port)
                            .header("Content-Type", mime())
                            .multiPart("img", (File) data)
                            .expect().statusCode(200)
                            .post(url()).then()
                            .extract()
                            .body().asString();

                    JsonObject jsonObject1 = new JsonObject(output);
                    JsonObject ndarraySerde = jsonObject1.getJsonObject("default");
                    JsonArray probabilities = ndarraySerde.getJsonArray("probabilities");
                    double outpuValue = probabilities.getJsonArray(0).getDouble(0);
                    assertEquals(7, outpuValue, 1e-1);

                    async.complete();
                } catch (Exception e) {
                    testContext.fail(e);
                }
            } else {
                testContext.fail();
            }
        });
    }

    @After
    public void after(TestContext testContext) {
        vertx.close(testContext.asyncAssertSuccess());
    }
}
