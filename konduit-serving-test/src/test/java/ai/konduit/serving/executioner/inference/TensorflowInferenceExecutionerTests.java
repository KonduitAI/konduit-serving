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

package ai.konduit.serving.executioner.inference;

import ai.konduit.serving.config.ParallelInferenceConfig;
import ai.konduit.serving.model.loader.tensorflow.TensorflowModelLoader;
import org.deeplearning4j.parallelism.inference.InferenceMode;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.nd4j.common.io.ClassPathResource;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.tensorflow.conversion.graphrunner.SavedModelConfig;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

@NotThreadSafe
public class TensorflowInferenceExecutionerTests {
    @Rule
    public TemporaryFolder testDir = new TemporaryFolder();

    @Test(timeout = 60000)
    public void testInferenceExecutioner() throws Exception {
        TensorflowModelLoader tensorflowModelLoader = TensorflowModelLoader.builder()
                .inputNames(Arrays.asList("input_0", "input_1"))
                .outputNames(Collections.singletonList("output"))
                .protoFile(new ClassPathResource("inference/tensorflow/frozen_model.pb").getFile())
                .build();

        TensorflowInferenceExecutioner tensorflowInferenceExecutioner = new TensorflowInferenceExecutioner();

        tensorflowInferenceExecutioner.initialize(tensorflowModelLoader, ParallelInferenceConfig.builder()
                .batchLimit(1)
                .workers(1)
                .queueLimit(1)
                .inferenceMode(InferenceMode.SEQUENTIAL)
                .build());

        INDArray assertion = Nd4j.linspace(1, 4, 4).muli(2);
        INDArray[] output = tensorflowInferenceExecutioner.execute(new INDArray[]{Nd4j.linspace(1, 4, 4), Nd4j.linspace(1, 4, 4)});
        assertEquals(assertion, output[0]);
        tensorflowInferenceExecutioner.stop();
    }

    @Test
    public void testSavedModelInferenceExecutioner() throws Exception {
        File f = testDir.newFolder();
        new ClassPathResource("/inference/tensorflow/saved_model_counter/00000123/").copyDirectory(f);

        SavedModelConfig savedModelConfig = SavedModelConfig.builder()
                .modelTag("serve")
                .signatureKey("incr_counter_by")
                .savedModelPath(f.getAbsolutePath())
                .build();

        TensorflowModelLoader tensorflowModelLoader = TensorflowModelLoader.builder()
                .savedModelConfig(savedModelConfig)
                .build();

        TensorflowInferenceExecutioner tensorflowInferenceExecutioner = new TensorflowInferenceExecutioner();

        tensorflowInferenceExecutioner.initialize(tensorflowModelLoader, ParallelInferenceConfig.builder()
                .batchLimit(1)
                .workers(1)
                .queueLimit(1)
                .inferenceMode(InferenceMode.SEQUENTIAL)
                .build());

        INDArray assertion = Nd4j.create(new float[]{42}, new long[0]);
        INDArray[] output = tensorflowInferenceExecutioner.execute(new INDArray[]{assertion});
        assertEquals(assertion.getDouble(0), output[0].getDouble(0), 1e-1);
        tensorflowInferenceExecutioner.stop();
    }
}
