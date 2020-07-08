package ai.konduit.serving.models.tensorflowpython;

import ai.konduit.serving.pipeline.util.BuildUtils;
import org.nd4j.python4j.Python;
import org.nd4j.python4j.PythonGC;
import org.nd4j.python4j.PythonObject;
import org.nd4j.python4j.PythonProcess;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TFModel {

    private PythonObject pyModel;
    private String[] inputNames;
    private String[] outputNames;
    private Map<String, String> inputDTypes;
    private Map<String, String> outputDTypes;

    static {
        installTF();
    }

    private static void installTF() {
        String tfPackage = BuildUtils.isCudaBuild() ? "tensorflow-gpu" : "tensorflow";
        if (!PythonProcess.isPackageInstalled(tfPackage)) {
            PythonProcess.pipInstall(tfPackage);
        }
    }

    private static PythonObject loadModel(String path) {
        try (PythonGC gc = PythonGC.watch()) {
            PythonObject tf = Python.importModule("tensorflow");
            PythonObject model = tf.attr("saved_model").attr("load").call(path);
            PythonGC.keep(model);
            return model;
        }
    }

    public TFModel(String path) {
        pyModel = loadModel(path);
        try (PythonGC gc = PythonGC.watch()) {
            PythonObject inputs = Python.list(pyModel.attr("signatures").get("serving_default").attr("structured_input_signature").get(1).attr("items").call());
            int numInputs = Python.len(inputs).toInt();
            inputNames = new String[numInputs];
            inputDTypes = new HashMap<>();
            for (int i = 0; i < numInputs; i++) {
                String inpName = inputs.get(i).get(0).toString();
                inputNames[i] = inpName;
                inputDTypes.put(inpName, inputs.get(i).get(1).attr("dtype").attr("name").toString());
            }
            PythonObject outMap = Python.list(pyModel.attr("signatures").get("serving_default").attr("structured_outputs").attr("items").call());
            int numOuts = Python.len(outMap).toInt();
            outputNames = new String[numInputs];
            outputDTypes = new HashMap<>();
            for (int i = 0; i < numOuts; i++) {
                String outName = outMap.get(i).get(0).toString();
                outputNames[i] = outName;
                outputDTypes.put(outName, outMap.get(i).get(1).attr("dtype").attr("name").toString());
            }


        }
    }

    public int numOutputs() {
        return outputNames.length;
    }

    public int numInputs() {
        return inputNames.length;
    }

    public String[] inputNames() {
        return inputNames;
    }

    public String[] outputNames(){
        return outputNames;
    }


    public Map<String, NumpyArray> predict(Map<String, NumpyArray> inputs) {
        try (PythonGC gc = PythonGC.watch()) {
            PythonObject tf = Python.importModule("tensorflow");
            Map<String, NumpyArray> ret = new HashMap<>();
            PythonObject kwargs = Python.dict();
            for (Map.Entry<String, NumpyArray> e : inputs.entrySet()) {
                String inpDtype = e.getValue().dtype();
                String expectedDtype = inputDTypes.get(e.getKey());
                PythonObject inpTensor = tf.attr("constant").call(e.getValue().getPythonObject());
                if (!inpDtype.equals(expectedDtype)) {
                    inpTensor = tf.attr("cast").call(inpTensor, tf.attr(expectedDtype));
                }
                kwargs.set(new PythonObject(e.getKey()), inpTensor);
            }
            PythonObject outs = pyModel.attr("signatures").get("serving_default").callWithKwargs(kwargs);
            for (String k: outputNames){
                PythonObject npArr = outs.get(k).attr("numpy").call();
                PythonGC.keep(npArr);
                ret.put(k, new NumpyArray(npArr));
            }
            return ret;
        }
    }

    public Map<String, NumpyArray> predict(NumpyArray input) {
        if (inputNames.length > 1) {
            throw new IllegalStateException("Model has multiple inputs. Specify input names.");
        }
        return predict(Collections.singletonMap(inputNames[0], input));
    }

}
