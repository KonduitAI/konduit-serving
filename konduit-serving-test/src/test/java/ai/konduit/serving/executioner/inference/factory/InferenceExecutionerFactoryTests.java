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

package ai.konduit.serving.executioner.inference.factory;

import ai.konduit.serving.executioner.inference.*;
import ai.konduit.serving.model.*;
import ai.konduit.serving.model.loader.tensorflow.TensorflowGraphHolder;
import ai.konduit.serving.model.loader.tensorflow.TensorflowModelLoader;
import ai.konduit.serving.pipeline.step.ModelStep;
import ai.konduit.serving.train.TrainUtils;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.io.ClassPathResource;
import org.nd4j.linalg.primitives.Pair;
import org.nd4j.tensorflow.conversion.graphrunner.GraphRunner;

import java.io.File;

import static org.junit.Assert.*;

public class InferenceExecutionerFactoryTests {

    @Rule
    public TemporaryFolder testDir = new TemporaryFolder();

    @Test
    public void testTensorflow() throws Exception {
        ClassPathResource classPathResource = new ClassPathResource("inference/tensorflow/frozen_model.pb");
        TensorflowInferenceExecutionerFactory tensorflowInferenceExecutionerFactory = new TensorflowInferenceExecutionerFactory();
        TensorFlowConfig tensorFlowConfig = ModelConfig.tensorFlow(classPathResource.getFile().getAbsolutePath(),
                TensorDataTypesConfig.builder().inputDataType("default", TensorDataType.INT32).build(),
                classPathResource.getFile().getAbsolutePath());

        ModelStep modelPipelineStep = ModelStep.builder()
                .inputName("default")
                .outputName("output")
                .modelConfig(tensorFlowConfig)
                .build();

        TensorflowModelLoader tensorflowModelLoader = TensorflowModelLoader.createFromConfig(modelPipelineStep);
        assertFalse(tensorflowModelLoader.getInputNames().isEmpty());
        assertFalse(tensorflowModelLoader.getOutputNames().isEmpty());
        assertFalse(tensorflowModelLoader.getCastingInputTypes().isEmpty());
        assertTrue(tensorflowModelLoader.getCastingOutputTypes().isEmpty());

        InitializedInferenceExecutionerConfig initializedInferenceExecutionerConfig = tensorflowInferenceExecutionerFactory.create(modelPipelineStep);
        InferenceExecutioner inferenceExecutioner = initializedInferenceExecutionerConfig.getInferenceExecutioner();
        assertNotNull(inferenceExecutioner);

        TensorflowInferenceExecutioner tensorflowInferenceExecutioner = (TensorflowInferenceExecutioner) inferenceExecutioner;
        assertNotNull(tensorflowInferenceExecutioner.model());
        assertNotNull(tensorflowInferenceExecutioner.modelLoader());
        assertNotNull(tensorflowInferenceExecutioner.getTensorflowThreadPool());

        TensorflowGraphHolder tensorflowGraphHolder = tensorflowInferenceExecutioner.model();
        assertNotNull(tensorflowGraphHolder.getCastingInputTypes());
        assertFalse(tensorflowGraphHolder.getCastingInputTypes().isEmpty());
        assertNotNull(tensorflowGraphHolder.getTfGraph());

        GraphRunner create = tensorflowGraphHolder.createRunner();
        assertNotNull(create.getInputDataTypes());
        assertFalse(create.getInputDataTypes().isEmpty());
        assertEquals(create.getInputDataTypes().keySet().iterator().next(), create.getInputOrder().iterator().next());
    }

    @Test
    public void testKerasSequential() throws Exception {
        ClassPathResource classPathResource = new ClassPathResource("inference/keras/bidirectional_lstm_tensorflow_1.h5");
        ModelConfig modelConfig = ModelConfig.keras(classPathResource.getFile().getAbsolutePath());

        ModelStep modelPipelineStep = ModelStep.builder()
                .inputName("default")
                .outputName("output")
                .modelConfig(modelConfig)
                .build();

        KerasInferenceExecutionerFactory factory = new KerasInferenceExecutionerFactory();
        InitializedInferenceExecutionerConfig initializedInferenceExecutionerConfig = factory.create(modelPipelineStep);
        MultiLayerNetworkInferenceExecutioner multiLayerNetworkInferenceExecutioner = (MultiLayerNetworkInferenceExecutioner) initializedInferenceExecutionerConfig.getInferenceExecutioner();
        assertNotNull(multiLayerNetworkInferenceExecutioner);
        assertNotNull(multiLayerNetworkInferenceExecutioner.model());
        assertNotNull(multiLayerNetworkInferenceExecutioner.modelLoader());
    }


    @Test
    public void testSameDiff() throws Exception {
        ClassPathResource classPathResource = new ClassPathResource("inference/tensorflow/frozen_model.pb");
        SameDiffInferenceExecutionerFactory tensorflowInferenceExecutionerFactory = new SameDiffInferenceExecutionerFactory();
        SameDiffConfig tensorFlowConfig = ModelConfig.sameDiff(classPathResource.getFile().getAbsolutePath(),
                TensorDataTypesConfig.builder().inputDataType("default", TensorDataType.INT32).build());

        ModelStep modelPipelineStep = ModelStep.builder()
                .inputName("default")
                .outputName("output")
                .modelConfig(tensorFlowConfig)
                .build();

        InitializedInferenceExecutionerConfig initializedInferenceExecutionerConfig = tensorflowInferenceExecutionerFactory.create(modelPipelineStep);
        InferenceExecutioner inferenceExecutioner = initializedInferenceExecutionerConfig.getInferenceExecutioner();
        assertNotNull(inferenceExecutioner);

        SameDiffInferenceExecutioner tensorflowInferenceExecutioner = (SameDiffInferenceExecutioner) inferenceExecutioner;
        assertNotNull(tensorflowInferenceExecutioner.model());
        assertNotNull(tensorflowInferenceExecutioner.modelLoader());
    }

    @Test
    public void testMultiLayerNetwork() throws Exception {
        Pair<MultiLayerNetwork, DataNormalization> trainedNetwork = TrainUtils.getTrainedNetwork();
        MultiLayerNetwork save = trainedNetwork.getLeft();
        File dir = testDir.newFolder();
        File tmpZip = new File(dir, "dl4j_mln_model.zip");
        tmpZip.deleteOnExit();
        ModelSerializer.writeModel(save, tmpZip, true);
        ModelConfig modelConfig = ModelConfig.dl4j(tmpZip.getAbsolutePath());

        ModelStep modelPipelineStep = ModelStep.builder()
                .inputName("default")
                .outputName("output")
                .modelConfig(modelConfig)
                .build();

        Dl4jInferenceExecutionerFactory factory = new Dl4jInferenceExecutionerFactory();
        InitializedInferenceExecutionerConfig initializedInferenceExecutionerConfig = factory.create(modelPipelineStep);
        MultiLayerNetworkInferenceExecutioner multiLayerNetworkInferenceExecutioner = (MultiLayerNetworkInferenceExecutioner) initializedInferenceExecutionerConfig.getInferenceExecutioner();
        assertNotNull(multiLayerNetworkInferenceExecutioner);
        assertNotNull(multiLayerNetworkInferenceExecutioner.model());
        assertNotNull(multiLayerNetworkInferenceExecutioner.modelLoader());
    }

    @Test
    public void testComputationGraph() throws Exception {
        Pair<MultiLayerNetwork, DataNormalization> trainedNetwork = TrainUtils.getTrainedNetwork();
        ComputationGraph save = trainedNetwork.getLeft().toComputationGraph();
        File dir = testDir.newFolder();
        File tmpZip = new File(dir, "dl4j_cg_model.zip");
        tmpZip.deleteOnExit();
        ModelSerializer.writeModel(save, tmpZip, true);

        ModelConfig modelConfig = ModelConfig.dl4j(tmpZip.getAbsolutePath());

        ModelStep modelPipelineStep = ModelStep.builder()
                .inputName("default")
                .outputName("output")
                .modelConfig(modelConfig)
                .build();

        Dl4jInferenceExecutionerFactory factory = new Dl4jInferenceExecutionerFactory();
        InitializedInferenceExecutionerConfig initializedInferenceExecutionerConfig = factory.create(modelPipelineStep);
        MultiComputationGraphInferenceExecutioner multiComputationGraphInferenceExecutioner = (MultiComputationGraphInferenceExecutioner) initializedInferenceExecutionerConfig.getInferenceExecutioner();
        assertNotNull(multiComputationGraphInferenceExecutioner);
        assertNotNull(multiComputationGraphInferenceExecutioner.model());
        assertNotNull(multiComputationGraphInferenceExecutioner.modelLoader());
    }
}
