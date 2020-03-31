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
import ai.konduit.serving.output.types.ClassifierOutput;
import ai.konduit.serving.output.types.NDArrayOutput;
import ai.konduit.serving.pipeline.step.PythonStep;
import ai.konduit.serving.util.ObjectMappers;
import ai.konduit.serving.util.PortUtils;
import ai.konduit.serving.verticles.inference.InferenceVerticle;
import com.jayway.restassured.response.Response;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunnerWithParametersFactory;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.datavec.python.PythonType.TypeName;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.io.ClassPathResource;
import org.nd4j.serde.binary.BinarySerde;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.jayway.restassured.RestAssured.given;
import static javax.swing.RowFilter.ComparisonType.AFTER;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import static org.apache.http.entity.ContentType.MULTIPART_FORM_DATA;
import static org.datavec.python.PythonExecutioner.JAVACPP_PYTHON_APPEND_TYPE;
import static org.junit.Assert.assertArrayEquals;
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
    @NonNull Object expected;
    // ----------------------------------------------------------------------------------

    private static final String PORT_KEY = "port";
    static {
        System.setProperty(JAVACPP_PYTHON_APPEND_TYPE, AFTER.name());
    }

    private Vertx vertx;

    @Parameterized.Parameters(name = " {index} | {0}: {2} -> {3} | {4} ")
    public static List<Object[]> input() throws IOException {
        int testIndex = -1;

        Object[][] permutations = new Object[][] {
                // Tensorflow model types
                // JSON Output
                { "Tensorflow", file("data/7.png"),
                        Input.DataFormat.IMAGE,
                        Output.DataFormat.JSON,
                        PredictionType.CLASSIFICATION,
                        map("img", TypeName.NDARRAY.name()),
                        map("prediction", TypeName.NDARRAY.name()),
                        script("scripts/tensorFlow/image_tensorflow.py"),
                        ClassifierOutput.builder().decisions(new int[] { 7 }).probabilities(new double[][] {{ 0, 0, 0, 0, 0, 0, 0, 1, 0, 0 }}).build()
                },
                { "Tensorflow", file("data/7.png"),
                        Input.DataFormat.IMAGE,
                        Output.DataFormat.JSON,
                        PredictionType.RAW,
                        map("img", TypeName.NDARRAY.name()),
                        map("prediction", TypeName.NDARRAY.name()),
                        script("scripts/tensorFlow/image_tensorflow.py"),
                        Nd4j.create(new double[] { 0, 0, 0, 0, 0, 0, 0, 1, 0, 0 }, 1, 10)
                },
                // ND4J Output
                { "Tensorflow", file("data/7.png"),
                        Input.DataFormat.IMAGE,
                        Output.DataFormat.ND4J,
                        PredictionType.RAW,
                        map("img", TypeName.NDARRAY.name()),
                        map("prediction", TypeName.NDARRAY.name()),
                        script("scripts/tensorFlow/image_tensorflow.py"),
                        Nd4j.create(new double[] { 0, 0, 0, 0, 0, 0, 0, 1, 0, 0 }, 1, 10)
                },
                // Numpy Output
                { "Tensorflow", file("data/7.png"),
                        Input.DataFormat.IMAGE,
                        Output.DataFormat.NUMPY,
                        PredictionType.RAW,
                        map("img", TypeName.NDARRAY.name()),
                        map("prediction", TypeName.NDARRAY.name()),
                        script("scripts/tensorFlow/image_tensorflow.py"),
                        Nd4j.create(new double[] { 0, 0, 0, 0, 0, 0, 0, 1, 0, 0 }, 1, 10)
                }

                // Keras
                // Scikit-learn
                // Deeplearning4j
                // Pytorch
                // Custom (Only Python)
        };

        if(testIndex < 0) {
            return Arrays.asList(permutations);
        } else {
            return Arrays.asList(new Object[][] { permutations[testIndex] });
        }
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

    private static INDArray ndArray(byte[] bytes) {
        return BinarySerde.toArray(ByteBuffer.wrap(bytes));
    }

    private static INDArray numpy(byte[] bytes) {
        return Nd4j.createNpyFromByteArray(bytes);
    }

    @Before
    public void before(TestContext testContext) throws Exception {
        this.vertx = Vertx.vertx(new VertxOptions()
                .setMaxEventLoopExecuteTime(60)
                .setMaxEventLoopExecuteTimeUnit(TimeUnit.SECONDS)
        ).exceptionHandler(testContext::fail);

        testContext.put(PORT_KEY, PortUtils.getAvailablePort());

        InferenceConfiguration inferenceConfiguration = InferenceConfiguration.builder()
                .servingConfig(ServingConfig.builder()
                        .outputDataFormat(outputDataFormat)
                        .httpPort(testContext.get(PORT_KEY))
                        .build())
                .steps(Collections.singletonList(
                        new PythonStep(PythonConfig.builder()
                                .pythonCode(script)
                                .pythonPath(PythonPathUtils.getPythonPath())
                                .pythonInputs(inputs)
                                .pythonOutputs(outputs)
                                .build())
                        )
                ).build();

        vertx.deployVerticle(InferenceVerticle.class,
                new DeploymentOptions().setConfig(inferenceConfiguration.toJsonObject()),
                testContext.asyncAssertSuccess());
    }

    @Test(timeout = 60000)
    public void test(TestContext testContext) {
        Response response = given().port(testContext.get(PORT_KEY))
                .header("Content-Type", mime())
                .multiPart("default", (File) data)
                .expect().statusCode(200)
                .post(url()).then()
                .extract()
                .response();

        switch(outputDataFormat) {
            case JSON:
                String output = new JsonObject(String.valueOf(response.asString())).getJsonObject("default").toString();

                switch (predictionType) {
                    case CLASSIFICATION:
                        ClassifierOutput classifierOutput = ObjectMappers.fromJson(output, ClassifierOutput.class);
                        ClassifierOutput expectedClassifierOutput = (ClassifierOutput) expected;

                        assertArrayEquals(expectedClassifierOutput.getDecisions(), classifierOutput.getDecisions());
                        assertArrayEquals(expectedClassifierOutput.getProbabilities(), classifierOutput.getProbabilities());
                        break;
                    case RAW:
                        NDArrayOutput ndArrayOutput = ObjectMappers.fromJson(output, NDArrayOutput.class);

                        assertEquals(expected, ndArrayOutput.getNdArray());
                        break;
                }
                break;
            case ND4J:
                assertEquals(expected, ndArray(response.asByteArray()));
                break;
            case NUMPY:
                assertEquals(expected, numpy(response.asByteArray()));
                break;
        }
    }

    @After
    public void after(TestContext testContext) {
        vertx.close(testContext.asyncAssertSuccess());
    }
}
