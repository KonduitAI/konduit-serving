# Extend Vert.x Launcher for creating a Konduit Serving CLI

## Status
ACCEPTED

Proposed by: Shams Ul Azeem (18-03-2020)

Discussed with: Paul Dubs, Alex Black

## Context

Currently, we have a main class called [KonduitServingMain](https://github.com/KonduitAI/konduit-serving/blob/45af79d15abe4912ccd81e78c9d215306472036e/konduit-serving-core/src/main/java/ai/konduit/serving/configprovider/KonduitServingMain.java) that is the entrypoint for a konduit-serving application to run. The main command line arguments are defined inside [KonduitServingNodeConfigurer](https://github.com/KonduitAI/konduit-serving/blob/e791741b80721980f8b66a35ed42f20b30612d5c/konduit-serving-core/src/main/java/ai/konduit/serving/configprovider/KonduitServingNodeConfigurer.java) class. We also have an additional [Python CLI](https://github.com/KonduitAI/konduit-serving/blob/7965965b58217f2b4d983fd41aaea013264491ee/python/cli.py) that can be just implemented in Java. Vert.x Launcher supports the ability to start, stop and list running verticles out of the box.

## Decision

- Extend `KonduitServingNodeConfigurer` from [Vert.x Launcher](https://vertx.io/docs/vertx-core/java/#_the_vert_x_launcher) class. 
- Write all the initialization/tear-down logic inside the lifecycle methods of Vert.x Launcher which is present inside both of the above classes (KonduitServingMain and KonduitServingNodeConfigurer). 
- Extend the CLI and write separate classes for every command we want to add to the CLI (see the documentation [here](https://vertx.io/docs/vertx-core/java/#_extending_the_vert_x_launcher)). 
- Depending on the final decided API, register or unregister commands in Vert.x Launcher.

### Example CLI:

The following CLI will be the start for how this will be look:

#### Starting Verticles

Every verticle will be named some sensible value instead of the full classpath so that the verticles can be started with a simple name. For example:

    konduit serve inference-server --config '{}' # With a JSON string
    konduit serve inference-server --config config.json # With a JSON file
    konduit serve inference-server --config config.json --name=mnist-prediction-server # With a server name. If no names are given then a random name
                                                                                    # is generated.

The CLI can also have options for setting up deployment options like:

    konduit serve inference-server --config config.json --instances 3 # For running 3 instances of a Verticle. Usually it runs on a single port with 
                                                                    # round robin fashion requests transfer for load balancing

#### Stopping Verticles

    konduit stop inference --name=mnist-prediction-server # stops the server by the name of "mnist-prediction-server"

#### Listing Verticles

    konduit list # Lists all of the running verticle services with their names, host, port, configuration

#### Inspecting Verticles

Possible details will include:
- configuration
- host and port
- current resource usage

```bash
konduit inspect # Give details of all the running verticles
konduit inspect --name=mnist-prediction-server # Details of a specific verticle
```

#### Running Predictions

#### Predict with JSON (application/json)

    konduit predict --name=mnist-prediction-server --input '{"key":"value"}' # With JSON string
    konduit predict --name=mnist-prediction-server --input input.json # With JSON file

#### Predict with Files (multipart/form-data)

    konduit predict --name=mnist-prediction-server --input file.npy # Numpy
    konduit predict --name=mnist-prediction-server --input file.zip # DL4J
    konduit predict --name=mnist-prediction-server --input image.jspg # Image
    konduit predict --name=mnist-prediction-server --input file1.zip,file2.zip # DL4J with multiple inputs

## Consequences 

### Advantages
- Properly defined exit codes that we will use for log messages
- CLI will be implemented in java, so tests are more easily written as compared to the python CLI tests for which an uber-jar is required.
- Can list processes easily (through the vert.x launcher `list` command).
- Ability to start and stop vert.x server by name/ID.
- Easy CLI lifecycle management.
- Already documented base commands.
- Getting rid of python CLI maintenance.
- Ability for a verticle to join a clustered event-bus without many changes in the CLI. 
- The default CLI contains option for both deployment and vert.x object initialization.
  
### Disadvantages
- Code refactoring.

## Discussion

### 01. How would running KS from Python work?
        
Making sure that we can detect konduit-serving Jar file based on environment variables (currently `KONDUIT_JAR_PATH`) or a default path and if it's not available we'll download it. After that making sure the version corresponding to the python package is the one that's downloaded. After the jar file is in place, all the commands from the python CLI (for starting, stopping, listing servers) will be passed on to the java version of the CLI. Even the --help command will be written in java. This way there's no maintenance of multiple CLIs since all the commands are channeled to just one CLI. If there's a new update needed within the CLI, only the Java version will be extended.

### 02. How does "--instances 3" work in practice?

Vertx handles this internally. `--instances` option is internal to vert.x. It can run multiple verticles on the same port and runs them in the same java process. It distributes them between multiple threads. There's a small repo [here](https://github.com/ShamsUlAzeem/VertxMetricsDemonstrator/blob/master/src/main/java/tests/shamsulazeem/VerticleMetricsDemonstrator.java) that demonstrate how it performs. This is not exactly running on multiple nodes. It's just a way to utilize multiple CPU threads.