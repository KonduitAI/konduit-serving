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
                "class TensorDataType(object):\n",
                "class TensorDataType(object):\n" +
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

        code = code.replace(
                "class PmmlConfig(object):\n",
                "class PmmlConfig(object):\n" +
                        "    \"\"\"PmmlConfig\n" +
                        "\n" +
                        "    Configuration for models in PMML format\n" +
                        "\n" +
                        "    :param tensor_data_types_config: konduit.TensorDataTypesConfig\n" +
                        "    :param model_config_type: konduit.ModelConfigType\n" +
                        "    :param evaluator_factory_name: defaults to \"org.jpmml.evaluator.ModelEvaluatorFactory\". Custom extensions\n" +
                        "           have to be written in Java.\n" +
                        "    \"\"\""
        );

        code = code.replace(
                "class ObjectDetectionConfig(object):\n",
                "class ObjectDetectionConfig(object):\n" +
                        "    \"\"\"ObjectDetectionConfig\n" +
                        "\n" +
                        "     Configuration for object detection output of models.\n" +
                        "\n" +
                        "    :param threshold: cut-off threshold for detected objects, defaults to 0.5\n" +
                        "    :param num_labels: the number of labels to predict with your model.\n" +
                        "    :param labels_path: Path to file containing the labels\n" +
                        "    :param priors: list of bounding box priors (list of list of floating point numbers)\n" +
                        "    :param input_shape: input shape of the data\n" +
                        "    \"\"\""
        );

        code = code.replace(
                "class SchemaType(object):\n",
                "class SchemaType(object):\n" +
                        "    \"\"\"SchemaType\n" +
                        "\n" +
                        "    Type of an input or output to a pipeline step. Can be any of:\n" +
                        "\n" +
                        "    'String', 'Integer', 'Long', 'Double', 'Float', 'Categorical', 'Time', 'Bytes',\n" +
                        "    'Boolean', 'NDArray', 'Image'.\n" +
                        "    \"\"\""
        );

        code = code.replace(
                "class Input(object):\n",
                "class Input(object):\n" +
                        "    \"\"\"Input\n" +
                        "\n" +
                        "    Used for specifying various kinds of configuration about inputs\n" +
                        "    for the server. Input.DataFormat defines in which data format\n" +
                        "    an input variable is expected to be specified. Can be any of\n" +
                        "\n" +
                        "    'NUMPY', 'JSON', 'ND4J', 'IMAGE', or 'ARROW'\n" +
                        "    \"\"\""
        );


        code = code.replace(
                "class Output(object):\n",
                "class Output(object):\n" +
                        "    \"\"\"Output\n" +
                        "\n" +
                        "    Used for specifying various kinds of configuration about outputs\n" +
                        "    for the server. Outnput.DataFormat defines in which data format\n" +
                        "    an input variable is expected to be specified. Can be any of\n" +
                        "\n" +
                        "    'NUMPY', 'JSON', 'ND4J', or 'ARROW'.\n" +
                        "\n" +
                        "    Additionally, Output.PredictionType defines the type of prediction\n" +
                        "    you want to specify for your pipeline. The prediction type determines\n" +
                        "    which \"output adapter\" is used to transform the output. Currently you\n" +
                        "    can choose from the following values:\n" +
                        "\n" +
                        "    'CLASSIFICATION', 'YOLO', 'SSD', 'RCNN', 'RAW', 'REGRESSION'\n" +
                        "    \"\"\""
        );

        code = code.replace(
                "class SameDiffConfig(object):\n",
                "class SameDiffConfig(object):\n" +
                        "    \"\"\"SameDiffConfig\n" +
                        "\n" +
                        "    Extension of ModelConfig to DL4J SameDiff models\n" +
                        "\n" +
                        "    :param tensor_data_types_config: konduit.TensorDataTypesConfig\n" +
                        "    :param model_config_type: konduit.ModelConfigType\n" +
                        "    \"\"\""
        );

        return code;
    }
}
