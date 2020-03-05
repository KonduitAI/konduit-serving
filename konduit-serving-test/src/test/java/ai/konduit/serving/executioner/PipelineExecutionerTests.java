/*
 *
 *  * ******************************************************************************
 *  *  * Copyright (c) 2015-2019 Skymind Inc.
 *  *  * Copyright (c) 2019 Konduit AI.
 *  *  *
 *  *  * This program and the accompanying materials are made available under the
 *  *  * terms of the Apache License, Version 2.0 which is available at
 *  *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  *  * License for the specific language governing permissions and limitations
 *  *  * under the License.
 *  *  *
 *  *  * SPDX-License-Identifier: Apache-2.0
 *  *  *****************************************************************************
 *
 *
 */

package ai.konduit.serving.executioner;

import ai.konduit.serving.InferenceConfiguration;
import ai.konduit.serving.config.*;
import ai.konduit.serving.model.*;
import ai.konduit.serving.model.PythonConfig.PythonConfigBuilder;
import ai.konduit.serving.pipeline.config.ObjectDetectionConfig;
import ai.konduit.serving.pipeline.step.ImageLoadingStep;
import ai.konduit.serving.pipeline.step.ModelStep;
import ai.konduit.serving.pipeline.step.PythonStep;
import ai.konduit.serving.util.PortUtils;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import junit.framework.TestCase;
import org.datavec.python.PythonType;
import org.junit.Ignore;
import org.junit.Test;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.io.ClassPathResource;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class PipelineExecutionerTests {


    @Test
    @Ignore
    public void testDoJsonInference() {
        int port = PortUtils.getAvailablePort();

        ServingConfig servingConfig = ServingConfig.builder()
                .httpPort(port)
                .build();

        JsonObject jsonSchema = new JsonObject();
        JsonObject schemaValues = new JsonObject();
        JsonObject wrapper = new JsonObject();
        SchemaType[] values = SchemaType.values();

        List<String> fieldNames = Arrays.stream(values)
                .filter(input -> input == SchemaType.Boolean)
                .map(Enum::name)
                .collect(Collectors.toList());
        Map<String,PythonConfig> namesToPythonConfig = new LinkedHashMap<>();
        PythonConfigBuilder pythonConfig = PythonConfig.builder();

        for (SchemaType value : values) {
            if(value == SchemaType.Boolean)
                continue;
            JsonObject fieldInfo = new JsonObject();
            JsonObject topLevel = new JsonObject();
            fieldInfo.put("type",value.name());
            topLevel.put("fieldInfo",fieldInfo);
            jsonSchema.put(value.name(), topLevel);
            switch (value) {
                case NDArray:
                    pythonConfig.pythonInput(value.name(), PythonType.TypeName.NDARRAY.name());
                    fieldInfo.put("shape",new JsonArray().add(1).add(1));
                    schemaValues.put(value.name(), Nd4j.toNpyByteArray(Nd4j.scalar(1.0)));
                    break;
               //need to wait till dl4j can support the boolean type in the python executioner
                    /* case Boolean:
                    pythonConfig.pythonInput(value.name(),Type.BOOL.name());
                    schemaValues.put(value.name(), true);
                    break;*/
                case Float:
                    pythonConfig.pythonInput(value.name(),PythonType.TypeName.FLOAT.name());
                    schemaValues.put(value.name(), 1.0f);
                    break;
                case Double:
                    pythonConfig.pythonInput(value.name(),PythonType.TypeName.FLOAT.name());
                    schemaValues.put(value.name(), 1.0);
                    break;
                case Image:
                    schemaValues.put(value.name(), new byte[]{0, 1});
                    pythonConfig.pythonInput(value.name(),PythonType.TypeName.STR.name());
                    break;
                case Integer:
                    pythonConfig.pythonInput(value.name(),PythonType.TypeName.INT.name());
                    schemaValues.put(value.name(), 1);
                    break;
                case String:
                    pythonConfig.pythonInput(value.name(),PythonType.TypeName.STR.name());
                    schemaValues.put(value.name(), "1.0");
                    break;
                case Time:
                    fieldInfo.put("timeZoneId", TimeZone.getDefault().getID());
                    Instant now = Instant.now();
                    schemaValues.put(value.name(), now);
                    pythonConfig.pythonInput(value.name(),PythonType.TypeName.STR.name());
                    break;
                case Categorical:
                    pythonConfig.pythonInput(value.name(),PythonType.TypeName.STR.name());
                    fieldInfo.put("categories",new JsonArray().add("cat"));
                    schemaValues.put(value.name(), "cat");
                    break;
                case Bytes:
                    pythonConfig.pythonInput(value.name(),PythonType.TypeName.STR.name());
                    schemaValues.put(value.name(), new byte[]{1, 0});
                    break;
                case Long:
                    pythonConfig.pythonInput(value.name(),PythonType.TypeName.INT.name());
                    schemaValues.put(value.name(), 1L);
                    break;

            }

        }

        pythonConfig.pythonCode(SchemaType.NDArray.name() + " += 2");

        PythonConfig built = pythonConfig.build();
        JsonObject schemaWrapper = new JsonObject();
        JsonObject valuesWrapper = new JsonObject();
        for(String fieldName : fieldNames) {
            schemaWrapper.put(fieldName,jsonSchema);
            valuesWrapper.put(fieldName,schemaValues);
            namesToPythonConfig.put(fieldName,built);
        }

        wrapper.put("values",valuesWrapper);
        wrapper.put("schema",schemaWrapper);
        PythonStep pythonStep = PythonStep.builder()
                .pythonConfigs(namesToPythonConfig)
                .inputNames(fieldNames)
                .outputNames(fieldNames)
                .build();

        InferenceConfiguration inferenceConfiguration = InferenceConfiguration.builder()
                .servingConfig(servingConfig)
                .steps(Collections.singletonList(pythonStep))
                .build();

        PipelineExecutioner pipelineExecutioner = new PipelineExecutioner(inferenceConfiguration);
        pipelineExecutioner.init(Input.DataFormat.IMAGE, Output.PredictionType.RAW);
        // Run the test
        pipelineExecutioner.doJsonInference(wrapper,null);
    }

    @Test
    public void testInitPipeline() throws Exception {
        ParallelInferenceConfig parallelInferenceConfig = ParallelInferenceConfig.defaultConfig();
        int port = PortUtils.getAvailablePort();
        System.out.println("Started on port " + port);
        String path = new ClassPathResource("inference/tensorflow/mnist/lenet_frozen.pb").getFile().getAbsolutePath();

        TensorDataTypesConfig tensorDataTypesConfig = TensorDataTypesConfig.builder()
                .inputDataType("image_tensor", TensorDataType.INT64)
                .build();


        TensorFlowConfig modelConfig = TensorFlowConfig.builder()
                .tensorDataTypesConfig(tensorDataTypesConfig)
                .modelConfigType(
                        ModelConfigType.builder()
                                .modelLoadingPath(path)
                                .modelType(ModelConfig.ModelType.TENSORFLOW)
                                .build()
                ).build();


        ServingConfig servingConfig = ServingConfig.builder()
                .httpPort(port)
                .build();

        ModelStep modelStepConfig = ModelStep.builder()
                .parallelInferenceConfig(parallelInferenceConfig)
                .inputNames(Collections.singletonList("image_tensor"))
                .outputNames(Collections.singletonList("detection_classes"))
                .modelConfig(modelConfig)
                .build();


        InferenceConfiguration configuration = InferenceConfiguration.builder()
                .step(modelStepConfig)
                .servingConfig(servingConfig)
                .build();

        PipelineExecutioner pipelineExecutioner = new PipelineExecutioner(configuration);
        pipelineExecutioner.init(Input.DataFormat.IMAGE, Output.PredictionType.RAW);
        assertNotNull("Input names should not be null.", pipelineExecutioner.inputNames());
        assertNotNull("Output names should not be null.", pipelineExecutioner.outputNames());
        assertNotNull(pipelineExecutioner.inputDataTypes);
        assertFalse(pipelineExecutioner.inputDataTypes.isEmpty());
        TestCase.assertEquals(TensorDataType.INT64, pipelineExecutioner.inputDataTypes.get("image_tensor"));
    }

    @Test
    public void testInitPipelineYolo() throws Exception {
        ParallelInferenceConfig parallelInferenceConfig = ParallelInferenceConfig.defaultConfig();
        int port = PortUtils.getAvailablePort();
        System.out.println("Started on port " + port);
        String path = new ClassPathResource("inference/tensorflow/mnist/lenet_frozen.pb").getFile().getAbsolutePath();

        TensorDataTypesConfig tensorDataTypesConfig = TensorDataTypesConfig.builder()
                .inputDataType("image_tensor", TensorDataType.INT64)
                .build();

        ObjectDetectionConfig objectRecognitionConfig = ObjectDetectionConfig
                .builder()
                .numLabels(80)
                .build();
        ImageLoadingStep imageLoadingStepConfig = ImageLoadingStep.builder()
                .inputNames(Collections.singletonList("image_tensor"))
                .outputNames(Collections.singletonList("detection_classes"))
                .objectDetectionConfig(objectRecognitionConfig)
                .build();

        ServingConfig servingConfig = ServingConfig.builder()
                .outputDataFormat(Output.DataFormat.JSON)
                .httpPort(port)
                .build();

        TensorFlowConfig modelConfig = TensorFlowConfig.builder()
                .tensorDataTypesConfig(tensorDataTypesConfig)
                .modelConfigType(ModelConfigType.tensorFlow(path))
                .build();

        ModelStep modelStepConfig = ModelStep.builder()
                .parallelInferenceConfig(parallelInferenceConfig)
                .modelConfig(modelConfig)
                .inputNames(Collections.singletonList("image_tensor"))
                .outputNames(Collections.singletonList("detection_classes"))
                .build();

        InferenceConfiguration configuration = InferenceConfiguration.builder()
                .servingConfig(servingConfig)
                .step(imageLoadingStepConfig)
                .step(modelStepConfig)
                .build();

        PipelineExecutioner pipelineExecutioner = new PipelineExecutioner(configuration);
        pipelineExecutioner.init(Input.DataFormat.IMAGE, Output.PredictionType.YOLO);
        assertNotNull("Input names should not be null.", pipelineExecutioner.inputNames());
        assertNotNull("Output names should not be null.", pipelineExecutioner.outputNames());
        TestCase.assertEquals(TensorDataType.INT64, pipelineExecutioner.inputDataTypes.get("image_tensor"));
        assertNotNull(pipelineExecutioner.getMultiOutputAdapter());
    }


}
