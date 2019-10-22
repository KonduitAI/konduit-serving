# Tests 
This article discusses the tests for the Python SDK of Konduit.

## Pipeline steps 

### [`test_start.py`](https://github.com/KonduitAI/konduit-serving/blob/master/python/tests/test_start.py)

Checks if a server configured with a TensorFlow model can be started, then stops it. Starts a server based on an [InferenceConfiguration](../konduit/inference.py#L2386-L2427) with a single [ModelPipelineStep](https://github.com/KonduitAI/konduit-serving/blob/master/python/konduit/inference.py#L1579-L1778) built with a model trained in TensorFlow, configured using the [TensorFlowConfig](../konduit/inference.py#L656-L720) class. This server takes as input data type 'NUMPY' and outputs a 'NUMPY' object. 

Possible data types are listed in [`konduit.inference`](../konduit/inference.py#L863-L865) and should be specified in ServingConfig:
- Accepted input data types are JSON, ARROW, IMAGE, ND4J (not yet implemented), NUMPY. 
- Accepted output data types are NUMPY, JSON, ND4J (not yet implemented), ARROW
- Prediction types supported are CLASSIFICATION, YOLO, SSD, RCNN, RAW, REGRESSION. 

### [`test_transform_process.py`](test_transform_process.py)

Build [InferenceConfiguration](../konduit/inference.py#L2386-L2427) with [TransformProcessPipelineStep](../konduit/inference.py#L1400-L1573), then dumps a JSON of the configuration. Use [InferenceConfiguration](../konduit/inference.py#L2386-L2427) object in Server, Client queries Server and prints predicted output. 

### [`test_transform_process_arrow.py`](test_transform_process_arrow.py)

Similar to `test_transform_process.py`, but with Arrow output from client. 

### [`test_python_serving.py`](test_python_serving.py)

Tests whether Python pipelines can be served. Takes as input a NumPy array saved in `../data/input-0.npy` and performs a simple operation on the model inputs in a [PythonPipelineStep](../konduit/inference.py#L1221-L1394).

### [`test_bert_serving.py`](test_bert_serving.py)

Similar to `test_start.py`, but also prints the predicted output given NumPy arrays. 

## Saving configurations 

### [`test_json.py`](test_json.py)

Saves server configuration as JSON, loads it again, and checks if the loaded and existing configuration steps have equal length. 

`test_json_compare()` creates a configuration similar to `test_start.py`, while `test_python_serde()` creates a Python pipeline similar to `test_python_serving.py`.


## Client conversion and encoding 

### [`test_client_serde.py`](test_client_serde.py)

#### `test_multipart_encode()`
Tests the following methods for the [Client](../konduit/client.py#L10-L139) class: 
- [`_convert_numpy_to_binary()`](../konduit/client.py#L72-L76) - converts inputs into a dictionary with keys and binary data as values
- [`_convert_multi_part_inputs()`](../konduit/client.py#L100-L108) - encodes the dictionary from `convert_numpy_to_binary()` in a multipart request body
- [`_encode_multi_part_input()`](../konduit/client.py#L85-L97) - decodes the multipart response into binary data or, depending on the output type, converts into the corresponding output class
- [`_convert_multi_part_output()`](../konduit/client.py#L110-L129) - converts output returned by the server into the output type requested by the client

#### `test_python_serde()`
Similar to `test_python_serving.py`. Dumps JSON.
