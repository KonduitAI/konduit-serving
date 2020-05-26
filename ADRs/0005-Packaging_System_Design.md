# Konduit Serving Packaging System

## Status
ACCEPTED 26/05/2020

Proposed by: Alex Black (25/05/2020)

Discussed with: Sam, Paul

## Context

Konduit Serving is a complex modular tool intended to be deployed in a number of different configurations, in multiple packaging formats.

For any given model/pipeline, the deployment/packaging scenarios can vary widely. For example, a user might want to deploy a TensorFlow model via Konduit Serving in one of these configurations (and many more):
* Docker image packaging using TensorFlow + CUDA 10.1 on an Linux ARM64 system, with serving via HTTP/REST
* A self-contained .exe (with embedded JVM) using SameDiff TensorFlow import to run the model on CPU, on a Windows x86 + AVX2 system with Intel MKL + MKLDNN (OneDNN) included, with serving being performed via gRPC


Currently, packaging for Konduit Serving is done via Maven profiles and Maven modules.
A user selects the combination of dopendencies and functionality they need by enabling a number of profiles and system properties.
For example, building a Windows CPU uber JAR looks something like this:
```
mvn clean package -DskipTests -Puberjar -Pcpu -Ppython -Pnative -Ppmml -Ptensorflow -Dchip=cpu -Djavacpp.platform=windows-x86_64
```
The other packaging options are executed by adding different profiles.

This approach has got us quite far in terms of packaging (enabling flexible packaging options including uber-JARs, Docker, WARs, DEB/RPMs, tar files and .exe files), we are running up against the limits of this approach.

Specifically, this approach has the following problems:
* The combination of options available to users is only going to continue to grow (too many profiles and combinations for devs/users to know about and understand)
* Some combinations are difficult or impossible using just profiles and properties (for example, building a binary for both Windows and Linux, but not Mac or PPC etc)
* It is easy to leave performance on the table - i.e., using ND4J/SameDiff/TensorFlow etc binaries built without AVX support
* Many incompatibilities will only become apparent at runtime (example: build for a CUDA 10.x version only to find that TensorFlow only releases with CUDA 10.y and hence we have a runtime problem)
* Now (with the Data/API rewrite) configuration and execution is separate; the one configuration can be run many different ways. For example, a TensorFlow model could be run with TensorFlow, SameDiff, TVM, or (possibly automated) conversion ONNX, etc. This will be challenging to support via a "profiles and properties" build approach.
* Usability issues: For example, users need to know a lot about the different profiles, configuration, etc to get an optimal (or even functional) deployment - or even know what is possible.
    - An example of this: the user might build an uber-JAR without the PMML profile being enabled, only to discover their JAR can't run their pipeline (that has a PMML model)
* Packaging of custom code, dependencies and other assets (inc. model, vocabulary files etc) is difficult or impossible at present
* Building Konduit Serving artifacts (Uber-JARs, docker images, etc) requires cloning the main source code repo (though we hide this and do it automatically in the case of Python CLI-based source builds)


## Proposal

The scope of this proposal is limited to the creation/packaging of a Konduit Serving uberjar, which may be deployed in many forms (Docker, RPM, WAR, etc).  
Note that non-Java packaging/deployments of pipelines is out of scope (i.e., deploy a pure C++ binary); OSGi support is relevant but only in scope to the extent that an OSGi-based system could work with (or is build on top of) the functionality described in this proposal.

**Proposal Goals**

The goals of this packaging proposal are as follows:
1. To retain and enhance the existing deployment options - uber-jar, docker, WAR, .exe, etc
2. Remove the reliance on "build from source" for constructing Konduit Serving artifacts
3. Enable greater flexibility in the build/deployment configuration
4. To enable custom Java and Python code (and dependencies) to be easily included in a deployment
5. To improve usability and reliability of packaging, in the following ways
    - Remove the reliance on Maven profiles and properties (at least as the only option) for executing builds
    - Automate the selection (or recommendation) of modules to include for a given pipeline (i.e., look at pipeline config, find what's necessary/useful to include)
    - Add validation and checking for common pitfalls such as dependency issues (incompatible with CPU architecture, wrong CUDA version, etc)
    - Make it clear to the user what requirements (in terms of hardware and software), if any, need to be satisfied on the deployment system (requires CUDA 10.1, Java 8+, etc)

**Proposal Overview**

This proposal has a number of parts:
1. A build tool (on top of Gradle, via build.gradle.kts generation) that utilizes a configuration format to actually perform the required build
2. A Konduit Serving build configuration format
3. UI and command line tools for creating a build configuration for user's Pipeline (and then if necessary triggering a build based on the generated build configuration file)
4. A system for packaging custom Java code and dependencies

Note that for usability, where possible we'll make it so the user doesn't have to be aware of the build configuration file - for example, a simple CLI might be used to configure and execute a build. The CLI would generate the configuration, and pass it to the build tool, without the user being aware of the configuration file.
However, for advanced users and use cases (such as system administrators, devops, etc) we will allow the configuration file to be written or modified directly, outside of the CLI/UI workflows that most users will use.

### Part 1 - Build Tool

Given a configuration file that specifies what should be included in the build (details later), the build tool will execute the build/s necessary to create the requested artifacts (JAR/s, docker images, etc).
Note that the term "build tool" may not be an ideal name, as the proposed tool is simply a thin layer on top of Maven - and is not comparable to a "true" build tool like Maven, Gradle, Ant, etc.

Note also that in principle (though this is not proposed for right now) we can have multiple build tools for creating the final artifacts from these  - i.e., the configuration (definition) and the build tool (build execution) are separate.
Until (if) we look at pure C++ deployments, the main possible use for a second build tool would be for OSGi-based deployments. However, this would still be Maven based.

The proposed build tool will generate (and then execute via Gradle) a build.gradle.kts file based on the configuration file.
Similar to the current "modules and profiles" approach, we will continue to use Maven plugins for the actual packaging - i.e., creation of uberjars, etc.

This generated build.gradle.kts file will include:
* A repositories section - `repositories { mavenCentral() }`
* A plugin section -  `plugins { java }`
* A `dependencies { ... }` section, listing the direct dependencies:
    * The required konduit serving modules - konduit-serving-tensorflow, konduit-serving-nd4j, etc
    * Any "native library" / "backend" dependencies (ND4J native/CUDA backends, for example)
    * Logging etc dependencies
* Properties: `sourceCompatibility = 1.8`, `targetCompatibility = 1.8`
* Any other sections as necessary for creating the build artifacts (docker images, WAR files, etc) and utility tasks (enforcing dependency convergence, etc)

One consequence is that all of the "packaging" modules would be removed, in favor of a single `konduit-serving-build` module. i.e., `konduit-serving-docker`, `konduit-serving-rpm`, `konduit-serving-uberjar` etc will no longer exist.

In the future, we will likely allow the build tool to create multiple different artifacts based on one configuration file - i.e., one uberjar for each of a users' target platforms (to output for example 3 separate JAR files, one for each of Linux x86, Linux armhf, Windows x86).

From a usability perspective, note that most users usually won't interact with this build tool directly, instead only touching (or being aware) of the UI/CLI layer on top of it. 

**Gradle vs. Maven**

In the near term, either tool (Gradle or Maven) should be adequate for implementing this proposal build tool.  
Maven has the advantage of being something the team currently has more experience with.  

However, Gradle seems to have the edge in two respects:  
(a) build speed/performance - https://gradle.org/maven-vs-gradle/  
(b) extensibility/flexibility (including coding directly in the build.gradle file)  

The plan is to proceed with Gradle, and if it results in any major blockes we either switch to (or add in parallel) an implementation based on Maven.

As for Gradle - we will use Kotlin instead of Groovy for the generated build files; in practice it won't make much difference (the build configuration is _generated_ not _written by hand_) but Kotlin does provide benefits over Groovy such as better IDE support (due to static typing) hence we get the benefit of auto-completion, easy navigation to source, easier refactoring, etc if we need to work with the generated build.gradle.kts files directly.


### Part 2 - Configuration File

The configuration file should provide information necessary to determine for the build (via a generated build.gradle.kts file), the set of:
- direct dependencies
- plugins
- properties and profiles


To that end, the following information will be included as part of the configuration:
* The Konduit Serving modules to include
* The Konduit Serving version (optional, and defaults to latest if not specified)
* The deployment packaging type(s) - Uber-JAR, Docker, etc - and their associated configuration
* The deployment target(s) - OS, architecture, CPU vs. GPU, etc
* Selected or preferred pipeline step runners (where more than one option exists for one of the included pipeline steps)
* Information necessary to package any required external/custom code, dependencies, files, resources, etc
* Any additional dependency configuration or overrides (such as dependency management, exclusions, etc)
* Metadata such as timestamps, comments, author, etc

JSON/YAML is proposed to be used as the format for the build configuration files.


### Part 3 - CLI

A CLI build tool will be one way for users to configure and build their required deployment artifacts (uber-jar, docker images, etc). Internally (usually without the user being aware), the CLI tool will create a build configuration and pass it to the build tool for execution.

Two modes of operation are proposed for the CLI:
1. Command line style
2. "Wizard" style

The command line style will provide the information necessary to produce the configuration file in a short form. The exact configuration and options will be designed in more detail later, but it will likely look something like the following:
```bash
konduit-build myPipeline.json --modules tensorflow,nd4j,image --deploy docker --docker.config "name=x,version=y" --incudeJava "com.company:mylibrary:1.0.0"
```

The Wizard style of CLI use will guide users through selecting the options for their pipeline. In terms of implementation priority, this will be implementation after the "command line" style of use.
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

The "wizard" style would then output (a) the "command line style" command for what they entered, and optionally (b) the configuration file; it would then execute the build based on the configuration.


### Part 4 - Build UI

The Build UI would be a simple, single-page UI (nothing fancy or feature rich in the near term) that focused on doing three things:  
1. Guiding users through the configuration process for their pipeline  
    The main goal here is to show the user what the required modules are for serving their pipeline, and the options they have for customing the deployment (target platform, selected model runner, configure each step, etc)  
2. Creating the configuration file (though this would be implemented in the back-end based on what the user selects via the UI)  
3. Triggering the build based on the generated configuration file  

Users should be able to load a previously-created build configuration file (partially or completely specified) as a starting point for their pipeline build.  


At a later date, we may add a way to visualize and create pipeline configurations using this UI also. If we were to look at that, it would be after a separate ADR has been proposed and accepted.


Starting and stopping the build UI should be straightforward (assuming the user has Konduit Python package or similar installed):
```
> konduit-build-ui

Konduit Serving build UI launched at: https://localhost:9123/
Use ctrl+c to exit
```

The UI workflow would for the user would be something like:
1. Launch the konduit-build-ui
2. Select the Konduit Serving pipeline to deploy (later: allow "generic TF model" and similar selections instead of providing a pipeline configuration)
3. Select the deployment environment(s) - OS, CPU architecture, CPU vs. GPU, AVX support or not, etc (later: device profiles)
4. Select the pipeline step runners (if multiple are available)  
   Example: when running a TensorFlow model - whether to use TF, SameDiff, TVM, etc to run the model
5. Optionally add custom Java code, Python code, and dependencies  
   Again, Java code/dependencies will be as simple as specifying the GAV coordinates of the user's project.  
   Python packaging and dependencies is TBD, but may be something like a directory + a requirements.txt
6. Optionally, embed files/resources in the deployment artifact (including the model file if required)
7. Select packaging (uberjar, docker, exe, etc)  
   Each selected option should then show configuration relevant to that packaging
8. Click "verify" to check all options and produce a final report  
   This would check dependencies, estimate final file size, verify binary compatibility, etc
9. If necessary, prompt the user for any things they need to explicitly approve (for example, if necessary, accepting licenses for any 3rd party software to be bundled)
10. Click "Build" to execute the build, which would pass the configuration to the build tool to create the final artifacts (uber-jar(s), docker images, etc)

At any point the user would be able to save the current configuration as a YAML/JSON file (and load it back in later).


At each stage, we would only allow the user to select options that are consistent with previous choices (with other options still visible but grayed out).


For step 3, regarding the "device profiles" idea - these would allow users to select things like ("Raspberry pi 4B", "Jetson Nano", "Generic Linux x86_64", and possibly even common cloud VMs) to reduce the amount of knowledge/configuration required to create the pipeline.


### Part 5 - Pipeline Analysis and Module Selection

An important component of both the CLI and UI would be determining what Konduit Serving modules need to be included to execute a pipeline - and what options are available (i.e., which runners could be used to execute the steps contained within).

In the near-term, we could add something to (semi-automatically) track/aggregate execution support/capabilities across all modules - i.e., we'd build a mapping between module names (or rather, PipelineStepRunners) and the PipelineStep (configurations) they can run.
A basic version should not be especially difficult, with the idea that we would encode information like: "SameDiffPipelineStepRunner in module konduit-serving-samediff can execute steps of type 'TensorFlowPipelineStep'".
A more advanced version that actually checks the configuration would be added at a later date (i.e., SameDiffPipelineStepRunner can run _most_ but not _all_ TensorFlow models - so we'll check this at configuration time).


One thing to keep in mind is extensibility - for example, one day we might have custom pipeline steps available via a "Konduit Serving Hub" - code/dependencies for these custom pipeline steps could be pulled in automatically. However this should not substantially alter the basic approach for doing analysis/module selection (in principle simply adding an external web lookup step to determine what can run a given step).



### Part 6: Custom/External Java Code and Dependencies

In some pipelines, a user will want to write custom Java code - for example custom pipeline steps, custom metrics, etc.
Some of this custom Java code will have dependencies (direct and transitive) that also need to be included in the built JAR.
We need an easy system for including both the code and the dependencies in a Konduit Serving pipeline.

For Java, the proposal for handling this is trivially simple:
1. Users package their code as a standard Maven project
2. Users `mvn install` their project, including their custom code, to their local Maven repository  
   Note this need not be an uber-JAR; a standard module/JAR with direct and transitive dependencies is fine
3. Users specify the GAV coordinate (group ID, artifact ID and version) for their custom functionality in the configuration file (or more likely, via the CLI/UI)

An additional (or possible alternative) mechanism that could be added later, would be to provide a way of building a GitHub repository.
i.e., "clone, install, add to Konduit Serving deployment" would be doable in just a couple of lines of configuration. This could be useful for CI/CD based pipelines.

This "install and provide GAV" approach should also work fine for OSGi-based builds/deployments in the future.

### Future ADRs

There are a number of aspects of this packaging system that would need to be worked out in future ADRs.
ADRs may or may not need to be produced for the following components:
* The configuration format
* UI design
* Custom Python code (and dependency) embedding
* Architecture compatibility checking for dependencies with native code (i.e., "if I include dependency X, will it actually work on ARM64, PPC64le, etc?")
* File/resource embedding (and usability isuses - how do user pipelines access these embedded files?)


## Consequences 

### Advantages

* We get a flexible and powerful build system that should enable most/all of our Java-based packaging needs, including improved configuration options/control vs. the current profiles/properties approach
* Improved build reliability via compatibility checks built into the system (move some problems from run time to build/configuration time)
* Improved usability via guiding users through available and compatible options (via CLI or UI)
* Easier debugging of builds (we can see the exact generated standalone pom.build.gradle.kts - no need to work backwards when something goes wrong to try and figure out exactly what was included from where)
  
### Disadvantages

* Some checks will be difficult to implement, and may not be possible to always perform reliably
    - For example: does arbitrary Python library X work on ARM64?
* Adds yet another configuration file/format for (some) users to know about and learn
* The proposed "custom Java packaging via a Maven project/install" might not work as well for Gradle and SBT users? (however an analogous workflow for Gradle/SBT could be added added)

## Discussion

> We should consider basing this tool on Gradle. May be a better match for this than Maven and is easier to extend if necessary.

Decision - use Gradle not Maven. (originally the ADR proposed to use Maven)

Also Gradle may be beneficial if/when we deploy to Android (though there are many other issues for Android deployments to consider beyond just Maven/Gradle).

Note however that the pom.xml/build.gradle won't be generated then reused for long-lived projects or anything, instead being generated just before the build from our config.
So usability, user experience and IDE support shouldn't matter for Maven vs. Gradle.

One benefit is that Alex (who will be either building or overseeing this) and the KS team generally has more Maven experience.

Also: it's not necessarily an either-or decision: we could have both Maven and Gradle build tool implementations, or switch at a later date (without users really being aware of the switch).

Gradle has a pom.xml file generator also which might provide another option here.

> Can we make this build tool "live inside" the parent build tool?

The motivation for this is to enable users to access the Konduit Serving build functionality without any external installation.  
Though `pip install konduit-serving` as a way to get the build tool will probably serve the average Python developer/data scientist OK, it won't be ideal for JVM-based developers. For them, perhaps a Maven plugin or Gradle task would provide good usability:
```
mvn ai.konduit.serving:for-pipeline foo-bar.yaml
gradle doWhateverINeed
```

The ability to do do Konduit Serving builds with only a JVM and Maven or Gradle installed, nothing else.  
Again, it's not either or - we can potentially do both.

Alternative one-liners for getting set up with the Konduit Serving build tools include:  
* `sudo apt-get install konduit-serving`  (for Linux users)  
* A `konduit-buildw` script (i.e., wrapper script like Maven and Gradle wrappers, `mvnw` and `gradlew`)


> Who is actually going to build Konduit Serving from Source?

Few people. Using this build tool would not require or even encourage building from source. Given it's just generating a pom.xml/build.gradle it will work with release versions too without a local copy of the source.


> Who is the target audience for the build tool?

Most users usually won't interact with this build tool directly, instead only touching (or being aware) of the UI/CLI layer on top of it. 
The target of the whole package of functionality (CLI+UI+build tool etc) will be pretty much any KS user who needs more than standard "off-the-shelf" model serving. i.e., anyone who needs custom code/dependencies, or needs to customize the build for specific hardware or to use a specific execution framework, etc.

Down the line I see us expanding this to allow other packaging for other deployment targets (like helping people deploy on android) too.


> Do we expect every single user of Konduit Serving to be building a special build for their deployment?

No, but that's kind of tangential to the proposal. There's nothing stopping us from having off-the-shelf builds/artifacts for common scenarios (linux/win/mac TF CPU, for example) with or without this tool, either distributed directly (via docker hub, or whatever) or simply as a "simple"/default build option.

One day we'll provide OSGi-based deployments too, which will allow for automatic/runtime download and installation of modules and dependencies also.


