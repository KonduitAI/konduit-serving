# Konduit: Enterprise Runtime for Machine Learning Models

<p align="center">
  <img src="https://s3.amazonaws.com/TWFiles/486936/companyLogo/tf_44cf7e67-0538-4a83-b686-e5145eca1c41.Konduit_teamwork.jpg">
</p>

---

## Overview

Konduit is a serving system and framework focused on deploying machine learning
pipelines to production. The core abstraction is an idea called a "pipeline step".
An individual step is meant to perform a task as part of using a machine learning
model in a deployment. These steps generally include:

1. Pre- or post-processing steps
2. One or more machine learning models
3. Transforming the output in a way that can be understood by humans,
such as labels in a classification example.

For instance, if you want to run arbitrary Python code for pre-processing purposes,
you can use a`PythonPipelineStep`. To perform inference on a (mix of) TensorFlow,
Keras, DL4J or PMML models, use `ModelPipelineStep`. Konduit Serving also contains
functionality for other preprocessing tasks, such as DataVec transform processes,
or image transforms.

## Why Konduit

Konduit was built with the goal of providing proper low level interop
with native math libraries such as TensorFlow and our very own DL4J's
core math library libnd4j.

At the core of pipelines are the [JavaCPP Presets](https://github.com/bytedeco/javacpp-presets),
[vertx](http://vertx.io) and [Deeplearning4j](http://deeplearning4j.org)
for running Keras models in Java.

Konduit Serving (like some of the other libraries of a similar concept such as [Seldon](http://seldon.io/) or [MLflow](http://mlflow.org/))
provides building blocks for developers to write their own production
ML pipelines from pre-processing to model serving, exposable 
as a simple REST API.

Combining JavaCPP's low-level access to C-like apis from
Java, with Java's robust server side application development (vertx on top of [netty](http://netty.io/))
allows for better access to faster math code in production while minimizing
the surface area where native code = more security flaws (mainly in server side networked applications)

This allows us to do things like in zero-copy memory access of NumPy arrays
or Arrow records for consumption straight from the server without copy or serialization overhead.

When dealing with deep learning, we can handle proper inference on the GPU
(batching large workloads)

Extending that to Python SDK, we know when to return a raw Arrow record and return it as a pandas DataFrame!

We also strive to provide a Python-first SDK that makes it easy to integrate
pipelines into a Python-first workflow.

Optionally, for the Java community, a vertx based model server and pipeline development
framework allow a thin abstraction that is embeddable in a Java microservice.

Finally, we want to expose [modern standards](http://prometheus.io/) for monitoring everything from your GPU
to your inference time.

Visualization can happen with applications such as [Grafana](http://grafana.com)
or anything that integrates with the [Prometheus](https://prometheus.io/) standard for
visualizing data.

Finally, we aim to provide integrations with more enterprise platforms
typically seen outside the big data space.

## Usage

### Python SDK

See the `python` subdirectory for our Python SDK.

## Configuring server on startup

Upon startup, the server loads a `config.json` or `config.yaml` file specified by
the user.  If the user specifies a YAML file, it is converted to a `config.json`
that is then loaded by [vertx](http://vertx.io/).

This gets loaded in to an [InferenceConfiguration](konduit-serving-api/src/main/java/ai/konduit/serving/InferenceConfiguration.java)
which just contains a list of pipeline steps. Configuring the steps is relative to the implementation.

A small list (but not all!)  of possible implementations can be found [here](konduit-serving-core/src/main/java/ai/konduit/serving/pipeline/steps).

An individual agent is a Java process that gets managed by a [KonduitServingMain](konduit-serving-core/src/main/java/ai/konduit/serving/configprovider/KonduitServingMain.java).

Outside of the pipeline components themselves, the main configuration is a [ServingConfig](konduit-serving-api/src/main/java/ai/konduit/serving/config/ServingConfig.java)
which contains information such as the expected port to start the server on, and the 
host to listen on (default localhost).

If you want your model server to listen on the public internet, please use `0.0.0.0`
instead.

Port configuration varies relative to your type of packaging (for example, in Docker, it may not matter)
because the port is already mapped by Docker instead.

From there, your pipeline may run in to issues such as memory, or warm up issues.

When dealing with either, there are generally a few considerations:

1. [Off heap memory in JavaCPP/DL4J](https://deeplearning4j.org/docs/latest/deeplearning4j-config-memory)

2. Warmup time for Python scripts (sometimes your Python script may require warming up the interpreter).
Summary of this is: depending on what your Python script does when running the Python server,
you may want to consider sending a warmup request to your application
to obtain normal usage.

3. Python path: When using the Python step runner, an additional Anaconda distribution
may be required for custom Python script execution. An end to end example can be found in the
[docker](./docker) directory.

4. For monitoring, your server has an automatic `/metrics` endpoint built in
that is pollable by Prometheus or something that can parse the Prometheus format.

5. A PID file automatically gets written upon startup. Overload the locaion with `--pidFile=....`.

6. Logging is done via logback. Depending on your application, you may want to over ride how the logging works.
This can be done by [overriding the default `logback.xml` file](https://logback.qos.ch/manual/configuration.html)

7. Configurations can be downloaded from the internet! Vertx supports different ways of configuring
different configuration providers. HTTP (without auth) and file are supported by default. For more on this, please see
the [official vertx docs](https://vertx.io/docs/) and bundle your custom configuration provider
within the built uber jar. If your favorite configuration provider isn't supported,
please file an issue.

8. Timeouts: Sometimes work execution may take longer. If this is the case,
please consider looking at the `--eventLoopTimeout` and `--eventLoopExecutionTimeout`
arguments.

9. Other vertx arguments: Due to this being a vertx application at its core,
other vertx JVM arguments will also work. We specify a few that are important
for our specific application (such as file upload directories for binary files)
in the KonduitServerMain but allow vertx arguments as well for startup.

For your specific application, consider using the built-in monitoring capabilities for both CPU and GPU memory
to identify what your ideal pipelines configuration should look like under load.

### Core workflow:

The core intended workflow is a few simple steps:

1. Configure a server setting up:
  - `InputType`s of your pipeline
  - `OutputType`s of your pipeline
  - A `ServingConfiguration` containing things like
         host and port information
  - A series of `PipelineStep`s that represent
      what steps a deployed pipeline should perform

2. Configure a client to connect to the server.

## Building/Installation

Dependencies:

1. [JDK 8](https://adoptopenjdk.net/) is preferred.
2. [mvnw](https://stackoverflow.com/questions/38723833/what-is-the-purpose-of-mvnw-and-mvnw-cmd-files/41239572) will download and setup
Maven automatically 

In order to build pipelines, you need to configure:

1. Chip (`-Dchip=YOURCHIP`)
2. OS   (`-Djavacpp.platform=YOUR PLATFORM`)
3. Way of packaging (`-P<YOUR-PACKAGING>`)
4. Modules to include for your pipeline steps(`-P<MODULE-TO-INCLUDE>`)

-D is a JVM argument and and -P is a Maven profile.
Below we specify the requirements for each configuration.

### Chips

Konduit Serving can run on a wide variety of chips including:

- ARM (experimental):       `-Dchip=arm`
- Intel/X86: `-Dchip=cpu`
- CUDA:      `-Dchip=gpu`

### Operating systems

Supported operating systems include:

- Linux
- Mac
- Windows

Untested but should work (please let us know if you would like to try setting this up!):

- Android
- IOS via [gluon](http://gluonhq.com/)

Packaging pipelines for a particular operating system typically will depend on
the target system's supported chips.
For example, we can target Linux with ARM or Intel architecture.

JavaCPP's platform classifier will also work depending only on the targeted chip.
For these concerns, we introduced the -Dchip=gpu/cpu/arm argument
to the build. This is a thin abstraction over JavaCPP's packaging
to handle targeting the right platform automatically.

To further thin out other binaries that maybe included (such as opencv)
we may use -Djavacpp.platform directly. This approach is mainly tested
with Intel chips right now. For other chips, please file an issue.

These arguments are as follows:

1. -Djavacpp.platform=windows-x86_64
2. -Djavacpp.platform=linux-x86_64
3. -Djavacpp.platform=macosx-x86_64

Specifying this can optimize the jar size quite a bit, otherwise
you end up with extra operating system specific binaries in the jar.
Initial feedback via GitHub Issues is much appreciated!

### Packaging options

Konduit Serving packaging works by including all of the needed dependencies
relative to the selected profiles/modules desired for inclusion
with the package. Output size of the binary depends on a few core variables:

Many of the packaging options depend on the [konduit-serving-distro-bom](konduit-serving-distro-bom)
or pipelines bill of materials module.
 This module contains all of the module inclusion behavior
 and all of the various dependencies that end up in the output.
 
 All of the modules rely on building an [uber jar](https://stackoverflow.com/questions/11947037/what-is-an-uber-jar), and then packaging it in
 the appropriate platform specific way.

1. The javacpp.platform jvm argument 

2. The modules included are relative to the [Maven profiles](https://maven.apache.org/guides/introduction/introduction-to-profiles.html)
Modules are described below

-  Standard Uberjar: `-Puberjar`
-  Debian/Ubuntu: `-Pdeb`
-  RPM(Centos, RHEL, OpenSuse,..):  `-Prpm`
-  Docker: `-Pdocker`
-  WAR file(Java Servlet Application Servers): `-Pwar`
-  TAR file: `-Ptar`
-  Kubernetes: See the [helm charts](helm-charts) directory
for sample charts on building a pipelines module for Kubernetes.


For now, there are no hosted packages beyond what is working in pip at the moment.
Hosted repositories for the packaging formats listed above will be published later.


### Modules to include

- Python support: `-Ppython`
- PMML support: `-Ppmml`

In order to configure pipelines for your platform, you use a Maven-based build profile.
An example running on CPU:

```bash
./mvnw -Ppython -Ppmml -Dchip=cpu -Djavacpp.platform=windows-x86_64 -Puberjar clean install -Dmaven.test.skip=true
```

This will automatically download and setup a pipelines uber jar file (see the uber jar sub directory)
containing all dependencies needed to run the platform.

The output will be in the target directory of whatever packaging mechanism you specify
(docker, tar, ..)

For example if you build an uber jar, you need to use the -Puberjar
profile, and the output will be found in model-server-uber-jar/target.

## Custom Pipeline steps

Konduit Serving supports customization via 2 ways: Python code or implementing your own
[PipelineStep](konduit-serving-api/src/main/java/ai/konduit/serving/pipeline/PipelineStep.java)
via the [CustomPipelineStep](konduit-serving-api/src/main/java/ai/konduit/serving/pipeline/PipelineStep.java)
and associated [PipelineStepRunner](konduit-serving-core/src/main/java/ai/konduit/serving/pipeline/steps/CustomPipelineStepRunner.java#L40)
in Java.

Custom pipeline steps are generally recommended for performance reasons.
Nothing is wrong with starting with just a Python step though. Depending on scale,
it may not matter.


## Orchestration

Running multiple versions of a pipelines server with an orchestrations system with load balancing
etc will heavily rely on vertx functionality. Konduit Serving is fairly small in scope right now.

Vertx has support for many different kinds of typical clustering patterns 
such as an [API gateway](https://github.com/sczyh30/vertx-blueprint-microservice/blob/master/api-gateway/src/main/java/io/vertx/blueprint/microservice/gateway/APIGatewayVerticle.java), [circuit breaker](https://github.com/vert-x3/vertx-examples/tree/master/circuit-breaker-examples)

Depending on what the user is looking to do, we could support some built in patterns in the future
(for example basic [load balanced pipelines](https://github.com/aesteve/vertx-load-balancer/tree/master/src/main/java/io/vertx/examples/loadbalancer)).

Vertx itself allows for different patterns that could be implemented in either vertx itself
or in Kubernetes.

[Cluster management](https://github.com/vert-x3/vertx-awesome#cluster-managers) is also possible
using one of several cluster node managers allowing a concept of node membership. Communication
with multiple nodes or processes happens over the [vertx event bus](https://medium.com/@hakdogan/working-with-multiple-verticles-and-communication-between-them-in-vert-x-2ed07e8e6425).
Examples can be found [here](https://github.com/vert-x3/vertx-examples/tree/master/core-examples/src/main/java/io/vertx/example/core/eventbus) for how to send messages between instances.

A recommended architecture for fault tolerance is to have an API gateway + load balancer
setup with multiple versions of the same pipeline on a named endpoint.
That named endpoint would represent a load balanced pipeline instance
where 1 of many pipelines maybe served.

In a proper cluster, you would address each instance (an InferenceVerticle in this case representing a worker)
as: `/pipeline1/some/inference/endpoint`

For configuration, we recommend versioning all of your assets that are needed alongside
the `config.json` in something like a bundle where you can download each versioned asset
with its associated configuration and model and start the associated instances from that.

Reference [KonduitServingMain](konduit-serving-core/src/main/java/ai/konduit/serving/configprovider/KonduitServingMain.java)
for an example of the single node use case.

We will add clustering support based on these ideas at a later date. Please file an issue if you have specific questions
in trying to get a cluster setup.

## License

Every module in this repo is licensed under the terms of the Apache license 2.0, save for `konduit-serving-pmml` which is agpl to comply with the JPMML license.
