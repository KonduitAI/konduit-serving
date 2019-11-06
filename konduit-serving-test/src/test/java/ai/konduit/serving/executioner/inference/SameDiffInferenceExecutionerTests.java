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

import ai.konduit.serving.model.loader.samediff.SameDiffModelLoader;
import ai.konduit.serving.config.ParallelInferenceConfig;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.nd4j.autodiff.samediff.SDVariable;
import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class SameDiffInferenceExecutionerTests {

    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    @Test(timeout = 60000)

    public void testSameDiff() throws Exception {
        SameDiffInferenceExecutioner sameDiffInferenceExecutioner = new SameDiffInferenceExecutioner();
        SameDiff sameDiff = SameDiff.create();
        SDVariable input1 = sameDiff.var("input1",2,2);
        SDVariable input2 = sameDiff.var("input2",2,2);
        SDVariable result = input1.add("output",input2);
        INDArray input1Arr = Nd4j.linspace(1,4,4).reshape(2,2);
        INDArray input2Arr = Nd4j.linspace(1,4,4).reshape(2,2);
        sameDiff.associateArrayWithVariable(input1Arr,input1.getVarName());
        sameDiff.associateArrayWithVariable(input2Arr,input2.getVarName());
        Map<String,INDArray> indArrays = new LinkedHashMap<>();
        indArrays.put(input1.getVarName(),input1Arr);
        indArrays.put(input2.getVarName(),input2Arr);
        Map<String, INDArray> outputs = sameDiff.outputAll(indArrays);
        assertEquals(3,outputs.size());

        ParallelInferenceConfig parallelInferenceConfig = ParallelInferenceConfig.defaultConfig();
        File newFile = temporary.newFile();
        sameDiff.asFlatFile(newFile);
        SameDiffModelLoader sameDiffModelLoader = new SameDiffModelLoader(newFile, Arrays.asList("input1","input2"),Arrays.asList("output"));


        sameDiffInferenceExecutioner.initialize(sameDiffModelLoader, parallelInferenceConfig);



        INDArray[] execute = sameDiffInferenceExecutioner.execute(new INDArray[]{input1Arr, input2Arr});
        assertEquals(outputs.values().iterator().next(),execute[0]);
    }

}
