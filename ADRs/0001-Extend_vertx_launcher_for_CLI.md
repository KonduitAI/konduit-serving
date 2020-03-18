# Extend Vert.x Launcher for creating a Konduit Serving CLI

## Status
PROPOSAL

Proposed by: Shams Ul Azeem (18-03-2020)

## Context

Currently, we have a main class called [KonduitServingMain](https://github.com/KonduitAI/konduit-serving/blob/45af79d15abe4912ccd81e78c9d215306472036e/konduit-serving-core/src/main/java/ai/konduit/serving/configprovider/KonduitServingMain.java) that is the entrypoint for a konduit-serving application to run. The main command line arguments are defined in [KonduitServingNodeConfigurer](https://github.com/KonduitAI/konduit-serving/blob/e791741b80721980f8b66a35ed42f20b30612d5c/konduit-serving-core/src/main/java/ai/konduit/serving/configprovider/KonduitServingNodeConfigurer.java) class. We also have a [Python CLI](https://github.com/KonduitAI/konduit-serving/blob/7965965b58217f2b4d983fd41aaea013264491ee/python/cli.py).

## Proposal

`KonduitServingNodeConfigurer` can be just an extension of the [Vert.x Launcher](https://vertx.io/docs/vertx-core/java/#_the_vert_x_launcher) class. Also, we can implement methods in this Launcher class that can contain the extra code that's available in both of the above classes (KonduitServingMain and KonduitServingNodeConfigurer). Furthermore, we can extend the CLI and write seperate classes for every command we want to add to the CLI (see the documentation [here](https://vertx.io/docs/vertx-core/java/#_extending_the_vert_x_launcher)). Depending on the final decided API we can register or unregister commands in Vert.x Launcher.

Due to the ability of starting, stopping and listing running verticles (out of the box), the following CLI can be easily created (this is not decided yet, this is just for an example.):

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

Details could include:
- configuration
- host and port
- current resource usage

    konduit inspect # Give details of all the running verticles
    konduit inspect --name=mnist-prediction-server # Details of a specific verticle

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
- Properly defined exit codes that we can use for log messages
- CLI will be implemented in java so we can test is more easily than the python tests for which we have to first have an uber-jar.
- Can list processes easily (through the vert.x launcher `list` command).
- Ability to start and stop vert.x server by name.
- Easy CLI lifecycle management.
- Already documented base commands.
- We can get rid of our python CLI maintenance.
- Ability for a verticle to join a clustered event-bus without many changes in the CLI. 
- The default CLI contains option for both deployment and vert.x object initialization.
  
### Disadvantages
- Code refactoring.