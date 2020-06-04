# Konduit Serving Build Tool Readme

For background: read ADRs/0005-Packaging_System_Design.md first.
Also reading the "Annotations and Metadata Collection" section of dev_docs/README.md

## Overview

The purpose of the Konduit Serving build tool is to produce (Java-based) artifacts that can be used to serve a pipeline.
That is, we want to produce an uber-jar, Docker file, .exe file, .rpm/.deb, etc that can be launched in order to start a
HTTP, GRPC or (soon) MQTT model server for a Pipeline (usually containing one or more machine learning models).

As described in the build tool ADR mentioned earlier, we want to:
* Automate the selection and recommendation of optional dependencies, such as ND4J backends, for a given target device
* Validate builds - i.e., configuration time failure for bad setups, not build time or runtime
* Attempt to optimize performance (i.e., ensure we use AVX2 or AVX512 on x86, or make sure cuDNN, etc)
* Provide good usability when building, especially as the number of different configurations and combinations continues to grow 

Note that for use cases such as those involving deployment on edge devices, the minimization of the resulting JAR size
is also an important goal: i.e., we want to include _only_ what we need, and nothing more. In the edge content:
* Network bandwidth is often restricted (i.e., large files mean slow updates)
* Network bandwidth is sometimes not free, such as for a 4G-connected device (i.e., every MB costs the user some amount
  to transfer when updating a device)
* Storage is limited on some edge devices


Configuration is implemented in the `ai.konduit.serving.build.config.Config` class, which specifies all of the information
necessary to define a build:
* A Pipeline that is to be deployed using the produced artifact (or rather, the path to the JSON/YAML for the pipeline)
* An optional additional list of modules to include (beyond those automatically inferred from the Pipelin JSON/YAML)
* The target hardware (OS + architecture + optionally compute device)
* The serving type (HTTP, GRPC or (soon) MQTT)
* The deployment types (uber-jar, docker, .exe, etc)

## Dependency Management

Dependency management is only relevant for the build tool for "optional" or "configurable" dependencies
Anything and everything else that is included as normal in the module's compile time dependencies is handled by the
Gradle/Maven build tool as in a normal build. So "dependency management" in the context of the build tool is strictly
limited to the addition/selection of optional dependencies for a specific target device (such as ND4J backends and native
library classifier JARs say for linux x86_avx2 etc). 

The purpose of the build tool dependency management is to answer two questions:
1. What must be included in the artifact build for a pipeline to actually be executable on a given target?
2. What optional components can be included/configured to optimize performance? 

The overall goal is to produce correct, fast and minimal size binaries for a target architecture/OS, and not allow users to build
binaries that won't work due to limitations of the underlying libraries (i.e., can't run X on device Y).

These optional dependencies are specified by a set of Java annotations as describe in the "Annotations and Metadata
Collection" section of dev_docs/README.md

### Required Dependencies with Native Code

Some modules have dependency requirements for the module to run, that are not included by default in the module's pom.xml.  
For example, the DL4J and SameDiff modules can only run if an appropriate ND4J backend (nd4j-native, nd4j-cuda-10.x) is
on the classpath.

One significant complication here is dependencies with native code - i.e., anything with compiled C/C++ libraries - which
is pretty much everything optional of interest in deep learning. These "native code" dependencies support one or sometimes
multiple target operating systems and architectures for each dependency. For example linux x86, linux x86 AVX2, linux x86
with CUDA 10.1, Linux arm-hf etc.

We need a way of doing the following before executing a build with native code dependencies.
1. Filtering the set of optional dependencies for a module to select only those that are compatible with the the target
   device, in order to recommend specifically which dependency (or dependencies) are compatible 
2. Where only one valid option exists (see example below) automatically add it to the build
3. Where multiple valid/compatible optional dependencies exist, select the "best" one (i.e., select the linux-x86_64-avx2
   classifier even though the linux-x86_64 classifier is also compatible)
4. Checking that the current build (i.e., proposed/selected set of dependencies) will actually work on the target
   operating system and hardware (and telling the user why not, if it won't - for example "library X does not support
   specified target T")

An example of where only one valid option exists is for the nd4j-native:linux-armhf classifier dependency when deploying on
Linux ARM-HF - the library simply won't run without this dependency, and there's no alternatives.

An example of where multiple valid options exist is for the ONNX backend (multiple execution backends exist for ONNX).

### Optional Dependencies

Two examples of optional (but recommended) dependencies are:
1. deeplearning4j-cuda-10.x (i.e., DL4J cuDNN support) for konduit-serving-deeplearning4j - but only when the user is targeting
   a CUDA deployment
2. MKL BLAS dependency for konduit-serving-deeplearning4j and -samediff - but only when the user is targeting an x86 CPU deployment 

Optional dependencies should recommended to the user, and perhaps selected by default in the UI/CLI.

### Dependency Validation Design

We will introduce the following classes in konduit-serving-build:
* **Module** - which represents a single KS module (such as konduit-serving-deeplearning4j)
* **ModuleRequirements** - which represent the set of dependency requirements that must be satisfied before the produced
  artifact will actually work  
  For example: "konduit-serving-deeplearning4j needs an ND4J backend, which is one of {nd4j-native, nd4j-cuda-10.0, ...}"  
  Note ModuleRequirements is simply a `List<DependencyRequirement>`
* **DependencyRequirement** - which represents a single requirement that must be satisfied - usually in the form of "all of {x,y,z}"
  or "one of {x,y,z}" where x/y/z are a single dependency
* **Dependency**: Represents a single dependency - as defined by it's GAV coordinates - group id, artifact id, version, and optionally classifier.
  We have a (currently simple) mechanism to determine if a dependency has native/target-specific code -  NativeDependencyRegistry
* **NativeDependency**: Represents a dependency with target/platform-specific code - and the targets it actually supports  
  Note that NativeDependency internally has a Dependency, and also a `Set<Target>`
* **Target**: Represents an operating system + CPU architecture + device. Devices represent compute accelerators such as a GPU
  but do not include CPUs. If a device is not specified, cpu-only execution is assumed.   
  For example: (linux + x86_avx2), (windows + x86 + CUDA 10.0), etc
* **NativeDependencyRegistry**: Used to get information about whether a given dependency has any native library (or otherwise
  platform specific code) or not - and if so, what deployment targets it supports.
  Exactly how this will be implemented remains to be determined. Right now we have a placeholder that just hardcodes this 
  information for some common dependencies.


### Dependency Recommendation/Selection Mechanism

Suppose the current set of dependencies for a user's project does not satisfy the module's requirements - and we need to add
a new dependency. How do we work out what ones to select (if there is only one valid option) or suggest (if there are multiple
valid options)?

We already have the module requirements - in the form of "all of {x,y,z}" and "one of {x,y,z}".
Working backwards from these is straightforward - we implement a ModuleRequirements.suggestDependencies method that looks
at the "all of" / "one of" requirements, and looks at the current set of dependencies, and suggest what must/could be
added to satisfy the requirements for that platform/target.
Where there is no valid way to satisfy the requirements (for example, native library X simply doesn't support CPU architecture Y)
we will return null.

This "what dependencies do we need" aspect is implemented using the **DependencyAddition** interface and the 
`DependencyRequirement.suggestDependencies(Target, Collection<Dependency>)` method (which suggests what dependencies must/could
be added, taking into account what is already present for the build).

## Build Execution

Build execution is split into two parts
* Gradle file generation - `GradleBuild.generateGradleBuildFiles`, and
* Build execution - `GradleBuild.runGradleBuild`

### Deployment Types

The goal is to allow artifacts for different deployments to be created. Deployment types are specified via a configuration
class that subtypes `ai.konduit.serving.build.config.Deployment`.
Supported deployment types (still WIP) include:
* Uber-JARs
* Docker
* Standalone .exe (with embedded JVM)
* WAR files
* RPM/DEB files
* TAR files

Note for these other deployment types (docker, rpm, tar etc) in practice it's just an "Uber-JAR in X" design for now.
i.e., it's fundamentally still an uber-JAR based deployment/artifact.  
In the future we may also support an assembly-style approach - i.e., launch the JVM with a list of independency dependency
JAR files, rather than creating a single uber-JAR.
