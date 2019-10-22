# Tests 
This article discusses the tests for Konduit's Python SDK.

## Pipeline steps 

### [`test_start.py`](https://github.com/KonduitAI/konduit-serving/blob/master/python/tests/test_start.py)

Checks if a server configured with a TensorFlow model can be started, then stops it. 

Starts a server based on an [InferenceConfiguration](../konduit/inference.py#L2386-L2427) with a single [ModelPipelineStep](../konduit/inference.py#L1579-L1778) built with a model trained in TensorFlow, configured using the [TensorFlowConfig](../konduit/inference.py#L656-L720) class. 

This server is configured to accept input data type 'NUMPY' and output data type 'NUMPY'. 

Possible data types for the server configuration are listed in [`konduit.inference`](../konduit/inference.py#L863-L865) and should be specified in [ServingConfig](../konduit/inference.py#L861-L1018):
- Accepted input data types are JSON, ARROW, IMAGE, ND4J (not yet implemented) and NUMPY. 
- Accepted output data types are NUMPY, JSON, ND4J (not yet implemented) and ARROW.

### [`test_transform_process.py`](test_transform_process.py)

Checks if a TransformProcessPipelineStep can be performed. 

This tests builds [InferenceConfiguration](../konduit/inference.py#L2386-L2427) with [TransformProcessPipelineStep](../konduit/inference.py#L1400-L1573). 

The transform process is defined by [TransformProcessBuilder](https://github.com/eclipse/deeplearning4j/blob/master/datavec/datavec-api/src/main/java/org/datavec/api/transform/TransformProcess.java#L611) (the Builder subclass of [TransformProcess](https://deeplearning4j.org/docs/latest/datavec-transforms) in DataVec), written to a JSON file and loaded again for use in a [TransformProcessPipelineStep](../konduit/inference.py#L1400-L1573). [TransformProcess](https://deeplearning4j.org/docs/latest/datavec-transforms) in turn requires defining a [Schema](https://deeplearning4j.org/docs/latest/datavec-schema). 

The [InferenceConfiguration](../konduit/inference.py#L2386-L2427) object is an argument to Server. Given a JSON file as input, the Client sends a query to the Server and prints the predicted output. 

This server is configured for JSON input and output. 

### [`test_transform_process_arrow.py`](test_transform_process_arrow.py)

Similar to `test_transform_process.py`, checks if a DataVec transformation can be configured, but with the server configured to return Arrow output instead of JSON. 

### [`test_python_serving.py`](test_python_serving.py)

Tests whether Python pipelines can be served. Takes as input a NumPy array saved in `../data/input-0.npy` and performs a simple operation on the model inputs in a [PythonPipelineStep](../konduit/inference.py#L1221-L1394).

The input and output data types of this server are configured to be NUMPY. 

### [`test_bert_serving.py`](test_bert_serving.py)

Similar to `test_start.py`, creates a configuration with a TensorFlow BERT model loaded with [TensorFlowConfig](../konduit/inference.py#L656-L720), but also prints the predicted output given NumPy arrays. 

The input and output data types of this server are configured to be NUMPY. 

## Saving configurations 

### [`test_json.py`](test_json.py)

After configuring a server, saves server configuration as JSON, loads it again, and checks if the existing configuration steps has the same length as the configuration loaded from the JSON file. 

Two server configurations are defined: 
- Similar to `test_start.py`, `test_json_compare()` creates a configuration with a TensorFlow model loaded with [TensorFlowConfig](../konduit/inference.py#L656-L720), 
- `test_python_serde()` configures a server with a [PythonPipelineStep](../konduit/inference.py#L1221-L1394) similar to `test_python_serving.py`.


## Client conversion and encoding 

### [`test_client_serde.py`](test_client_serde.py)

#### `test_multipart_encode()`
Tests the following methods for the [Client](../konduit/client.py#L10-L139) class: 
- [`_convert_numpy_to_binary()`](../konduit/client.py#L72-L76): converts inputs into a dictionary (key-value pairs) with binary data as values.
- [`_convert_multi_part_inputs()`](../konduit/client.py#L100-L108): encodes the dictionary from `convert_numpy_to_binary()` in a multipart request body.
- [`_encode_multi_part_input()`](../konduit/client.py#L85-L97): decodes the multipart response into binary data or, depending on the output type, converts into the corresponding output class.
- [`_convert_multi_part_output()`](../konduit/client.py#L110-L129): converts output returned by the server into the output type requested by the client.

#### `test_python_serde()`
Similar to `test_python_serving.py`, configures a server with [PythonPipelineStep](../konduit/inference.py#L1221-L1394), then dumps a JSON file containing the configuration.
