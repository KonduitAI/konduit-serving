# Konduit Serving Packaging System

## Status
PROPOSED

Proposed by: Alex Black (15-05-2020)

Discussed with: 

## Context

Konduit Serving is a complex modular software package intended to be deployed in a number of different configurations and packaging.

For any given model/pipeline, the deployment/packaging scenarios can vary widely. For example, a user might want to deploy a TensorFlow model in one of these configurations (and many more):
* Docker image packaging using TensorFlow + CUDA 10.1 on an Linux ARM64 system, with serving via HTTP/REST
* A self-contained .exe (with embedded JVM) using SameDiff TensorFlow import to run the model on CPU, on a Windows x86 + AVX2 system with Intel MKL + MKLDNN (OneDNN) included, with serving being performed via gRPC


Currently, packaging is done via Maven profiles and Maven modules.
A user selects the combination of dopendencies and functionality they need by enabling a number of profiles and system properties.
For example, building a Windows CPU uber JAR looks something like this:
```
mvn clean package -DskipTests -Puberjar -Pcpu -Ppython -Pnative -Ppmml -Ptensorflow -Dchip=cpu -Djavacpp.platform=windows-x86_64
```

This approach has got us quite far in terms of packaging (enabling flexible packaging options including uber-JARs, Docker, WARs, DEB/RPMs, tar files and .exe files), we are running up against the limits of this approach.

Specifically, this approach has the following problems:
* The combination of options only going to continue to grow
* Some combinations are difficult (for example, building a binary for both Windows and Linux, but not Mac, PPC, etc)
* It is easy to leave performance on the table - i.e., using ND4J/SameDiff/TensorFlow etc binaries built without AVX support
* Many incompatibilities will only become apparent at runtime (example: build for a CUDA version only to find that TensorFlow only releases one CUDA version and hence we have a runtime problem)
* Now (with the Data/API rewrite) configuration and execution is separate; the one configuration can be run many different ways. For example,  a TensorFlow model could be run with TensorFlow, SameDiff, TVM, or (possibly automated) conversion ONNX, etc.
* Usability issues: For example, users need to know a lot about the different profiles, configuration, etc to get an optimal (or even functional) deployment - or even know what is possible.
    - An example of this: the user might build an uber-JAR without the PMML profile being enabled, only to discover their JAR can't run their pipeline
* Packaging of custom code and dependencies is difficult


## Proposal

The scope of this proposal is limited to the creation/packaging of a Konduit Serving uberjar, which may be deployed in many forms (Docker)
Note that non-Java packaging/deployments of pipelines is out of scope; OSGi support is relevant but only in scope to the extent that an OSGi-based system could work with (or build on top of) the functionality described in this proposal.

**Proposal Goals*

The goals of this packaging proposal are as follows:
1. To retain and enhance the existing deployment options - uber-jar, docker, WAR, .exe, etc
2. Enable greater flexibility in the build/deployment configuration
3. To enable custom Java and Python code (and dependencies) to be easily included in a deployment
4. To improve usability and reliability of packaging, in the following ways
    - Remove the reliance on Maven profiles and properties (at least as the only option)
    - Automate the selection (or recommendation) of modules to include for a given pipeline
    - Add validation and checking for common pitfalls such as dependency issues (incompatible with architecture, wrong CUDA version, etc)
    - Make it more clear to the user what requirements (in terms of hardware and software), if any, need to be satisfied on the deployment system

**Proposal Overview**

This proposal has a number of parts:
1. A Konduit Serving build configuration format
2. A build tool (on top of Maven) that utilizes the configuration format to actually perform the required build
3. UI and command line tools for creating a build configuration for a given Pipeline configuration
4. A system for packaging custom Java code and dependencies

Note that for usability, where possible we'll make it so the user doesn't have to be aware of the build configuration file - for example, a simple CLI might be used to configure and execute a build. The CLI would generate the configuration, and pass it to the build tool, without the user being aware of the configuration file.


### Part 1 - Build Tool

Given a configuration file that specifies what should be included in the build (details later), the build tool will execute the build/s necessary to create the requested artifacts (JAR/s, docker images, etc).
Note that the term "build tool" may not be an ideal name, as the proposed tool is simply a thin layer on top of Maven - and not comparable to a "true" build tool like Maven, Gradle, Ant, etc.

Note also that in principle (though this is not proposed for right now) we can have multiple build tools for creating the final artifacts from these  - i.e., the configuration (what) and the build tool (how) are separate.
Until (if) we look at pure C++ deployments, the main possible use for a second build tool would be for OSGi-based deployments. However, this would still be Maven based.

The proposed build tool will generate (and then execute via Maven) a pom.xml file based on the configuration file.
Similar to the current "modules and profiles" approach, we will continue to use Maven plugins for the actual packaging.

This generated pom.xml file will include:
* A `<dependencies>` section, listing the direct dependencies:
    * The required konduit serving modules - konduit-serving-tensorflow, konduit-serving-nd4j, etc
    * Any "native library" / "backend" dependencies (ND4J native/CUDA backends, for example)
    * Logging etc dependencies
* If necessary, a `<dependencyManagement>` section
* A simple `<properties>` section, for the source encoding and Java version
* A `<build><plugins>` section
    * Always included plugins for tasks such as enforcing dependency convergence
    * One or more plugins for each deployment type. For example, maven-shade-plugin for building uber-jars, and 

One consequence is that all of the "packaging" modules would be removed, in favor of a single `konduit-serving-build` or `konduit-serving-deploy` module.


An alternative design would be to attempt to use profiles and properties, however this seems much less flexible and harder to understand/maintain especially when things go wrong.

### Part 2 - Configuration File

The configuration file should provide information necessary to determine for the build, the set of:
- direct dependencies
- plugins
- properties and profiles


To that end, the following information will be included as part of the configuration:
* The Konduit Serving modules to include
* The deployment packaging type(s) - Uber-Jar, Docker, etc - and their associated configuration
* The deployment target(s) - OS, architecture, CPU vs. GPU, etc
- Selected or preferred pipeline step runners (where more than one option exists)
* Information necessary to package any required external/custom code, dependencies, files, resources, etc
* Any additional dependency configuration or overrides (such as dependency management, exclusions, etc)

JSON/YAML is proposed to be used as the format.


### Part 3 - CLI

The CLI will 

Two modes of operation are proposed:
1. Command line style
2. "Wizard" style

The command line style will provide the information necessary to produce the configuration file in a short form. The exact configuration and options will be designed in more detail later, but it will likely look something like the following:
```bash
konduit-build myPipeline.json --modules tensorflow,nd4j,image --deploy docker --docker.config "name=x,version=y" --incudeJava "com.company:mylibrary:1.0.0"
```

The Wizard style of use will guide users through selecting the options. This will be lower priority for implementation than the "command line" style of use.
Again the specifics of the design need to be worked out, but it is suggested that usage will look something like the following

```
> konduit-build
Konduit Serving build tool, v0.2.1
Enter path to Pipeline .json or .yml file (or ctrl+c to exit)

> myFile.yml

Select deployment environment OS (comma or space delimited, case insensitive)
Options:
l = Linux x86-64
w = Windows x86-64
m = Mac OSX x86-64
lahf = Linux ARM - armhf
la64 = Linux ARM - arm64
...

> l,la64

...
```

### Part 4 - Build UI

The Build UI would be a simple UI (nothing fancy or feature rich) that focused on doing two things:  
(a) Guiding users through the configuration process for their pipeline  
    The main goal here is to show the user what the required modules are for serving their pipeline, and the options they have for customing the deployment (target platform, selected model runner, etc)  
(b) Creating the configuration file (though this would be implemented in the back-end)  
(c) Triggering the build based on the generated configuration file  

Users should be able to load a previously-created build configuration file (partially or completely specified) as a starting point for their pipeline build.  


At a later date, we may add a way to visualize and create pipeline configurations using this UI also. If we were to look at that, it would be after a separate ADR.


Starting and stopping the build UI should be straightforward (assuming the user has Konduit Python package or similar installed):
```
> konduit-build-ui

Konduit Serving build UI launched at: https://localhost:9123/
Use ctrl+c to exit
```

The workflow would for the user would be something like:
1. Select the Konduit Serving pipeline to deploy
2. Select the deployment environment(s) - OS, CPU architecture, CPU vs. GPU, AVX support or not, etc
3. Select the pipeline step runners (if multiple are available)  
   Example: when running a TensorFlow model - whether to use TF, SameDiff, TVM, etc
4. Optionally add custom Java code, Python code, and dependencies  
   Again, Java code/dependencies will be as simple as specifying the GAV coordinates.  
   Python packaging and dependencies is TBD, but may be something like a directory + a requirements.txt
5. Optionally, embed files/resources (including the model file)
6. Select packaging (uberjar, docker, exe, etc)  
   Each selected option should then show configuration
7. Click "verify" to check all options and produce a final report
   Check dependencies, produce final file size, etc
8. Execute build

At any point the user would be able to save the current configuration as a YAML/JSON file (and load it back in later).


At each stage, we would only allow the user to select options that are consistent with previous choices (with other options still visible but grayed out).


For step 2, at a later date we may introduce device profiles ("Raspberry pi 4B", "Jetson Nano", "Generic Linux x86_64", and possibly even common cloud VMs) to reduce the amount of knowledge/configuration required to create the pipeline.


### Part 5 - Pipeline Analysis and Module Selection

An important component of both the CLI and UI would be determining what Konduit Serving modules need to be included to execute a pipeline - and what options are available (i.e., which runners could be used to execute the steps contained within).

In the near-term, we could add something to (semi-automatically) track/aggregate execution support/capabilities across all modules - i.e., we'd build a mapping between module names (or rather, PipelineStepRunners) and the PipelineStep (configurations) they can run.


One thing to keep in mind is extensibility - for example, we might have custom pipeline steps available via a "Konduit Serving Hub" - code and dependencies for those custom pipeline steps could be pulled in automatically. However this should not substantially alter the basic approach for doing analysis/module selection.



### Part 6: Custom/External Java Code and Dependencies

In some pipelines, a user will want to write custom Java code - for example custom pipeline steps, metrics, etc.
Some of this custom Java code will have dependencies (direct and transitive) that also need to be included.
We need an easy system for including both the code and the dependencies in a Konduit Serving pipeline.

For Java, the proposal for handling this is trivially simple:
1. Users package their code as a Maven module
2. Users install the module, including their custom code, to their local Maven repository
   Note this need not be an uber-JAR, a standard JAR with direct and transitive dependencies is fine
3. Users specify the GAV coordinate (group ID, artifact ID and version) for their custom functionality in the configuration file (or more likely, via the CLI/UI)

An additional (or possible alternative) mechanism that could be added later, would be to provide a way of building a GitHub repository.
i.e., "clone, install, add to Konduit Serving deployment" would be doable in just a couple of lines of configuration. This could be useful for CI/CD based pipelines.

This "install and provide GAV" approach should also work fine for OSGi-based builds/deployments in the future.

### Future ADRs

There are a number of aspects of this packaging system that would need to be worked out in future ADRs.
These might include:
* The configuration format
* UI design
* Custom Python code (and dependency) embedding
* Architecture compatibility checking for dependencies with native code (i.e., "if I include dependency X, will it actually work on ARM64, PPC64le, etc?")
* File/resource embedding


## Consequences 

### Advantages

  
### Disadvantages


## Discussion

