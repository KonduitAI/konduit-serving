/* ******************************************************************************
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
 ******************************************************************************/


package ai.konduit.serving.models.tensorflowpython;

import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.impl.data.JData;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.nd4j.python4j.Python;
import org.nd4j.python4j.PythonGC;
import org.nd4j.python4j.PythonObject;
import org.nd4j.python4j.PythonProcess;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RunWith(Parameterized.class)
public class TFStepTest {

    @ClassRule
    public static TemporaryFolder testDir = new TemporaryFolder();
    public static String modelFileSM = null;

    private String modelPath;
    private String[] inputKeys;
    private String[] outputKeys;
    private NDArray[] inputs;
    private NDArray[] outputs;

    private static NDArray rand(long... shape) {
        try (PythonGC gc = PythonGC.watch()) {
            PythonObject ret = Python.importModule("numpy").attr("random").attr("random").call(shape);
            PythonGC.keep(ret);
            return new NumpyArray.NumpyNDArray(new NumpyArray(ret));
        }
    }

    @Parameterized.Parameters
    public static Object[] getTestModels() throws Exception {
        if (!PythonProcess.isPackageInstalled("tensorflow")) {
            PythonProcess.pipInstall("tensorflow");
        }

        // TODO use test resources instead

        PythonObject keras = Python.importModule("tensorflow").attr("keras");
        PythonObject layers = keras.attr("layers");
        PythonObject models = keras.attr("models");
        PythonObject model;
        NDArray[] inputs;
        NDArray[] outputs;

        testDir.create();
        File dir = testDir.newFolder();
        File pathSM = new File(dir, "savedModel1");
        modelFileSM = pathSM.getAbsolutePath();

        // single input/output sequential
        model = models.attr("Sequential").call();
        model.attr("add").call(layers.attr("Dense").callWithArgsAndKwargs(Collections.singletonList(10), Collections.singletonMap("input_dim", 5)));
        model.attr("save").call(modelFileSM);


        inputs = new NDArray[]{rand(32, 5)};
        NumpyArray[] npOuts = new KerasModel(model).predict(inputs[0].getAs(NumpyArray.class));
        outputs = new NDArray[npOuts.length];
        for (int i=0;i<npOuts.length;i++){
            outputs[i] = new NumpyArray.NumpyNDArray(npOuts[i]);
        }

        return new Object[]{
                new Object[]{modelFileSM, inputs, outputs, null, new String[]{"out"}}
        };

    }

    public TFStepTest(String modelPath, NDArray[] inputs, NDArray[] outputs, String inputKeys[], String outputKeys[]) {
        this.modelPath = modelPath;
        this.inputs = inputs;
        this.outputs = outputs;
        this.inputKeys = inputKeys;
        this.outputKeys = outputKeys;
    }

    @Test
    public void testTFStep() {
        Map<String, String> inputKeyMap;
        if (inputKeys == null){
            inputKeyMap = null;
        }
        else{
            inputKeyMap = new HashMap<>();
            for (String k: inputKeys){
                inputKeyMap.put(k, k);
            }
        }
        PipelineStep step = new TFStep().modelUri(modelPath).inputKeyMap(inputKeyMap).outputKeys(outputKeys);
        Pipeline pipeline = SequencePipeline.builder().add(step).build();
        Data inp = new JData();
        if (inputKeys == null) {
            inp.put("xxx", inputs[0]);
        } else {
            for (int i = 0; i < inputs.length; i++) {
                inp.put(inputKeys[i], inputs[i]);
            }
        }

        Data out = pipeline.executor().exec(inp);

        for (int i = 0; i < outputKeys.length; i++) {
            Assert.assertEquals(outputs[i], out.getNDArray(outputKeys[i]));
        }
    }


}
