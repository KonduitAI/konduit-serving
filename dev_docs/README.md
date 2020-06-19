# Developer Documentation

These docs are designed to explain the architecture of Konduit Serving (for the "new" API as of 05/2020).

See `ADRs/0003-Pipeline-API_Rewrite.md` for background and motivation on the current design.  
See also: dev_docs/BuildToolReadme.md for details on the Konduit Serving build tool (reading this first is recommended).
See also: dev_docs/ClientsAndDocsGeneration.md for details on how we're generating client code along with its reference documentation.

## Core classes:

* Pipeline: An *interface* that defines a `Data -> Data` transformation made up of one or more pipeline steps.
    - Note: There are 2 types - `SequencePipeline` (a stack of operations) and `GraphPipeline` (a directed acyclic graph
      possibly including optional branches). Analogous to MultiLayerNetwork/Sequential and ComputationGraph/Functional in DL4J/Keras.
* PipelineStep: Also an interface defines a `Data -> Data` transformation. Consider these the building blocks for building a Pipeline
    - Model inference, ETL, data transformation, etc are all implemented as a PipelineStep.
* PipelineExecutor: An interface that can execute a Pipeline. Made up of multiple PipelineStepRunners - one for each PipelineStep
* PipelineStepRunner: An interface for running a PipelineStep
* PipelineStepRunnerFactory: For creating PipelineStepRunner from PipelineExecutor. Explained in more detail later.
* Data: A map-like store of key-value pairs.
    * Keys are always Strings, values are one of: {String, int64 (long), double, boolean, NDArray, byte[], Image, bounding box,
     List[T], Data} where T is any of these types.
      Note that we allow nested Data and List objects.
    * Data instances also have optional metadata, stored as a nested Data instance 
    * Note NDArray and Image are special cases here, discussed in detail below

**Key modules - New API:**

The list below briefly describes those implemented so far (as of 04/06/2020)- there are many more to be added.

* konduit-serving-pipeline: Core API for pipelines and local execution
    * Note: very few dependencies. No ND4J, DataVec, Python, TensorFlow etc in this module. Only Java API and baseline Java functionality.
* konduit-serving-models: Parent module for each of the neural network model types
    * konduit-serving-deeplearning4j: Deeplearning4j model support + Keras support (via DL4J Keras import)
    * konduit-serving-samediff: SameDiff model support (and soon TensorFlow frozen model support)
    * konduit-serving-tensorflow: TensorFlow Frozen Model and SavedModel support via TensorFlow Java API
    * (soon) konduit-serving-onnx
    * (soon) konduit-serving-pmml
* konduit-serving-data: Parent module for data and datatypes
    * konduit-serving-nd4j: Mainly NDArray integration/functionality for ND4J. Used also by deeplearning4j and samediff modules
    * konduit-serving-image: Image conversion functionality using JavaCV; also Image -> NDArray functionality, and a bunch of utility
      steps for showing images, drawing segmentation masks on images, drawing/croping from a grid, and drawing bounding boxes
* konduit-serving-io: Parent module for I/O functionality - sensors, cameras, etc - and maybe later things like HDFS, S3, etc
    * konduit-serving-camera: Steps related to capturing data from device-connected cameras (WIP)
* konduit-serving-metadata
    * konduit-serving-meta: collects metadata from all modules - what. More on this later.
    * konduit-serving-annotations: Annotations: `@JsonName`, `@ModuleInfo`,  `@CanRun` and `@RequiresDependencyAny`, `@RequiresDependencyAll`, `@InheritRequiredDependencies`.
      More on annotations later, and see also build tool readme.
* konduit-serving-build: A tool for producing artifacts suitable for serving and execution of pipelines- i.e., uber-jars, docker
  images, standalone .exe files, deb/rpm files, and more.
* konduit-serving-metrics/konduit-serving-prometheus: Prometheus-based metrics support (/metrics endpoint)
* konduit-serving-vertx: Baseline Vert.x serving functionality
* konduit-serving-vertx-protocols
    * konduit-serving-http: HTTP serving with Vert.x
    * konduit-serving-grpc: gRPC serving with Vert.x
    * konduit-serving-mqtt: (placeholder as of 04/06/2020) MQTT serving with Vert.x

Note that as of 06/2020 the remaining modules will be removed in the near future

**Key design philosophies:**

* Deployment scenario agnostic
    - That is: we won't privilege REST/HTTP (or even model server type design) over other deployment scenarios. If a user
      wants to deploy a Pipeline within say a Kafka pipeline, Spark job, or embedded in an Android app - we should allow
      that (within reason, given limitations for those deployment scenarios). That is: REST/gRPC/etc is _built on top of_
      the pipeline functionality - but is optional and separate to the actual pipeline definition, implementation and execution.
* Modularity
    - One (model etc) format per module - users should get only the modules/dependencies they need - and nothing else (i.e.,
    deployments should be as small as possible dependency-wise unless the user explicitly chose to include have extra dependencies)
* Extensibility
    - New formats, model types, modules etc - it should be possible for users to add any/all of these without any modifications
     to the konduit-serving repository.  
* Separation between pipeline definition and pipeline execution
    - See PipelineStep section later. But we want to enable one pipeline step (configuration) to be run by different executors.
      For example, a TensorFlow model - as defined by a the exact same TensorFlowStep configuration could be run by any of
      the TensorFlow, SameDiff, or ONNX executors - without any modification to the configuration itself (other than maybe
      prioritizing which to use if multiple are available).  
      Put another way: there is a many-to-many relationship between PipelineStep (configuration) and PipelineStepRunner (execution)
* Suitable for both uber-jar type deployments and OSGi deployment scenarios
* (Mostly) language agnostic
    - Pipeline JSON/YAML specification is language agnostic: Though the Konduit Serving core is Java-based, the pipeline / pipeline
      step definitions should not contain any Java-specific configuration. Languages needed for executing some of the PipelineSteps
      themselves aside, in principle there would be nothing stopping us executing the exact same pipeline definition (defined
      as JSON/YAML) in say C++, C# or Python.
    - Data interface is also language agnostic - it should be possible to implement/use Data instances in nearly any language
      we are likely to see in enterprise. 
* Efficiency and performance
    - Avoid unnecessary copies, serialization/deserialization, etc

## Pipeline

As noted above - `Pipeline` is an interface that defines a `Data -> Data` transformation. A Pipeline is usually made
up of multiple separate steps - one or more `PipelineStep` instances.

There are two types of Pipelines (both with the same API - Data in, Data out) - SequencePipeline and GraphPipeline.  
For graph pipelines, see ADRs/0004-Graph_pipelines.md.  

Though the Java API for execution of these is the same, the difference between the two is:  
(a) internal - GraphPipeline allows branching, and conditional operations  
(b) JSON/YAML format definition - users define a list for SequencePipeline, or a Map/Dictionary for GraphPipeline

For many use cases, SequencePipeline would be perfectly adequate.

GraphPipeline is intended to be used for use cases such as the following:
* Conditional execution: i.e., route input to one of two (or more) models based on some condition
    * For example: One model per country, or for an A/B split testing of different models, etc
* Complex graph structures
    * For example: user inputs image, which is fed into an object detection model, post processed to a list of bounding boxes,
      then the bounding boxes are drawn onto the original input image and returned. Such a structure would be difficult
      to implement using a SequencePipeline



## PipelineStep, PipelineStepRunner, PipelineStepRunnerFactory

As noted earlier, one of the key design philosophies is a separation between PipelineStep definition and pipeline execution.
That is: a user can define a PipelineStep and have it executed by different executors.

An example: Suppose a user defines a PipelineStep for performing inference on a TensorFlow model (TensorFlowStep).
Under this design, we could allow this same step - exact same configuration and model file, no modifications to either -
to be executed by any of the following libraries / graph executors: (and more)
* TensorFlow
* SameDiff
* ONNX (after conversion - perhaps done automatically/internally)
* TVM

That is: for a user to switch their model from one executor to another - all they have to do is make that executor type available,
and (if multiple executors are available that can execute the model) perhaps setting the priority for which executor should
be used.

This is the motivation for the split into the 3 interfaces - PipelineStep, PipelineStepRunner and PipelineStepRunnerFactory:
* PipelineStep: Defines the configuration only
* PipelineStepRunner: Used for executing the pipeline step (including loading model/resources, etc)
* PipelineStepRunnerFactory: Use for checking execution support that a PipelineStepRunner has for a PipelineStep, and
  for creating the PipelineStepRunner for executing the PipelineStep


In the old API design, there was exactly a 1:1 mapping between PipelineStep configuration and execution.
i.e., there was exactly one way of executing a given step.

At runtime, the PipelineStepRunnerFactory instances are loaded and stored in the PipelineRegistry class.

## Data

The `Data` interface defines the key abstraction for input and output. It is language agnostic, in that the API contains only
generic datatypes found in most/all programming languages - not just in any particular language or library.
That is, the idea is to have the same Data interface in every language we want 
The Data interface will also be present for the client implementations (though JSON in/out will be possible too).

Again, the supported datatypes include: (see `ValueType` enum):
* NDARRAY
* STRING
* BYTES
* IMAGE
* DOUBLE
* INT64
* BOOLEAN
* DATA
* LIST
* BOUNDING_BOX

`JData` is the Java Map-based implementation of the `Data` interface - i.e., literally it has a `Map<String,Value<T>>` internally,
where `Value<T>` is a trivially simple object to hold objects of different types.

We also have a Protobuf-based `Data` implementation - ProtoData that stores data in protobuf form.
Protobuf - aka Protocol Buffers - is a widely used binary data serialization format.


Why protobuf? 4 reasons
* Format is language independent (i.e., create a file/bytes in any language, read in any other language)
    - Also: define once and generate ser/de code in each language (though we need custom/non-generated wrapper code for UX and Data compatibility)
* Performance
    - considerably faster to encode/decode than formats such as JSON
    - Can allow for zero-copy deserialization in some cases
* Space efficient - much more so than other formats such as JSON (1-10x depending on the format)
* Enables gRPC (which Konduit Serving supports as an alternative to HTTP/REST)

A key point regarding Data: Any time we need to do any serialization, network transfers, saving/loading to/from file etc - we will use the
protobuf format. i.e., the protobuf-based Data implementation is considered the canonical representation for anything other
than temporarily when running within a single JVM.

In practice, we'll still use JData a lot for monolithic Java deployments. The reason for this is that we don't want to pay the
serialization/deserialization cost on every pipeline step - so we'll use JData as a holder, and (automatically, internally)
convert JData to ProtoData whenever we need to do IPC, serialization or sending binary data over the wire.

Similar to how the ProtoData (protobuf) is the canonical format for Data in binary, we will have only one JSON format for
all Data implementations. i.e., JData, Protodata (or any possible future Data implementation) will all produce the exact
same JSON/YAML. The JSON ser/de for Data is implemented in the konduit-serving-pipeline classes DataJsonSerializer and
DataJsonDeserializer.  

## NDArray and Image

NDArray and Image are interfaces for n-dimensional arrays and images in Konduit Serving.

We have 5 main design challenges for implementing NDArray and Image support:
* Multiple types: Many different possible formats/types exist for both Image and NDArray 
* Extensibility: We want to support many different NDArray and image formats - more than just what is defined now in Konduit Serving
* Modularity: We can't have all dependencies in konduit-serving-pipeline
* Usability: we don't want to burden users with manual conversion to/from some "generic but practically useless other than storage" format
* Performance: In principle we could just convert everything to some canonical format and be done with it - but we don't
  want to pay the performance overhead. Suppose 2 pipeline steps both produce/use INDArray - why should we pay the
  `INDArray -> SomeFormat -> INDArray` conversion cost?

NDArray and Image are interfaces. The design for NDArray and Image is that they are both just an "object holders with converters".
That is, the only thing the user can do is say "give me the content of this NDArray/Image in this format (automatically converting
if necessary)".

The main downside is that we (as developers) need to define ahead of time format converters - for example, how an INDArray can
be converted to a `float[][]`, and vice versa. This requires a bit of tedious boilerplate for us - as there are many
possible conversions to implement.

However, once we have done that, the result is reasonably good usability:
```java
float[][] myFloatArray = new float[][]{{1,2},{3,4}};

NDArray ksArr = NDArray.create(myFloatArray);

INDArray myINDArray = ksArr.getAs(INDArray.class);
```

Internally, the `ksArr` NDArray just stores the `float[][]`; if the user had instead done `ksArr.getAs(float[][].class)` no
conversion (or performance/memory overhead) would be done. Conversely, for the `getAs(INDArray.class)` the appropriate
`float[][] -> INDArray` conversion happens automagically internally.

Note that types are fully extensible - if a user wants to add some other library - it's possible for them to add a few
classes and converters so that `NDArray.create(someUserCustomType)` and `NDArray.getAs(...)` just works.

We also implement simple multi-step conversion for easy extensibility. That is, suppose the user wants to do `NDArray.create`
for some custom type X. Instead of having to write conversion for `X -> float[]`, `X -> INDArray` etc etc for all possible
types that Konduit serving supports, they simply need to write two converters:
* For Images: format to/from PNG
* For NDArrays: format to/from SerializedNDArray

Then, when no direct X -> Y converter exists, we simply do X -> (PNG/SerializedNDArray) -> Y which will work with any combination
of types for X and Y. 


In practice, this is implemented via instances of the following interfaces:
* ImageConverter - for converting one Image format to another
* ImageFactory - used in `Image.create(Object)` to create Image object wrapping that image type
* ImageFormat - specifies a format with more detail than just a class name (for example, format configuration)
* NDArrayConverter - for converting one NDArray format to another
* NDArrayFactory - used in `NDArray.create(Object)`
* NDArrayFormat - specifies a format with more detail than just a class name (for example, format configuration)

The actual converters and creators are loaded (using Java ServiceLoader mechanism) and stored in the following classes:
* ImageConverterRegistry
* ImageFactoryRegistry
* NDArrayConverterRegistry
* NDArrayFactoryRegistry 


Serialization and IPC: While we are within a single JVM, we don't necessarily need to do any conversion - for
example, all of our pipeline steps could use `INDArray` or use `float[][]` etc - that's fine. But what happens when we
want to save (or transmit over the network) a Data instance? Clearly we need some language-agnostic format for storing
the NDArray and Image values, no matter what type they are internally at present.  
For NDArray, this format is straightforward - we store a C-order buffer of values (with some fixed endianness), a type
enum and a shape array (as `long[]`). This is implemented in the SerializedNDArray.  
For images - we currently standardize on PNG format for images. While something like JPG format would seem like
a reasonable option - one issue is that it is lossy - i.e., there's no guarantee that we'll get the exact same image out
of serialization/conversion as went in (only a close approximation). That might matter for some use cases.
PNG is lossless, but has the downside of being significantly larger for 'natural' images (i.e., typical photos) that will
 frequently be fed into a neural network.


For JSON serialization, we convert all arrays to SerializedNDArray - that simply stores the type, shape, and the data
as a C order ByteBuffer. Then we can serialize in a simple NDArray format that (for most of the "standard" datatypes)
should be supportable nearly anywhere, without extra dependencies.
For things like ND4J, the ByteBuffer can point to off-heap memory so should be zero-copy under most circumstances. 
Internally, we basically do:
```java
NDArray myArr = NDArray.create(new float[]{1,2,3});
SerializedNDArray sArr = myArr.getAs(SerializedNDArray.class);
long[] shape = sArr.getShape();
NDArrayType type = sArr.getType();
ByteBuffer bb = sArr.getBuffer();
```
All modules that define an NDArray type should implement conversion to SerializedNDArray, so we can do JSON and Protobuf
serialization from any format - in a standardized form.

As of 07/05/2020 supported formats for images include:
* Png
* Jpg
* Bmp
* BufferedImage
* JavaCV Mat    (konduit-serving-javacv)
* JavaCV Frame  (konduit-serving-javacv)
More formats will be added in the future as they are needed.

As of 04/06/2020 supported NDArray formats include:
* SerializedNDArray (Konduit Serving serialization/interchange format)
* Rank 1 to 5 arrays for all Java primitive types (x[], x[][], x[][][], x[][][][], x[][][][][] where x is any java primitive type - int, long, short, float, etc)
* INDArray      (konduit-serving-nd4j)


### Image to NDArray

If we want to make predictions based on an Image using a neural network, we need to convert it to an NDArray, and likely normalize it.

There are two main classes (both in konduit-serving-image)
* ImageToNDArrayStep - Image -> NDArray as a pipeline step 
* ImageToNDArray utility class

Both of these take an ImageToNDArrayConfig to specify the format, which allows configuration of:
* The output NDArray height/width
* The output NDArray datatype (i.e., float, double, etc)
* Whether a leading dimension (of size 1) should be included or not - i.e., [c,h,w] or [1,c,h,w] (or similar for channels last) 
* How to handle the situation where image aspect ratio doesn't match the output NDArray aspect ratio
* Whether to use channels first or channels last for the NDArray
* The chanels layout - RGB, BGR, etc
* The normalization to apply (build in methods: none, scale 0-1, subtract mean, standardize, inception or VGG)



## JSON Serialization / Deserialization

JSON ser/de was discussed earlier. This section is about JSON ser/de for the various configuration classes in Java, such
as Pipeline, PipelineStep, etc.

The choice of modularity makes JSON serialization somewhat more challenging.

Normally we'd just use Jackson to do something like this in the main/API module:
```java
@JsonSubTypes({
        @JsonSubTypes.Type(value = PmmlStep.class, name = "PmmlConfig"),
        @JsonSubTypes.Type(value = SameDiffStep.class, name = "SameDiffConfig"),
        ...)
})
@JsonTypeInfo(use = NAME, property = "type")
```

How do we do JSON ser/de for configurations if we can't refer to the implementation classes in konduit-serving-pipeline?
That is, `SameDiffStep.class` obviously won't compile if there isn't a `SameDiffStep` class in that module (or as a dependency).
And how do we make it user extensible if they want to add their own modules?

The practical answer is simple: we have introduced our own `@JsonName("...")` annotation (defined in konduit-serving-annotation)
These annotations should be placed on things that are intended to be JSON serializable (that have to deal with JSON subtypes)
PipelineStep implementations being the main use for these annotations.
For example, placing `@JsonName("MY_STEP")` on a PipelineStep means that it's JSON representation will contain `"@type": "MY_STEP"`
which will allow us to map back to the Java step.

This custom annotation-based approach is implemented in practice by automatically generating one class and two metadata files:
1. A class that implements JsonSubTypesMapping
2. A Java Service Loader file (META-INF/services/ai.konduit.serving.pipeline.api.serde.JsonSubTypesMapping) that points to
   the class generated in step 1.
3. A META-INF/konduit-serving/ai.konduit.serving.annotation.json.JsonName file - see the annotations section later.

All 3 of these files are generated automatically without the developer's intervention - the developer simply needs to add
the `@JsonName` annotation (and a `@ModuleInfo` annotation somewhere in the module - more on this in the next section).

This ServiceLoader based implementation should also be adaptable for use in an OSGi deployment context (which we will be
supporting in the future).
 

At runtime, the JSON subtypes are loaded (and registered with Jackson) in the ObjectMappers class.

As for the actual JSON format - this is basically as follows (taken from DataJsonTest.testBasic):
```
 ----- NDARRAY -----
{
  "myKey" : {
    "@NDArrayType" : "FLOAT",
    "@NDArrayShape" : [ 3 ],
    "@NDArrayDataBase64" : "AAAAAD+AAABAAAAA"
  }
}
 ----- STRING -----
{
  "myKey" : "myString"
}
 ----- BYTES -----
{
  "myKey" : {
    "@BytesBase64" : "AAEC"
  }
}
 ----- IMAGE -----
{
  "myKey" : {
    "@ImageFormat" : "PNG",
    "@ImageData" : "iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsQAAA7EAZUrDhsAAAPcSURBVFhH7VfZK3VRFF/XNRSZyTyHyAOSUqZHpRCRPCjlzZM/QH3/AU+G4oHyJK9KmQohJSKZZxkjmcf1rbWce13Hce5xP3Vfvl/92ufss4ffXnuttfcxAQASnYKkpCRwUZ6dhv8CnC7gn5zQZDIB4nv39PR08PHxAX9/fzg+PoalpSUoKyt7dzQXF5ienoaZmRk4Pz+X9gz+ZkgAD8ATeXt7ywReXl7CqKgoeHl5gcfHR5ksODgYQkNDYXFxEQYHB6G5uRliY2NljJaWFmhra4O1tTV5Z9gVwCt0dXWFyspKmYgHS0tLg7i4OAgLC4OEhASl5QeOjo5gYWEBrq+vIScnR6yxvr4Ora2tMD8/Dzc3N0pLAwI8PDwgIiICNjc3lZrPeHt7E7KFGFx2dnZCf3+/iL+/vxdBt7e3cHBwIG1swQIYLECTNKCUr6+vtAMfsH2nVeHw8LDyhtjQ0PBlnO9IAtBMD3+ImqDxpFxZWZFV9PX1wcPDAyQnJ0vJe52RkQE9PT3Q0dEhfjE3Nwerq6vSzx4CAwOl/KJMTV9fX0xMTMSAgACsqqrCra0t3N7extraWmsbs9mM0dHR0ta2rx7ZAobywNXVlfjBxcUFnJ6eioP5+flBamqq0gKAtgX29vak7U9gOBGxszGWl5clxHh76urqrGZ0FLo+oAX2BV5lSEgIZGdnW8OKTC8Wsgg1AsM+oCZHR2FhIVKMIzkjjoyMYFdXF1I2RApdpBDU7Kcm+wCV2h/t0dPTE0tLSyX0LOju7sbc3FwRodVHzX8SYCGlZ5yYmFAkIFIWRPINaw7R468IYLKI4uJi3NnZERGHh4fY3t6u2daWvybAQjrxRABjf38fKUw121loOA/ogT25uroaGhsbgRKR1N3d3cHu7q6ErD04dB/ggyYyMlJOu7y8PMjKyoKgoCCIiYkBNzc3SVRDQ0NQXl6u9NAGWUDKL6bRI+93QUEBNjU14eTkJD49PSlGR6QjGzc2NpBOQ0OHEm+BIQtQnpejmW89vOKKigrIzMyUegYnJ07Ds7OzMDAwAJQX4OzsTL7pwa4FOJTItJiSkoJFRUVIg+Pz87OyXkS6CSFlQhwdHcWamhrNMfRoNwro1oNjY2N4eXmpTPkBFjI1NYX5+fmafY1QUwBPyolkfHwcyZuV6T6DLpxYX1//qZ8j1BRQUlIiq7YF7Sf29vZKvueJw8PDJRWr+/6Umk5Ig4tzcElmlpDjsKLsJifdycmJIQczAp7HoTzwW2ABTv0zEgtziHH2UoM/quv534D/E9T4rp6SFLi7uytv7yCXkusbb218fDz8Bd+qYeQWjEzlAAAAAElFTkSuQmCC"
  }
}
 ----- DOUBLE -----
{
  "myKey" : 1.0
}
 ----- INT64 -----
{
  "myKey" : 1
}
 ----- BOOLEAN -----
{
  "myKey" : true
}
 ----- BOUNDING_BOX -----
{
  "myKey" : {
    "@cx" : 0.5,
    "@cy" : 0.4,
    "@h" : 0.9,
    "@w" : 1.0
  },
  "myKey2" : {
    "@x1" : 0.1,
    "@x2" : 1.0,
    "@y1" : 0.2,
    "@y2" : 0.9,
    "label" : "label",
    "probability" : 0.7
  }
}
 ----- DATA -----
{
  "myKey" : {
  "myInnerKey" : "myInnerValue"
}
}
 ----- LIST -----
{
  "myKey" : [ "some", "list", "values" ]
}
```

Note that strings, bytes, double, int64, boolean and lists are basically as you would expect.  
The format for nested Data instances is identical to the non-nested ones.    
Bytes, is a special case - it's an object with a special protected key: `@BytesBase64`. Currently (and by default) we
will encode `byte[]` objects as base64 format for space efficiency; later we will allow "array style" byte[] encoding
(i.e., `"@BytesArray" : [1,2,3]`). Users are not allowed to add anything to a Data instance with these protected keys.

Image data is stored by default in PNG format, base64 encoded (this will be configurable eventually).

NDArray is a special case also: it in a JSON object with type/shape/data keys. Currently data is base64 encoded, but
we may allow a "1d buffer array" format in the future also.

Bounding boxes are also stored with special keys in either (cx, cy, h, w) format or (x1, x2, y1, y2) format, again with
special/reserved names to differentiate a bounding box from a Data instance (otherwise the JSON would be ambiguous). 

The full set of protected keys can be found on the Data interface. They include:
* @type (used for JSON subtype information)
* @BytesBase64
* @BytesArray
* @ImageFormat
* @ImageData
* @NDArrayShape
* @NDArrayType
* @NDArrayDataBase64
* @Metadata
* Bounding box: @x1, @x2, @y1, @y2
* Bounding box: @cx, @cy, @h, @w 

In practice, Data JSON serialization/deserialization is implemneted in the DataJsonSerializer and DataJsonDeserializer classes. 

## Annotations and Metadata Collection

The konduit-serving-annotation module defines a number of custom annotations:
* `@JsonName`: Used for defining JSON subtypes. All new PipelineStep implementations should be annotated with this. For example
   `@JsonName("MY_NEW_STEP")`
* `@ModuleInfo`: Used to provide the module name at compile time (and if ever necessary, at runtime also). Used mainly in
  conjunction with the other annotations such as JsonName, RequiresDependencies*, and CanRun.
* `@CanRun`: Should only ever be used on a PipelineStepRunner. It defines the types of configuration the runner can execute.
  Used mainly in the build tool to determine what modules we need to include to run a given pipeline.  
  For example, `@CanRun(MyPipelineStep.class)` or `@CanRun({SomeStep.class,OtherStep.class})`
* Optional dependency tracking annotations - these are used _only_ for dependencies that aren't included by default in a module.
  For example, ND4J backend dependencies, or dependencies that are applicable only to specific hardware devices / CPU architectures / OSs.
    * `@InheritRequiredDependencies(module_name)`: equivalent to "copy the annotations from the specified module". Note that
      the specified module doesn't need to be listed as an actual dependency of the module with the annotation. i.e., that
      inheritance is resolved in the build tool module, not at compile time.
    * `@RequiresDependenciesAll/RequiresDependenciesAny`: Used to specify a set of dependency requrements. i.e., "we need
      one of these" / "we need all of these".
    * `@Dependency`: nested only within a `@RequiresDependency*` annotation

For an example of the dependency requirement annotations, see Nd4jModuleInfo.
See also Javadoc for these annotations.

In practice, these annotations automatically (at compile time) write files on a per-module basis in META-INF/konduit-serving.
i.e.,:
* `@JsonName` writes a `META-INF/konduit-serving/ai.konduit.serving.annotation.json.JsonName` file
* `@CanRun` writes a `META-INF/konduit-serving/ai.konduit.serving.annotation.runner.CanRun` file
* The remaining annotations (dependency related) get aggregated into a `META-INF/konduit-serving/ai.konduit.serving.annotation.module.RequiresDependencies` file

As a general rule, an average user/developer won't ever be aware of (or need to be aware of) these files or the compile
time annotation processing. They just provide the dependencies and everything magically works.


Separately to the compile time generation, we have a mechanism to automatically aggregate the contents of those 3 `META-INF/konduit-serving/*`
files. The goal is to make the JSON, "CanRun" and dependency information available to the build tool (i.e., to konduit-serving-build)
without the need to have every single Konduit Serving module as a dependency within konduit-serving-build.
This "project-wide" aggregation is implemented in the konduit-serving-meta module (within konduit-serving-metadata parent module).
The konduit-serving-meta module produces an output JAR only 3 metadata files:
* `META-INF/konduit-serving/PipelineStepRunner` - the aggregation of all the `@CanRun` metadata files
* `META-INF/konduit-serving/JsonNameMapping` - the aggregation of all the `@JsonName` metadata files
* `META-INF/konduit-serving/ModuleRequiresDependencies` - the aggregation of the all the dependency annotation files

In practice this is implemented via (a slight abuse of) the Maven Shade plugin. Essentially, we are building an uber-jar
of all Konduit Serving modules with everything filtered out except the metadata files.
konduit-serving-build then depends on konduit-serving-meta, and can load those 3 aggregated files to get the metadata for
all modules without actually having any of the other modules as a direct dependency, or available at runtime.

For more details on the build system (and these files) see dev_docs/BuildToolReadme.md


## Adding a New Module

To reiterate: When adding a new module, no modifications to the konduit-serving-pipeline module are allowed. That means:
No new dependencies, new classes, code changes etc in konduit-serving-pipeline - nothing. The design was implemented
specifically to enable building new modules in isolation in a way that works with konduit-serving-pipeline.

So: What is actually involved in adding a new module?

*First*: The new module should have konduit-serving-pipeline as a dependency in order to use the API

If building on top of some other module, include it also. For example, `kondit-serving-deplearning4j` includes `konduit-serving-nd4j`.


*Second*: If a new PipelineStep is to be added, all of the following are required:
- Implement the new step, extending the PipelineStep interface
- (Usually) Add a new PipelineStepRunner, for executing the type of step you have just created
- (Usually) Add a new PipelineStepRunnerFactory, for creating your new PipelineStepRunner from your new PipelineStep interface
- Add a `@JsonName(...)` annotation on the new PipelineStep
- Add a `@CanRun(MyNewPipelineStep.class)` or `@CanRun({MyNewPipelineStep.class, SomeOtherPipelineStep.class})` annotation to the PipelineStepRunnerFactory
- Assuming a PipelineStepRunner was added, add a `resources/META-INF/services/ai.konduit.serving.pipeline.api.step.PipelineStepRunnerFactory`
  file, containing the fully-qualified path of your new PipelineStepRunnerFactory 

See for example: konduit-serving-deeplearning4j


*Third*: If a new Image or NDArray format is to be added (less common than new pipeline steps), all of the following are required:
- Add a new class for holding the Image/NDArray (extending BaseNDArray/BaseImage should suffice)
- Add NDArrayConverter / ImageConverter implementations to convert between different Image/NDArray formats (see for example NDArrayConverter in konduit-serving-nd4j)
    - For NDArray: the main (strictly required) one is conversion to/from SerializedNDArray - this is necessary for JSON
      and protobuf serialization/deserialization, and will be used as the intermediate format for conversion between arbitrary
      types that don't have direct (1 step) conversion enabled (i.e., X -> SerializedNDArray -> Y for any X and Y).
    - For Image: the main (strictly required) one is conversion to/from Png (i.e., `ai.konduit.serving.pipeline.impl.data.image.Png`)
      as this is used as the default format for both JSON and Protobuf serialization - and also used as the intermediate
      format for conversion between arbitrary image formats that don't have direct (1 step) conversion available (i.e., X
      -> Png -> Y for any X and Y).
      The reason PNG was chosen: PNG is a compressed lossless image format, unlike some alternatives such as jpeg. It does
      have a size overhead for natural images vs. jpeg, but in practice for deep learning we typically don't have very large
      input/output images so this is a secondary concern.
- Add an NDArrayFactory / ImageFactory (used within NDArray.create(Object) / Image.create(Object))
- Add a `resources/META-INF/services/ai.konduit.serving.pipeline.api.format.NDArrayConverter` (or `.ImageConverter`) file
  listing the fully-qualified class name of all of the new NDArrayConverter/ImageConverter implementations you added
  (one per line)
- Add a `resources/META-INF/services/ai.konduit.serving.pipeline.api.format.NDArrayFactory` (or `.ImageFactory`) file listing
  the fully qualified class name of the NDArrayFactory / ImageFactory that you added

See for example: konduit-serving-nd4j


A note on naming packages for new modules: The packages (that directly contain classe) should be unique, and are based on
the  module name.
For example, the kondit-serving-nd4j module (under konduit-serving-data) has classes in `ai.konduit.serving.data.nd4j`.
Similarly, konduit-serving-deeplearning4j (under konduit-serving-models) has classes in `ai.konduit.serving.models.deeplearning4j`.
It's fine to have sub-packages (i.e., any X in `ai.konduit.serving.data.nd4j.X` is fine) but we cannot have classes
in the same package - i.e., modules X and Y can't both define classes directly in the same namespace.
The reason is to avoid split packages issues for OSGi and Java 9 Modules. We will likely use OSGi extensively in the future.


