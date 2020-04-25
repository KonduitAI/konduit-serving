# Pipeline API Rewrite

## Status
Accepted (25/04/2020)

Proposed by: Alex Black (10/04/2020)

Discussed with: Shams, Paul, Adam

## Context
 
Konduit Serving has the concept of a Step, which represents one component of a machine learning deployment.
For example, a ModelStep contains a neural network model; a PythonStep runs some arbitrary code that can be executed.
One or more of these steps are chained together by users (via JSON/YAML configuration or API) to create a Konduit Serving
 deployment.
During execution, the output of one step is used as input to the next step; the output of the final step is returned to 
the user, perhaps after some post processing. 

This proposal proposes a significant change in the format of the data that is passed between pipeline steps, as well as
proposing changes to the user-facing API and REST endpoints as a result of this change.

**Background**

There are a number of key classes here:
* [PipelineStep](https://github.com/KonduitAI/konduit-serving/blob/master/konduit-serving-api/src/main/java/ai/konduit/serving/pipeline/PipelineStep.java) 
  interface, which is the configuration for a step
    - Much of the JSON/YAML that users write for their deployments related to configuring PipelineSteps    
* [PipelineStepRunner](https://github.com/KonduitAI/konduit-serving/blob/master/konduit-serving-api/src/main/java/ai/konduit/serving/pipeline/PipelineStepRunner.java)
  interface, which actually handles execution.
A PipelineStepRunner is instantiated from a PipelineStep
* [InferenceExecutioner](https://github.com/KonduitAI/konduit-serving/blob/b247b211d5e2441e781ddc960bfed12dff446890/konduit-serving-api/src/main/java/ai/konduit/serving/executioner/inference/InferenceExecutioner.java)
 / [InferenceExecutionerFactory](https://github.com/KonduitAI/konduit-serving/blob/be54a01f6116015241dd2108f33169d280162265/konduit-serving-api/src/main/java/ai/konduit/serving/executioner/inference/factory/InferenceExecutionerFactory.java) - used only for machine learning (neural network) models
   * ModelStep's PipelineStepRunner is InferenceExecutionerStepRunner
   * InferenceExecutionerStepRunner creates an InferenceExecutionerFactory (one implementation for each of SameDiff,
     Tensorflow, DL4J, Pmml, Onnx, and Keras)
   * The InferenceExecutionerFactory creates a InitializedInferenceExecutionerConfig, which has an InferenceExecutioner
     internally
   * There is one InferenceExecutioner implementation for each of DL4J, SameDiff, Tensorflow, etc
   * The InferenceExecutioner has generic types - `OUTPUT_TYPE execute(INPUT_TYPE input)` - INDArray[] in/out for DL4J,
     ONNX, TF and `List<Map<FieldName, Object>>` for PMML  
   * In summary: `ModelStep --> InferenceExecutionerStepRunner --> XInferenceExecutionerFactory --> XInferenceExecutioner`


At present, the highest-level internal API (PipelineStep schemas and PipelineStepRunner transform methods) is based on
 DataVec - and follows a data-frame type API.
Ultimately, everything is converted from a DataVec `Record[]` at each end of a pipeline step.
For example, in the case of most neural network models, we do `Record[] --> INDArray[] --> model --> INDArray[] --> Record[]`,
which happens in ModelStep and the InferenceExecutors.

Note that DataVec's `Record` object is simply a `List<Writable>` internally; a `Writable` object is just "some value".
DataVec supports for example `DoubleWritable`, `NDArrayWritable`, `Text`, `ImageWritable` etc. [Link](https://github.com/eclipse/deeplearning4j/tree/master/datavec/datavec-api/src/main/java/org/datavec/api/writable)

 
There is also the concepts of output data format, prediction types, schema types and Schema - defined by the following enums/classes:
* Input.DataFormat: {NUMPY, JSON, ND4J, IMAGE, ARROW}
* Output.DataFormat: {NUMPY, JSON, ND4J, ARROW}
* Output.PredictionType: {CLASSIFICATION, YOLO, SSD, RCNN, RAW, REGRESSION}
    * This is an optional "output adapter" applies to the output
* SchemaType: {String, Integer, Long, Double, Float, Categorical, Time, Bytes, Boolean, NDArray, Image}
    * Note that this is distinct from the DataVec _ColumnType_ which has the same values minus _Image_
* Schema - which is a DataVec Schema object

Note that Input.DataFormat and Output.PreductionType also impact the REST API: we have endpoints of
format `/:predictionType/:inputDataFormat`

Furthermore, these are not simply internal details - users need to understand most of these in order to create/configure
 their pipelines. For example:
* Users need to set Input.DataFormat and Output.DataFormat for their serving configuration [Link](https://serving.konduit.ai/yaml-configurations)
    * Though oddly the [docs](https://serving.konduit.ai/yaml-configurations#yaml-components) show setting serving input
      data format, but the Java [ServingConfig](https://github.com/KonduitAI/konduit-serving/blob/4c29eef51985122cedc9d40dda3b79f400b3b9a7/konduit-serving-api/src/main/java/ai/konduit/serving/config/ServingConfig.java)
      has no such setting (only output config is present there)
* Users need to set Input.DataFormat and Output.DataFormat values for PythonSteps [Link](https://serving.konduit.ai/yaml-configurations)
* Users can optionally specify the Output.PredictionType during their server configuration
    * However, again users can set it in their YAML config according to the docs, but ServingConfig has no such Output.PredictionType
      configuration option
* If calling any of the endpoints manually (not via our clients), users need to understand both PredictionType and InputDataFormat


### Issues with the current design

There are multiple problems with the current design and API
* Verbosity: the PipelineStep interface has 28 (!!!) methods related to inputs, outputs and their types/schemas
* Confusing types: We've got DataVec Schema, SchemaType, Input.DataFormat, Output.DataFormat _and_ Output.PredictionType
 as things to think about as developers, or know about/set/configure as users
* PipelineStepRunner defines 3 separate transform methods: `Object... --> Writable[][]`, `Object[][] --> Writable[][]`
  and `Record[] --> Record[]`
    - Usage of Object/Object[] is highly ambiguous and error prone
    - Note that these are all largely equivalent in practice - Record is just a `List<Writable>` internally, and
      `BaseStepRunner.transform(Object...)` and `.transform(Object[][])` just unwrap/convert them and calls `transform(Record[])`
      internally anyway
    - Note 1: _none_ of these are suitable for sequences (without ugly hacks like embedding sequence as an NDArray)
    - Note 2: Even if we added DataVec `SequenceRecord`/`List<List<Writable>>` support for sequences, there will still be
      no way to mix sequence and non-sequence data (again, without ugly hacks), or have sequences of different lengths
* A DataVec-based API is a significant blocker for adding other programming languages - what happens if we want a pipeline
  step based on C#, C++, Swift, JavaScript - there's no DataVec API here
* When communicating between languages or VMs (if a pipeline was to be split between processes/VMs as in a microservice),
  a DataVec-based API adds significant serialization overheads
* No support for dynamic schemas or optional return values (for example in classification: optionally return probabilities or not based on user request) 
* Writing custom (Java) pipeline steps requires users to be familiar with DataVec's APIs (as well as conversion to/from INDArray, etc)
* Input.DataFormat and Output.DataFormat contain language-specific features (NUMPY, ND4J)
* The Input.DataFormat and Output.DataFormat are restrictive: for example, there's no nice/easy way to return a String,
  an arbitrary byte[], or an image (for a segmentation model), at least without hacks like wrapping them in and NDArray
* Multi-output networks are not really supported. For example, what if I have a multi-task classification + regression model?
  A single PredictionType setting doesn't work here 
* Invalid combinations of configuration are easy to produce - what does YOLO prediction type on a JSON or ARROW Output.DataFormat
  mean?
    - Similarly, the Python client is not able to handle a Output.DataFormat of ND4J [Link](https://github.com/KonduitAI/konduit-serving/blob/825a10d7ba356b502a6a743c0532b0828546a365/python/konduit/client.py#L217-L218)
* No way to return metadata to users


In short, there are issues with usability, performance, maintainability and support for some use cases in the current design. 

### Requirements for a new API and data formats

Suppose we redesign the API for handling data. This will have consequences throughout the codebase, which changes impacting:
* Pipeline and PipelineStep APIs
* YAML/JSON Configuration (serving, client, and steps)
* REST Endpoints
* Client API

**Requirements**

* Extensible to other languages - can in principle allow interop with languages we might one day need to deploy, such as C#, C++, JavaScript, Matlab, etc
* Needs to support efficient serialization/deserialization (ideally, zero copy where possible)
* Suitable for a microservices type split of pipeline steps (i.e., some steps run in different processes/containers)
* Efficient for 'monolithic' deployment options
    - i.e., we're not paying a serialization/deserialization or memory cost when everything is running in the 1 JVM (like now)
* Suitable for use in other communication approaches and deployment scenarios (such as GRPC, MQTT for IoT, Kafka, etc)
* Flexible - doesn't restrict use cases to only those explicitly built-in to the library
* Supports optional values in and out
* Good usability
* Easier to write and maintain pipeline steps
* JSON serializable 

Nice to have:
* Metadata support (optionally present or absent)
* Supports batches (more than one record/example in an object)
* Binary serializable, suitable for long-term storage - for example, recording values for compliance or manual labelling purposes
    - Long-term storage needs schema evolution / backward compatibility (i.e., can load old serialized values in new library versions)
    - Efficient binary storage
    - Readable by multiple languages would also be nice (for example, write from Java, read in a UI or Python) 


## Proposal

Our starting point:
1. Remove DataVec entirely in the PipelineStep and PipelineStepRunner APIs
2. Remove the concept of pre-defined, fixed schemas for PipelineStep and PipelineStepRunner

With regards to (1), the current pipeline definition of `Record[] --> Record[]` is replaced with something else.
It is proposed to call this the `Data` class - and thus pipelines are defined as `Data --> Data` operations.

There are two aspects to consider here: *API* - what users and developers will interact with when writing pipeline steps,
and *Storage* - the actual data structure used to store and serialize `Data` instances.
After discussing these, a number of other aspects of the API/codebase will be discussed.

**API**

The `Data` class API is proposed to have Map/Dictionary-like semantics. That is, `Data` is proposed to be *map-like* -
 in that it holds a set of key-value pairs. Keys are String type only. Values are one of a pre-defined datatypes (see below). 

Each `Data` instance would hold one example; batches are represented by arrays or lists of `Data` (i.e., `Data[]` or `List<Data>`).
This allows for dynamic schemas, and different schemas between different examples in a batch.

Dynamic schema example: image classification. One example in a batch inference request may request just the predicted class
 (schema: `{class: String}`) whereas another example may request both the class and the probabilities (schema: 
 `{class: String; probabilities: NDArray}`). This also allows for optional/missing input values.


`Data` is similar to a data-frame type design (in that different "columns" (map entries) can be one of a set of predefined data types),
however, unlike a DataFrame it does not (a) allow multiple examples, and (b) can have different schema per example/record.


Values are proposed to be one of the following types:
* Value types:
    * NDArray
    * String
    * Bytes
    * Image
    * Double
    * Integer (int64)
    * Boolean
* Collection types
    * Data (i.e., nested Data instances are allowed - if possible given the limitations of the chosen storage/data structure)
    * Array/List<ValueType> - including multi-dimensional lists/arrays

Additionally, `Data` will have metadata support, with the same datatypes as the standard types above.
In terms of implementation, metadata is just a nested Data instance under a dedicated key (perhaps `__metadata__` or something).

Note that these data types can all be easily converted to and from JSON and binary formats also.
NDArray and Images have multiple possible JSON representations - raw text as multi-dimensional JSON array or base64 bytes for NDArray,
or NDArray / base64 image file bytes (jgp, png, etc) for images.
Clients could easily specify the NDArray and image encoding types they want in the request (via client config and input `Data` metadata). 


Proposed `Data` API (Java):
* toJson() : String
* keys() : List<String>
* key(int) : String
* type(String) : DataType (enum)
* listType(String) : DataType (enum) - type for list entries
* Getters:
    * getArray(String) : NDArray
    * getString(String) : String
    * getList(String, DataType) : List<ValueType>
    * getData(String) : Data
    * etc
* "Or default" getters: (i.e., get if present, or use provided default value if not)
    * getArray(String, NDArray)
    * getString(String, String)
    * getBoolean(String, boolean)
    * etc
* Put methods - all of format (column name, value)
    * put(String, String)
    * put(String, INDArray)
    * put(String, byte[])
    * etc
* Metadata methods:
    * hasMetaData() : boolean
    * getMetaData() : Data
    * setMetaData(Data) : void
* Serialization
    * save(File) : void
    * write(OutputStream) : void
    * asBytes() : byte[]

Static methods:
* fromJson(String) : Data
* fromFile(File) : Data
* fromStream(InputStream) : Data
* fromBytes(InputStream) : Data
* singleton(String, Object) : Data
* builder() : DataBuilder (`Data d = Data.builder().put("myValue", v).build()`)
* fromMap(Map<String,INDArray>) : Data

Note that for dynamically typed languages like Python, we may simply have a single `get(String)` and `set(String, Object)`
method, instead of overloads.


Example custom PipelineStepRunner API and implementation (one method to replace all 3 of the PipelineStepRunner.transform(...) methods)
```$java
public Data transform(Data data){
   INDArray arr = data.getArray("features");
   INDArray modelOutput = myCustomModel.output(arr);
   return Data.singleton("output", modelOutput);
}
```

Example for custom SameDiff model with optional input values, and optional return values, for an attention model:
```$java
public Data transform(Data data){
   boolean withAttnWeights = data.getBoolean("returnWeights", false);     //User requested attention weights array as output, or not? 
   Map<String,INDArray> ph = new HashMap<>();
   ph.put("in", data.getArray("features"));
   ph.put("mask", data.getArray("mask"));
   String[] outputs = withAttnWeights ? new String[]{"output", "attention"} : new String[]{"output"}; 
   Map<String,INDArray> map = sd.output(ph, outputs);
   return Data.fromMap(map);
}
```


**Storage/Serialization (Data Structure)**

The `Data` API should be an interface - we can (and should) have multiple implementations for underlying data structures/storage.

For Java, the main storage format for monolithic deployment scenarios (i.e., all running in the one JVM with no inter-process
communication like now) can be a simple Map<String,Object> type structure. This avoids paying any unnecessary
serialization/deserialization overheads.

For inter-process communication and persistent storage, we should use a format such as FlatBuffers or Protobuf.

These have advantages, including multi-language support, efficient creation/serialization, efficient space utilization,
 and support for zero-copy access of array data. 
Of course, the APIs for these languages are not user friendly, but as already noted, we'll have a API on top of the underlying storage,
so this is not an issue.

In summary:
* Use Map<String,Object> based storage for "within single JVM" use cases (such as current Konduit Serving monolithic deployments)
* Use FlatBuffers or Protobuf data structure when serialization or IPC is required
* Enable conversion between the two (internally) if/when required


So far, it appears that either FlatBuffers or Protobuf should be fine, for all of:
* REST endpoints
* MQTT endpoints
* gRPC endpoints (see [Protobuf](https://grpc.io/docs/guides/) and [FlatBuffers](https://grpc.io/blog/grpc-flatbuffers/))
* Serialization and long-term storage (both support schema evolution)



**Schemas and Validation**

At present, Konduit Serving implements some degree of schema validation - i.e., pipelines need to define a schema input/output
types, and these are checked.
However, runtime schema validation isn't as useful as it might at first appear. If something goes wrong:
* With schema validation: exception at runtime ("input data does not match schema for step X")
* Without schema validation: exception at runtime ("exception in step X: input data with key 'Y' was not found in input data")

Consequently, it is proposed that:
* Konduit Serving will allow dynamic schemas - pipeline steps may input return anything, including different `Data` keys
  and types for different examples
    * The individual pipeline steps are responsible for interpreting the format of their inputs
* Konduit Serving will not have runtime schema validation beyond the "each step checks it gets the required inputs"


**Post Processing and Output Configuration**

Currently, we have Output.PredictionType (CLASSIFICATION, YOLO, SSD, RCNN, RAW, REGRESSION) for output/prediction post processing.
`RAW` is the default setting - i.e., no post processing.

Currently, we have an `/:predictionType/:inputDataFormat` endpoint: for example, we get output from the `localhost:9000/CLASSIFICATION/NUMPY`
endpoint to pass NumPy data in, and get processed classification data out.
Note that the current `ClassifierOutputAdapter` returns _all_ information by default:
* Set of classifier labels
* Maximum predicted class index (as an integer)
* Full set of probabilities (as a double[])


Under this proposal, we would have a similar post-processing mechanism, but with a different design.

Consider for example image classification. It's image in, and we want to return one or more of: (depending on the user's request)
* predicted class
* probabilities array
* top N classes with their probabilities
* list of class labels

The following is proposed:
* Output.PredictionType is removed entirely from the API
* The `/:predictionType/:inputDataFormat` endpoint is removed entirely from the API
* Keep the existing output adapters, but refactored as pipeline steps (with some additional configuration)
    - Add support for post-processing a subset of outputs (especially useful for multi-task / multi-output networks)
    - Add support for configuring default behaviour (such as, for classification: "predicted class + probabilities by default")
* Add the ability for pipeline steps to get the initial input metadata
    - Used to allow users to request which of N output formats they want, at any pipeline step

An alternative would be to have the existing output adapters as some different type of class; however, having them
as a pipeline step may be simpler for users (just one type of thing to learn about and use; plus it's more flexible).

Example of a classification output adapter `transform` method:
```$java
public Data transform(Data data){
   INDArray output = data.getArray("output");
   Data inputMetaData = inputMetaData();                //Method defined in BasePipelineStepRunner; everything extends BasePipelineStepRunner
   boolean retProbabilities = inputMetaData.getBoolean("return_probabilities", config.isReturnProbabilities());
   boolean retClasses = inputMetaData.getBoolean("return_classes", config.isReturnClasses());
   ...

   Data ret = Data.create();        //Note Data in an interface hence can't do "new Data()"
   if(retProbabilities)
        ret.put("probabilities", output);
   if(retClasses)
        ret.put("classes", getClasses());
   ...
   return ret;
}
```

There are a few features of note here:
* The input metadata (i.e., metadata that comes from the _original user request_) is available in _all_ pipeline steps,
  no matter which or how many steps precede it (or how the deployment is split into separate microservices/processes etc)
* The default configuration for a step can be set by the user
* When applicable, the user can override the configuration for a step directly from the client (internally via metadata settings)
    - This allows for optional return values to be configured from the client


**Impact on Endpoints**

Under this design/proposal, we would no longer have "type specific" endpoints - just the one `predict` endpoint.
* Binary - `application/octet-stream` MIME type in/out - Protobuf/FlatBuffers encoded `Data` object
    - This would be the main method when using any of our built-in clients (handle the conversion to/from `Data` internally)
* JSON - `application/json` MIME type in/out - JSON encoded `Data` object
    - Secondary method if users are interacting with Konduit Serving manually (without one of our clients), or need
      JSON only for some reason

The current `/:predictionType/:inputDataFormat` endpoints would be removed.

**Refactor Model Execution**

As noted earlier, the current machine learning model class heirarchy is complex in Konduit Serving:
`ModelStep --> InferenceExecutionerStepRunner --> XInferenceExecutionerFactory --> XInferenceExecutioner`

I proposed to flatten this to into heirarchy into simply 2 levels:
* ModelStep
* A set of XStepRunner classes (one for DL4J, SameDiff, TensorFlow, ONNX, etc) based on InferenceExecutioner + utilities
  to do `Data` to INDArray[] / `Map<String,INDArray` etc conversion 

Normally a change of this magnitude should be done in a separate PR, but given the scale of the other API changes, I
suggest we do this at the same time as the `Data`/API changes. The reason is simple: I don't want us to waste time getting
the existing heirarchy (ModelStep, InferenceExecutionerStepRunner, etc) working again with the new `Data` API, only to remove
that newly refactored code right away.

## Consequences 

### Advantages

* Considerably simplified API for pipeline steps (fewer methods, complications from schemas and datatypes, etc)
* Greater flexibility, extensibility, performance, etc
* Support for sequences (not possible at present) and combinations of sequence and non-sequence data
* Easier development and maintenance of new pipeline steps - for both Konduit developers and users
* Will make some future enhancements easier, including:
     - Running pipeline steps in different processes/containers/hosts (i.e., non-monolithic deployments)
     - Input/output recording

This proposal should satisfy all of the requirements mentioned earlier:
* Extensible to other languages - via probuf/flatbuffers encoding
* Suitable for a microservices type split of pipeline steps
* Needs to support efficient serialization/deserialization (ideally, zero copy where possible)
* Efficient for 'monolithic' deployment options - via Java map-based `Data` implementation without serialization costs
* Suitable for use in other communication approaches and deployment scenarios (such as gRPC, MQTT for IoT, Kafka, etc) - via JSON and binary encoding
* Flexible - doesn't restrict use cases to only those explicitly built-in to the library - `Data` should be flexible enough to support most types of returned values 
* Supports batches (more than one record/example in an object) - partial, via Data[] 
* Supports optional values in and out
* Good usability - yes (IMO)
* Easier to write and maintain pipeline steps
* JSON serializable (ideally YAML serializable also)
* Metadata support (optionally present or absent) - for both input/requests, and output
* Binary serializable, suitable for long-term storage - via Flatbuffers or Protobuf, which TBD
  
### Disadvantages

* Schema mismatches can only be determined at runtime
    - i.e., one step outputs `{"array" : NDArray}` but next step expects `{"feature0" : Double; "feature1" : Double}`
    - However, this is no worse than current design (schema checks trigger only at runtime anyway)
* Potential for mismatch between pipeline steps that needs to be mapped manually by the user
    - Example: A preprocessing step produces arrays with names {"in", "mask"} but the model expects arrays with names {"inputArray", "maskArray"}
    - This can probably be solved by a combination of guessing (single input array -> not ambigious even if names differ) or some extra user config
* `Data` objects are single example only, which may introduce ambiguity
    - users could still sneak batches in a single `Data` object via NDArray, or via `List<Double>` as a batch of doubles etc
* Potential performance overhead for batching data for execution `Data[] -> INDArray` (no worse than current `Record[][]` being batch of `Record[]` approach however)
* Need to write converter utilities for to/from `Data`
    - This is no worse than the current to/from `Record[]` conversion however


## Discussion


> Micro service deployment / remote endpoints: What is the background here? Is it planned to allow a single konduit-serving deployment to be spread along multiple containers? Or is this more for the case where we'd like to ship off something to an already existing and deployed service, so this effectively becomes some sort of RPC call?

Yes, split between multiple containers is something we'll need. Not frequently, but some deployment situations will require this.

Consider for example the following scenarios:

* A pipeline with 2 models - a face detector (run on an edge device) and a powerful face verification model (run on a powerful remote server)
  The idea is that the edge device does the initial detection, and only if a face is detected pass on the data to a remote server.
  We can't and/or don't want to run the entire pipeline on one machine.
* A pipeline that requires multiple docker containers for isolation
  We can't embed _every_ language in KS/Java like we do with Python; in some cases, we'll need to split things up into multiple docker containers.
  C#, Matlab, R, Swift, etc come to mind here as things we might deploy in a separate docker image, even if all images are run on the one machine.
* A situation where we need non-linear scaling of pipeline steps
  For example: The first step does filtering (like in the face detection example) and we need 20 multi-core CPU machines to do that filtering. Then only 0.1% of the cases actually make it to one of 2 GPU machines; so our deployment is (20 CPU + 2 GPU) not (20 GPU)

Both aren't something we need right now, but I think it's inevitable that we'll run into some deployment scenarios where the current "single process monolithic" design won't work.

> Schema Validation: Wouldn't it be possible to make steps queryable for what input and output they want? Having a definition time checker for schema validation would be quite nice. But I guess that could be added in another step, and doesn't have to be built in right now.

That's not a bad idea, but it would have to be optional... not all steps will really know their inputs/outputs until runtime. And some inputs/outputs will be dynamic (most won't be though).
For example, in model steps - we won't know the placeholder/input names (or even number of inputs/outputs) until we load a specific model.
But yes, let's revisit this.


> Metadata: I like that input meta data is available at all steps. But wouldn't it be nice to have meta data be addable by the steps?

Yes, I don't think I covered that, but I something like that in mind too.
So for example an image loading step could return metadata about the original image (format, dimensions, filename, etc) back to the user, no matter how many other steps are present after the image loading step.



> Mapping between model steps: In the end you acknowledge that naming mismatches may be a problem, maybe the easiest way instead of guessing is to make this explicit, and have a reusable "MappingStep" that just maps one name to another?

For the ("input", "mask") vs. ("features", "featureMask") type labeling problem - yes, I don't see any alternative to that (other than the unambiguous cases like single array input, etc)
