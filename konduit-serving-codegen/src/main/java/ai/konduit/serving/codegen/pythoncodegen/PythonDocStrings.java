package ai.konduit.serving.codegen.pythoncodegen;

public class PythonDocStrings {

    public static String generateDocs(String code) {

        code = code.replace(
                "class TensorDataTypesConfig(object):\n",
                "class TensorDataTypesConfig(object):\n"+
                        "    \"\"\"TensorDataTypesConfig\n" +
                        "    \n" +
                        "    Configures TensorDataTypes for inputs and outputs of a step.\n" +
                        "    \n" +
                        "    :param input_data_types: List of input konduit.TensorDataType\n" +
                        "    :param output_data_types: List of output konduit.TensorDataType\n" +
                        "    \"\"\""
        );
        return code;
    }
}
