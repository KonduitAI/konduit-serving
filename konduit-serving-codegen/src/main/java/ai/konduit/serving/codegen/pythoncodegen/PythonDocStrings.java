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


        code = code.replace(
                "class SavedModelConfig(object):\n",
                "class SavedModelConfig(object):\n" +
                        "    \"\"\"SavedModelConfig\n" +
                        "\n" +
                        "    SavedModel Configuration for TensorFlow models\n" +
                        "\n" +
                        "    :param saved_model_path: path to the saved model\n" +
                        "    :param model_tag: a tag to give the model, e.g. \"serve\"\n" +
                        "    :param signature_key: TensorFlow SignatureDef key, e.g. \"incr_counter_by\"\n" +
                        "    :param saved_model_input_order: list of input variables in order\n" +
                        "    :param save_model_output_order: list of output variables in order\n" +
                        "    \"\"\""
        );

        code = code.replace(
                "class ParallelInferenceConfig(object):\n",
                "class ParallelInferenceConfig(object):\n" +
                        "    \"\"\"ParallelInferenceConfig\n" +
                        "\n" +
                        "    Configuration for parallel inference.\n" +
                        "\n" +
                        "    :param queue_limit:\n" +
                        "    :param batch_limit:\n" +
                        "    :param workers:\n" +
                        "    :param max_train_epochs:\n" +
                        "    :param inference_mode:\n" +
                        "    :param vertx_config_json:\n" +
                        "    \"\"\""
        );

        code = code.replace(
                "class ModelConfigType(object):\n",
                "class ModelConfigType(object):\n" +
                        "    \"\"\"ModelConfigType\n" +
                        "\n" +
                        "    This model configuration has meta data for a model loader, which\n" +
                        "    includes the model loading path and the model type. It's used in\n" +
                        "    konduit.ModelConfig.\n" +
                        "\n" +
                        "    :param model_type: Can be any of 'COMPUTATION_GRAPH', 'MULTI_LAYER_NETWORK',\n" +
                        "        'PMML', 'TENSORFLOW', 'KERAS', and 'SAMEDIFF'.\n" +
                        "    :param model_loading_path: path to the model file\n" +
                        "    \"\"\""
        );

        code = code.replace(
                "class ModelConfig(object):\n",
                "class ModelConfig(object):\n" +
                        "    \"\"\"ModelConfig\n" +
                        "\n" +
                        "    Model configurations hold the TensorDataTypeConfig and the\n" +
                        "    .ModelConfigType of the model you want to serve.\n" +
                        "\n" +
                        "    :param tensor_data_types_config: konduit.TensorDataTypeConfig\n" +
                        "    :param model_config_type: konduit.ModelConfigType\n" +
                        "    \"\"\""
        );

        code = code.replace(
                "class ModelConfig(object):\n",
                "class ModelConfig(object):\n" +
                        "    \"\"\"TensorDataType\n" +
                        "\n" +
                        "    Possible data types for tensors. Comes with conversions from TensorFlow\n" +
                        "    and Python and between ND4J types. Choose from\n" +
                        "\n" +
                        "    INVALID, FLOAT, DOUBLE, INT32, UINT8, INT16, INT8, STRING, COMPLEX64,\n" +
                        "    INT64, BOOL, QINT8, QUINT8, QINT32, BFLOAT16, QINT16, QUINT16, UINT16,\n" +
                        "    COMPLEX128, HALF, RESOURCE, VARIANT, UINT32, UINT64\n" +
                        "    \"\"\""
        );

        return code;
    }
}
