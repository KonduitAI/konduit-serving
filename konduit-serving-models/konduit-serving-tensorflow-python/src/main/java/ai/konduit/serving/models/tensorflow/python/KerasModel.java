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


package ai.konduit.serving.models.tensorflow.python;

import ai.konduit.serving.pipeline.util.BuildUtils;
import org.nd4j.python4j.*;

public class KerasModel {

    private PythonObject pyModel;


    static {
        installTF();
    }

    private static void installTF() {
        String tfPackage = BuildUtils.isCudaBuild() ? "tensorflow-gpu" : "tensorflow";
        if (!PythonProcess.isPackageInstalled(tfPackage)) {
            PythonProcess.pipInstall(tfPackage);
        }
    }

    private static PythonObject getKerasModule() {
        try (PythonGC gc = PythonGC.watch()) {
            PythonObject tf = Python.importModule("tensorflow");
            PythonObject keras = tf.attr("keras");
            PythonGC.keep(keras);
            return keras;
        }

    }

    private static PythonObject loadModel(String s) {
        //String path = URIResolver.getCachedFile(s).getAbsolutePath();     //TODO this isn't resolving "no path" URIs like "model.h5" instead of "C:/Data/model.h5"
        try (PythonGC gc = PythonGC.watch()) {
            PythonObject models = getKerasModule().attr("models");
            PythonObject loadModelF = models.attr("load_model");
            PythonObject model = loadModelF.call(s);
            PythonGC.keep(model);
            return model;
        }
    }

    public KerasModel(String path) {
        pyModel = loadModel(path);
    }

    public KerasModel(PythonObject pythonObject) {
        pyModel = pythonObject;
    }

    public NumpyArray[] predict(NumpyArray... inputs) {
        try (PythonGC gc = PythonGC.watch()) {
            PythonObject predictF = pyModel.attr("predict");
            PythonObject[] pyInputs = new PythonObject[inputs.length];
            for (int i = 0; i < inputs.length; i++) {
                pyInputs[i] = inputs[i].getPythonObject();
            }
            PythonObject inputList = new PythonObject(pyInputs);
            PythonObject pyOut = predictF.call(inputList);
            PythonGC.keep(pyOut);
            NumpyArray[] out;
            if (Python.isinstance(pyOut, Python.listType())) {
                out = new NumpyArray[Python.len(pyOut).toInt()];
                for (int i = 0; i < out.length; i++) {
                    out[i] = new NumpyArray(pyOut.get(i));
                }
            } else {
                out = new NumpyArray[]{new NumpyArray(pyOut)};
            }
            return out;
        }
    }

    public int numInputs() {
        try (PythonGC gc = PythonGC.watch()) {
            PythonObject inputs = pyModel.attr("inputs");
            PythonObject pyNumInputs = Python.len(inputs);
            int ret = pyNumInputs.toInt();
            return ret;
        }
    }

    public int numOutputs() {
        try (PythonGC gc = PythonGC.watch()) {
            PythonObject outputs = pyModel.attr("outputs");
            PythonObject pyNumOutputs = Python.len(outputs);
            int ret = pyNumOutputs.toInt();
            return ret;
        }
    }

    public long[][] inputShapes() {
        long[][] ret = new long[numInputs()][];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = inputShapeAt(i);
        }
        return ret;
    }

    public long[][] outputShapes() {
        long[][] ret = new long[numOutputs()][];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = outputShapeAt(i);
        }
        return ret;
    }

    public long[] inputShapeAt(int input) {
        try (PythonGC gc = PythonGC.watch()) {
            PythonObject inputs = pyModel.attr("inputs");
            PythonObject tensor = inputs.get(input);
            PythonObject tensorShape = tensor.attr("shape");
            PythonObject shapeList = Python.list(tensorShape);
            PythonObject pyNdim = Python.len(shapeList);
            int ndim = pyNdim.toInt();
            long[] shape = new long[ndim];
            for (int i = 0; i < shape.length; i++) {
                PythonObject pyDim = shapeList.get(i);
                if (pyDim == null || !Python.isinstance(pyDim, Python.intType())) {
                    shape[i] = -1;
                } else {
                    shape[i] = pyDim.toLong();
                }
            }
            return shape;
        }
    }

    public long[] outputShapeAt(int output) {
        try (PythonGC gc = PythonGC.watch()) {
            PythonObject inputs = pyModel.attr("outputs");
            PythonObject tensor = inputs.get(output);
            PythonObject tensorShape = tensor.attr("shape");
            PythonObject shapeList = Python.list(tensorShape);
            PythonObject pyNdim = Python.len(shapeList);
            int ndim = pyNdim.toInt();
            long[] shape = new long[ndim];
            for (int i = 0; i < shape.length; i++) {
                PythonObject pyDim = shapeList.get(i);
                if (pyDim == null || !Python.isinstance(pyDim, Python.intType())) {
                    shape[i] = -1;
                } else {
                    shape[i] = pyDim.toLong();
                }
            }
            return shape;
        }
    }

    public void close(){
        pyModel.del();
    }
}
