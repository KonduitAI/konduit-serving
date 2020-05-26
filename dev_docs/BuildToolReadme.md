# Konduit Serving Build Tool Readme

For background: read ADRs/0005-Packaging_System_Design.md first

## Dependency Management

The purpose of the build tool dependency management is to answer two questions:
1. What must be included in the artifact build for a pipeline to actually be executable?
2. What optional components can be included/configured to optimize performance? 

The overall goal is to produce correct, fast and minimal size binaries for a target architecture/OS, and not allow users to build
binaries that won't work due to limitations of the underlying libraries (i.e., can't run X on device Y).

### Required Dependencies with Native Code
Some modules have dependency requirements for the module to run, that are not included by default in the module's pom.xml.  
For example, the DL4J and SameDiff modules can only run if an appropriate ND4J backend (nd4j-native, nd4j-cuda-10.x) is
on the classpath.

One significant complication here is dependencies with native code - i.e., anything with compiled C/C++ libraries - which
is pretty much everything optional of interest in deep learning. These "native code" dependencies support one or sometimes
multiple target operating systems and architectures for each dependency. For example linux x86, linux x86 AVX2, linux x86
with CUDA, Linux arm-hf etc.

We need a way of doing four things before executing a build.
1. Checking that the current build (i.e., proposed/selected set of dependencies) will actually work on the target
   operating system and hardware
2. Where only one valid option exists (see example below) automatically add it to the build
3. Where multiple valid options exist, present the option to users (i.e., select one by default but allow them to customize their build)

An example of where only one valid option exists is for the nd4j-native:linux-armhf classifier dependency when deploying on
Linux ARM-HF - the library simply won't run without this dependency, and there's no alternatives.

An example of where multiple valid options exist is for the ONNX backend (multiple execution backends exist).

### Optional Dependencies

Two examples of optional (but recommended) dependencies are:
1. deeplearning4j-cuda-10.x (i.e., DL4J cuDNN support) for konduit-serving-deeplearning4j - but only when the user is targeting
   a CUDA deployment
2. MKL BLAS dependency for konduit-serving-deeplearning4j and -samediff - but only when the user is targeting an x86 CPU deployment 

Optional dependencies should recommended to the user, and perhaps selected by default in the UI/CLI.

### Dependency Validation Design

We will introduce the following classes in konduit-serving-build:
* **Module** - which represents a single KS module (such as konduit-serving-deeplearning4j)
* **ModuleRequirements** - which represent the set of dependency requirements that must be satisfied  
  For example: "konduit-serving-deeplearning4j needs an ND4J backend, which is one of {nd4j-native, nd4j-cuda-10.0, ...}"  
  Note ModuleRequirements is simply a `List<DependencyRequirement>`
* **DependencyRequirement** - which represents a single requirement that must be satisfied - usually in the form of "all of {x,y,z}"
  or "one of {x,y,z}" where x/y/z are a single dependency
* **Dependency**: Represents a single dependency - as defined by it's GAV coordinates - group id, artifact id, version, and optionally classifier.
  We will have a mechanism to check/look up if a dependency has native/target-specific code -  NativeDependencyRegistry
* **NativeDependency**: Represents the targets a dependency supports  
  Note that NativeDependency internally has a Dependency, and also a `Set<Target>`
* **Target**: Represents an operating system + architecture + device  
  For example: (linux + x86_avx2 + cpu), (windows + x86 + CUDA), etc



**NativeDependencyRegistry**: Used to get information about whether a given dependency has any native library (or otherwise
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
added to satisfy the requirements for that platform.
Where there is no valid way to satisfy the requirements (for example, native library X simply doesn't support CPU architecture Y)
we will return null.