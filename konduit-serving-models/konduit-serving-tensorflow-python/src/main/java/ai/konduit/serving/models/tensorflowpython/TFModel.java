package ai.konduit.serving.models.tensorflowpython;

import ai.konduit.serving.pipeline.util.BuildUtils;
import org.nd4j.python4j.Python;
import org.nd4j.python4j.PythonGC;
import org.nd4j.python4j.PythonObject;
import org.nd4j.python4j.PythonProcess;

import java.util.Collections;
import java.util.Map;

public class TFModel {

    private PythonObject pyModel;
    private String[] inputNames;
    private int numOutputs;

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
            PythonObject inputs = Python.list(pyModel.attr("signatures").get("serving_default").attr("structured_input_signature").get(1).attr("keys").call());
            int numInputs = Python.len(inputs).toInt();
            inputNames = new String[numInputs];
            for (int i = 0; i < numInputs; i++) {
                inputNames[i] = inputs.get(i).toString();
            }
            numOutputs = Python.len(pyModel.attr("signatures").get("serving_default").attr("outputs")).toInt();
        }
    }

    public int numOutputs(){
        return numOutputs;
    }

    public int numInputs(){
        return inputNames.length;
    }
    public String[] inputNames(){
        return inputNames;
    }



    public NumpyArray[] predict(Map<String, NumpyArray> inputs){
        try(PythonGC gc = PythonGC.watch()){
            NumpyArray[] ret = new NumpyArray[numOutputs];
            PythonObject kwargs = Python.dict();
            for (Map.Entry<String, NumpyArray> e: inputs.entrySet()){
                kwargs.set(new PythonObject(e.getKey()), e.getValue().getPythonObject());
            }
            PythonObject outs = pyModel.attr("signatures").get("serving_default").attr("structured_outputs");
            for (int i = 0; i < numOutputs; i++){
                ret[i] = new NumpyArray(outs.get("output_" + i).attr("numpy").call());
            }
            return ret;
        }
    }

    public NumpyArray[] predict(NumpyArray input){
        if (inputNames.length > 1){
            throw new IllegalStateException("Model has multiple inputs. Specify input names.");
        }
        return predict(Collections.singletonMap(inputNames[0], input));
    }

}
