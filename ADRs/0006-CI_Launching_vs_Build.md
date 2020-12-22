# CLI Dependencies and Launching

## Status
ACCEPTED - 09/06/2020

Proposed by: Alex Black (08/06/2020)

Discussed with: Shams

## Context

There are multiple ways a user might want to use Konduit Serving to serve a model.
For the sake of this ADR, I'll refer to two use cases as:
1. Immediate deployment case: "Deploy a server for this pipeline right now on this machine via the CLI"
2. Deployment artifact case: "Create an artifact (JAR, docker image, etc) for deployment somewhere else"

The ADR 0005-Packaging_System_Design.md dealt with the "create deployment artifact" (2) case above.  
However, the build system in its current form it does well not serve the needs of the immediate deployment use case 
particularly well, mainly due to its uber-jar design. This causes a few usability problems:
1. Deploying a pipeline with different modules (dl4j vs. samediff vs. TF, or CPU vs. GPU) requires building an uber-jar
2. Uber-jar builds can take a long time (30-120+ seconds, plus download time)
3. Either we always rebuild (slow launches) or we have an uber-JAR cache (potentially lots of unnecessary disk space used)

The old API CLI dependency/launching approach for this use case also had/has the following problems:
1. Requiring the user to build a JAR ahead of time and manually include the modules they need
2. Not being able to launch (for example) both CPU and GPU servers simultaneously without rebuilding the whole Konduit
   Serving uber-jar in between launching the different servers

For the CLI-based "deploy right now" use case, an alternative is proposed.

## Decision

For the "immediate deployment via CLI" scenario, we will not create an uber-jar; instead, we will use the Konduit Serving
build tool to work out the dependencies we need, download them, and return a list of all dependencies (i.e., a list of JAR file
paths) that the Konduit Serving server will be launched with.

This is similar to how IDEs like IntelliJ work. Consider for example the command IntelliJ uses when launching unit tests etc: (some parts omitted)
```
"C:\Program Files\AdoptOpenJDK\jdk-8.0.242.08-hotspot\bin\java.exe" -ea -Dfile.encoding=UTF-8 -classpath "C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2019.3.3\lib\idea_rt.jar;...;C:\DL4J\git\konduit-serving\konduit-serving-models\konduit-serving-deeplearning4j\target\test-classes;...;C:\Users\Alex\.m2\repository\org\nd4j\nd4j-api\1.0.0-SNAPSHOT\nd4j-api-1.0.0-SNAPSHOT.jar;C:\Users\Alex\.m2\repository\com\jakewharton\byteunits\byteunits\0.9.1\byteunits-0.9.1.jar;C:\Users\Alex\.m2\repository\com\google\flatbuffers\flatbuffers-java\1.10.0\flatbuffers-java-1.10.0.jar;C:\Users\Alex\.m2\repository\org\nd4j\protobuf\1.0.0-SNAPSHOT\protobuf-1.0.0-SNAPSHOT.jar;C:\Users\Alex\.m2\repository\commons-net\commons-net\3.1\commons-net-3.1.jar... " com.intellij.rt.junit.JUnitStarter -ideVersion5 -junit4 ai.konduit.serving.deeplearning4j.TestDL4JStep
```
Note the `-classpath "<list of JAR paths>"` component here.

In practice, we won't pass a list of JAR file paths directly due to constraints on the maximum command line length on Windows.  
Instead, we will use a small JAR containing a manifest file, which will list the absolute path of all dependencies:
https://www.baeldung.com/java-jar-manifest  
This single manifest JAR is then passed via the `-classpath <path>` arg during launch.

Note that this exact Manifest JAR approach is also used by IntelliJ as an option for command line shortening to work around
the maximum command line length problem.
    

The key aspects of this design:
**1 - Static CLI JAR**
The CLI JAR is a static JAR without any modules/dependencies that are needed to run pipeline steps; it never gets modified,
rebuilt, etc no matter what type of pipeline is being launched.

**2 - Konduit Serving Build Tool - Downloads and Resolves Dependencies**

When a user launches a server based on some Konduit Serving pipeline configuration, the following occurs:
1. The CLI calls the build tool
2. The build tool resolves all dependencies that need to be included to run that pipeline
3. All direct and transitive dependencies are downloaded as normal via Gradle and stored in their usual location
4. The build tool creates the required manifest JAR

**3 - We introduce a concept of "device profiles" in the CLI**

In practice, this will allow users to switch between different targets when launching (CPU vs. CUDA, and x86 instead of x86_AVX2 if needed
for some reason). Specifically:
1. On the first run of the Konduit Serving CLI, we automatically create a set of appropriate device profiles based on
   hardware and software available on this system. We will also set the default device profile to the CUDA profile if present.   
   In practice, this will usually be just a CPU profile (highest level supported - avx2, avx512, etc) and a CUDA profile
   if a CUDA device is present on the system. For the CUDA profile, we'll also detect if the CUDA is installed (if not,
   we'll use the JavaCPP presets CUDA redist binaries to provide it at runtime, avoiding the need for a manual install)
2. When running, we'll use the default profile unless the user passes a `-p <profile_name>` during launch. That is:  
   `konduit serve -c config.json -p CPU`

In practice most users won't need to worry about device profiles, unless they need:  
(a) to run on CPU only for a GPU-enabled device, or
(b) (very rarely, if ever) need to downgrade the target (for example, x86 instead of x86_avx2 on an avx2 compatible system)
    as a workaround to some issue with an avx2 or higher binary.  

### Example Workflow: Launch Locally

Suppose a user wants to deploy a server for inference, on system without a Konduit Serving installation.  
Here's what that could look like:

```text
$ pip install konduit-serving           #Or any other easy installation method - apt, yum, etc etc

$ konduit serve -c config.json

Konduit Serving
--- Konduit Serving First Run Initialization ---
Detecting hardware... done
  CPU:                 ARM64 (aarch64) - 4 core
  CUDA GPU:            <device name>, 4GB
  CUDA installation:   Found CUDA 10.2 (/usr/local/cuda)

Creating device profiles...
  Profile 1: "CUDA_10.2" - ARM64, CUDA 10.2 installed 
  Profile 2: "CPU"       - ARM64, CPU execution
Creating device profiles complete

Setting default profile: "CUDA_10.2"

Use <some_command> to set default profile or pass "-p <profile>" when launching to override
--- First run initialization complete ---

Launching server using default device profile "CUDA_10.2"
Acquiring dependencies... done

<usual Konduit Serving launch info>
```

Note that users need only 2 lines here to go from a brand new system (no KS install) to hosting a model server using the
 optimal hardware/configuration for that device (i.e., CUDA, or highest supported AVX level for x86 systems, etc).
Furthermore there is no slow "build uberjar" step that delays the launching of the server by 30-120 seconds, on top of
dependency downloading.

### Launching for the "Deployment Artifact" case 

This "manifest JAR" approach can likely be used in other situations:

* Docker: Could use either uber-JAR or switching to an assembly-JAR style (i.e., embed the original/unmodified dependency
  JARs instead of an uberjar)  
* RPM and DEB: As per docker
* Stand-alone .exe: Continue to use uber-JAR approach

If we decide an assembly-JAR style approach is useful for these deployment artifacts, we can implement that at a later date.
 
Also, in principle we can add extra dependencies on top of an uberjar... it may not be an especially elegant design, but
combining uber-jars with 'extra' classpath dependencies may be possible if we ever really need it. However that won't
be something we support for now

### Detecting Hardware and Creating Profiles

Detecting the CPU details should be straightforward, at least on x86-based systems using a library such as OSHI:
https://github.com/oshi/oshi
How well OSHI supports ARM-based platforms is something we need to explore, though Raspberry Pi (armhf) support does
seem to be available: https://github.com/oshi/oshi/issues/864
Falling back on a system utility (`cat /proc/cpuinfo` or similar) is also a possibility here.

Detecting the presence or absence of a compatible CUDA GPU may be harder. When CUDA is installed and available on the
path, this becomes easier (CUDA install -> assume CUDA device present, or parse output of `nvidia-smi`).  
For the "no CUDA installed but CUDA GPU available" case, OSHI may show it up, or there may be a command line based approach
to find it (like cpuinfo).  

In principle this is a solveable problem but additional work is required to find a robust solution for detecting hardware
(including CUDA GPUs, and maybe other GPUs in the future) that will work across all devices and operating systems we expect
to deploy on in practice.



## Consequences

### Advantages

* 2 commands to go from "no Konduit Serving" to "Server Launched"
* Faster builds (no uber-JAR build)
* Less disk space (no uber-JARs with redundant copies of dependencies)
* Easily allows mixing CPU and GPU deployments on the one system
* No need to distribute "pre-built" binaries
* A variant of this idea should be adaptable for use with OSGi if/when we need it

### Disadvantages

* Relying on JARs in Gradle/Maven caches (rarely - might cause a problem if user tries an install or similar command while a server is running?)
* Relies on the build tool hence Gradle (maybe a problem for 'offline/no network' or 'space restricted' deployment scenarios)
  For 'no network' deployments: we probably need to bundle Gradle itself, not just gradlew, then use `gradle --offline`  
  However, we can still use uber-jar style deployments instead of CLI-style deployments in this scenario


