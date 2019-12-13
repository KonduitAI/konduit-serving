package ai.konduit.serving.verticles.python;

import ai.konduit.serving.util.python.PythonVariables;
import org.nd4j.linalg.api.ndarray.INDArray;

public class PythonTestGenerator{

    private static String getPythonRepr(Object obj, PythonVariables.Type pythonType){
        switch (pythonType){
            case STR:
                return "'" + obj.toString() + "'";
            case NDARRAY:
                INDArray arr = (INDArray)obj;
                StringBuilder sb = new StringBuilder();
                sb.append("np.array([");
                for (int i=0;i <arr.length(); i++){
                    sb.append(arr.ravel().getDouble(i));
                    sb.append(", ");
                }
                sb.append("]).reshape(");
                for (int i=0; i < arr.rank(); i++){
                    sb.append(arr.shape()[i]);
                    sb.append(", ");
                }
                sb.append(")");
                return sb.toString();
            case BOOL:
                return (boolean)obj ? "True": "False";
                default:
                    return obj.toString();
        }
    }
    private static String getPythonCode(String inputName, String outputName, PythonVariables outputValues){
        String code = "";
        String inpKey = "__" + inputName + "_key";
        code +=  inpKey + " = str(" + inputName + ")\n";
        code += "__d = {'" + inpKey + "':" + getPythonRepr(outputValues.getValue(outputName), outputValues.getType(outputName)) + "}\n";
        code += outputName + " = __d['" + inpKey + "']\n";
        return code;
    }

    public static String getPythonCode(PythonVariables inputs, PythonVariables outputs){
        StringBuilder code = new StringBuilder();
        for (String inp: inputs.getVariables()){
            PythonVariables.Type inpType = inputs.getType(inp);
            code.append("assert isinstance(");
            code.append(inp);
            code.append(", ");
            code.append(inpType.name().toLowerCase()) ;
            code.append(")\n");
        }
        for (int i=0; i<outputs.getVariables().length; i++){
            String inp = inputs.getVariables()[i % inputs.getVariables().length];
            String out = outputs.getVariables()[i];
            code.append(getPythonCode(inp, out, outputs));
        }
        return code.toString();

    }


}
