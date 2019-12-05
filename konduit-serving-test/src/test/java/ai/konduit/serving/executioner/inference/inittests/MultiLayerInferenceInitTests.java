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

package ai.konduit.serving.executioner.inference.inittests;

import ai.konduit.serving.config.ParallelInferenceConfig;
import ai.konduit.serving.executioner.inference.MultiLayerNetworkInferenceExecutioner;
import ai.konduit.serving.model.loader.dl4j.mln.InMemoryMultiLayernetworkModelLoader;
import ai.konduit.serving.train.TrainUtils;
import org.deeplearning4j.datasets.iterator.impl.IrisDataSetIterator;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.primitives.Pair;

import java.io.File;


public class MultiLayerInferenceInitTests {

    @ClassRule
    public static TemporaryFolder folder = new TemporaryFolder();

    @Test(timeout = 60000)

    public void testMultiLayerNetwork() throws Exception {
        MultiLayerNetworkInferenceExecutioner multiLayerNetworkInferenceExecutioner = new MultiLayerNetworkInferenceExecutioner();

        ParallelInferenceConfig parallelInferenceConfig = ParallelInferenceConfig.defaultConfig();


        File arrayFolder = folder.newFolder();
        File testLabels = new File(arrayFolder, "test-labels.npy");
        File testInput = new File(arrayFolder, "test-input.npy");

        DataSet dataSet = new IrisDataSetIterator(150, 150).next();
        Nd4j.writeAsNumpy(dataSet.getFeatures(), testInput);
        Nd4j.writeAsNumpy(dataSet.getLabels(), testLabels);

        Pair<MultiLayerNetwork, DataNormalization> trainedNetwork = TrainUtils.getTrainedNetwork();
        trainedNetwork.getSecond().transform(dataSet.getFeatures());

        InMemoryMultiLayernetworkModelLoader memoryMultiLayernetworkModelLoader = new InMemoryMultiLayernetworkModelLoader(trainedNetwork.getFirst());
        multiLayerNetworkInferenceExecutioner.initialize(memoryMultiLayernetworkModelLoader, parallelInferenceConfig);

    }

}
