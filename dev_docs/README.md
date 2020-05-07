# Developer Documentation

These docs are designed to explain the architecture of Konduit Serving (for the "new" API as of 05/2020).

See `ADRs/0003-Pipeline-API_Rewrite.md` for background and motivation on the current design.

## Core classes:

* Pipeline: An *interface* that defines a `Data -> Data` transformation made up of one or more pipeline steps.
    - Note: There are 2 types - `SequencePipeline` (a stack of operations) and `GraphPipeline` (WIP - a directed acyclic graph
      possibly including optional branches). Analogous to MultiLayerNetwork/Sequential and ComputationGraph/Functional in DL4J/Keras.
* PipelineStep: Also an interface defines a `Data -> Data` transformation. Consider these the building blocks for building a Pipeline
    - Model inference, ETL, data transformation, etc are all implemented as a PipelineStep.
* PipelineExecutor: An interface that can execute a Pipeline. Made up of multiple PipelineStepRunners - one for each PipelineStep
* PipelineStepRunner: An interface for running a PipelineStep
* PipelineStepRunnerFactory: For creating PipelineStepRunner from PipelineExecutor. Explained in more detail later.
* Data: A map-like store of key-value pairs.
    * Keys are always Strings, values are one of: {String, int64 (long), double, boolean, NDArray, byte[], Image, List[T], Data} where T is any of these types.
      Note that we allow nested Data and List objects.
    * Data instances also have optional metadata, stored as a nested Data instance 
    * Note NDArray and Image are special cases here, discussed in detail below

**Key modules - New API:**

The list below briefly describes those implemented so far (as of 05/05/2020)- there are many more to be added.

* konduit-serving-pipeline: Core API for pipelines and local execution
    * Note: very few dependencies. No ND4J, DataVec, Python, TensorFlow etc in this module. Only Java API and baseline Java functionality.
* konduit-serving-models: Parent module for each of the model types
    * konduit-serving-deeplearning4j: Deeplearning4j models.
* konduit-serving-data: Parent module for data and datatypes
    * konduit-serving-nd4j: Mainly NDArray integration/functionality for ND4J
    * konduit-serving-javacv: Image conversion functionality for JavaCV 
* konduit-serving-io: Parent module for I/O functionality - sensors, cameras, etc - and maybe later things like HDFS, S3, etc
    * konduit-serving-camera: Steps related to capturing data from device-connected cameras (WIP)


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
Note that GraphPipeline is still WIP as of 05/05/2020.

Though the Java API is the same, the difference between the two is:  
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

`JData` is the Java Map-based implementation of the `Data` interface - i.e., literally it has a `Map<String,Value<T>>` internally,
where `Value<T>` is a trivially simple object to hold objects of different types.

We also have (WIP as of 05/05/2020) a Protobuf-based `Data` implementation - ProtoData that stores data in protobuf form.
Protobuf - aka Protocol Buffers - is a widely used binary data serialization format.


Why protobuf? 4 reasons
* Format is language independent (i.e., create a file/bytes in any language, read in any other language)
    - Also: define once and generate ser/de code in each language (though we need custom/non-generated wrapper code for UX and Data compatibility)
* Performance
    - considerably faster to encode/decode than formats such as JSON
    - Can allow for zero-copy deserialization in some cases
* Space efficient - much more so than other formats such as JSON (1-10x depending on the format)
* Enables gRPC (which we will add later as an alternative to HTTP/REST)

A key point regarding Data: Any time we need to do any serialization, network transfers, saving/loading to/from file etc - we will use the
protobuf format. i.e., the protobuf-based Data implementation is considered the canonical representation for anything other
than temporarily when running within a single JVM.

In practice, we'll still use JData a lot for monolithic Java deployments. The reason for this is that we don't want to pay the
serialization/deserialization cost on every pipeline step - so we'll use JData as a holder, and (automatically, internally)
convert JData to ProtoData whenever we need to do IPC, serialization or sending binary data over the wire.

Similar to how the ProtoData (protobuf) is the canonical format for Data in binary, we will have only one JSON format for
all Data implementations. i.e., JData, Protodata (or any possible future Data implementation) will all produce the exact
same JSON/YAML. The implementation for this is still WIP.  

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

NDArray and Image are interfaces. The design for NDArray and Image is that they are both just an "object holders and converters".
That is, the only thing the user can do is say "give me the content of this NDArray/Image in this format (automatically converting
if necessary)".

The main downside is that we (as developers) need to define ahead of time format converters - for example, how an INDArray can
be converted to a `float[][]`, and vice versa. This will require a bit of tedious boilerplate for us - as there are many
possible conversions to implement.

However, once we have done that, the result should be quite good usability for users:
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

Down the line we'll implement multi-step conversion for easy extensibility. That is, suppose the user wants to do `NDArray.create`
for some custom type X. Instead of having to write conversion for `X -> float[]`, `X -> INDArray` etc etc they will
simply write `X -> C` where `C` is some standard canonical format.
We'll probably need/use this anyway for serialization and IPC.


In practice, this is implemented via instances of the following interfaces:
* ImageConverter - for converting one Image format to another
* ImageFactory - used in `Image.create(Object)` to create Image object wrapping that image type
* ImageFormat - specifies a format with more detail than just a class name (for example, format configuration)
* NDArrayConverter - for converting one NDArray format to another
* NDArrayFactory - used in `NDArray.create(Object)`
* NDArrayFormat - specifies a format with more detail than just a class name (for example, format configuration)

The actual converters and creators are loaded (using Java ServiceLoadear mechanism) and stored in the following classes:
* ImageConverterRegistry
* ImageFactoryRegistry
* NDArrayConverterRegistry
* NDArrayFactoryRegistry 


Serialization and IPC: still WIP. While we are within a single JVM, we don't necessarily need to do any conversion - for
example, all of our pipeline steps could use `INDArray` or use `float[][]` etc - that's fine. But what happens when we
want to save (or transmit over the network) a Data instance? Clearly we need some language-agnostic format for storing
the NDArray and Image values, no matter what type they are internally at present.  
For NDArray, this format is straightforward - we store a C-order buffer of values (with some fixed endianness), a type
enum and a shape array (as `long[]`).  
For images - it's less obvious what the format - or formats - should be. Though something like JPG format would seem like
a reasonable option - one issue is that it is lossy - i.e., there's no guarantee that we'll get the exact same image out
of serialization as went in (only a close approximation). That might - or might not - matter in different use cases.
Alternatively, PNG is lossless - but it's size can be significantly larger for 'natural' images (i.e., typical photos)
that will frequently be fed into a neural network.


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

The solution is to use a registration + ServiceLoader type approach.  
That is: in konduit-serving-pipeline we have the `JsonSubType` class (fields: name, subtype, configInterface) and a
`JsonSubTypesMapping` interface.
The JsonSubType object should be interpreted as follows: "if a subtype with name *name* appears in some JSON or YAML, we
should deserialize it to class *subtype*, which is a subtype of *configInterface*".  
An alternative way of looking at it is that it's the runtime equivalent of this:
```java
@JsonSubTypes({ @JsonSubTypes.Type(value = <subtype>, name = <name>)})
public interface configInterface {
```


Each module then implements a single class that implements JsonSubTypesMapping, which returns at runtime the JSON subtype mapping.

Finally, each module also creates a `resources/META-INF/services/ai.konduit.serving.pipeline.api.serde.JsonSubTypesMapping`
file (with that exact name and path). The content of that file is the fully-qualified class name of the class that
implements the JsonSubTypesMapping interface.
This is a Java ServiceLoader file that allows the JsonSubTypesMapping to be discovered at runtime.

For now, we have this ServiceLoader based implementation, though we can use this approach also in an OSGi deployment
context (which we will be supporting in the future).
 

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
 ----- DATA -----
{
  "myKey" : {
    "myInnerKey" : "myInnerValue"
  }
}
```

Note that strings, bytes, double, int64 and boolean are basically as you would expect.  
The format for nested Data instances is identical to the non-nested ones.  
List formats (not shown above) will use JSON arrays - so it'll look like `"myKey" : ["some", "list", "values"]`  
Bytes, is a special case - it's an object with a special protected key: `@BytesBase64`. Currently (and by default) we
will encode `byte[]` objects as base64 format for space efficiency; later we will allow "array style" byte[] encoding
(i.e., `"@BytesArray" : [1,2,3]`). Users are not allowed to add anything to a Data instance with these protected keys.

Image data is stored by default in PNG format, base64 encoded (this will be configurable eventually).

NDArray is a special case also: it in a JSON object with type/shape/data keys. Currently data is base64 encoded, but
we may allow a "1d buffer array" format in the future also.

The full set of protected keys can be found on the Data interface. They include:
* @BytesBase64
* @BytesArray
* @ImageFormat
* @ImageData
* @NDArrayShape
* @NDArrayType
* @NDArrayDataBase64
* @Metadata

In practice, Data JSON serialization/deserialization is implemneted in the DataJsonSerializer and DataJsonDeserializer classes. 


## Adding a New Module

To reiterate: When adding a new module, no modifications to the konduit-serving-pipeline module are allowed. That means:
No new dependencies, new classes, code changes etc in konduit-serving-pipeline - nothing. The design was implemented
specifically to enable building new modules in isolation in a way that works with konduit-serving-pipeline.

So: What is actually involved in adding a new module?

*First*: The new module should have konduit-serving-pipeline as a dependency in order to use the API

If building on some other module, include it also. For example, `kondit-serving-deplearning4j` includes `konduit-serving-nd4j`.


*Second*: If a new PipelineStep is to be added, all of the following are required:
- Implement the new step, extending the PipelineStep interface
- (Usually) Add a new PipelineStepRunner, for executing the type of step you have just created
- (Usually) Add a new PipelineStepRunnerFactory, for creating your new PipelineStepRunner from your new PipelineStep interface
- Assuming a PipelineStepRunner was added, add a `resources/META-INF/services/ai.konduit.serving.pipeline.api.step.PipelineStepRunnerFactory`
  file, containing the fully-qualified path of your new PipelineStepRunnerFactory 
- Add a JsonSubTypesMapping implementation in the module as discussed in JSON section above
- Add a service loader file (JsonSubTypesMapping) as discussed in JSON section above 

See for example: konduit-serving-deeplearning4j


*Third*: If a new Image or NDArray format is to be added, all of the following are required:
- Add a new class for holding the Image/NDArray (extending BaseNDArray/BaseImage should suffice)
- Add NDArrayConverter / ImageConverter implementations to convert between different Image/NDArray formats (see for example NDArrayConverter in konduit-serving-nd4j)
    - For NDArray: the main (strictly required) one is conversion to/from SerializedNDArray - this is necessary for JSON
      and protobuf serialization/deserialization, and will be used as the intermediate format for conversion between arbitrary
      types that don't have direct (1 step) conversion enabled (i.e., X -> SerializedNDArray -> Y for any X and Y).
    - For Image: the main (strictly required) one is conversion to/from Png (i.e., `ai.konduit.serving.pipeline.impl.data.image.Png`)
      as this is used as the default format for both JSON and 
      The reason: PNG is a compressed lossless image format, unlike some alternatives such as JGP. It does have a size overhead
      for natural images vs. JPG, but in practice for deep learning we typically don't have very large input/output images.
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

 




 
 
