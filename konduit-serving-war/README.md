## Building a war file for distribution

When building a war file for a particular configuration, any combination of the following profiles
should be enabled:

1.  gpu: This will include the nd4j cuda jars.

2.  native: This will include the nd4j cpu jars.

3.  tf-cpu: This will include the tensorflow cpu jars

4.  tf-gpu This will include the tensorflow gpu jars.

Example usage:

```$bash
mvn -Pnative -Ptf-cpu clean install -DskipTests
```

## Configuration

In order to setup the servelet to start vertx, the web.xml must be configured.

A server.properties under bundled with the war file should have the following values that need to be changed:

1.  ai.konduit.serving.class: This is the value of the vertx to be deployed. A valid class is anything
    under the ai.konduit.serving.verticles package

2.  ai.konduit.serving.configpath : This is the absolute path to a config.json file
    for usage with the desired verticle. This is the entrypoint for vertx
    and should be considered the main configuration file.

An example file for syntax net would be:

```bash
ai.konduit.serving.configpath=/tmp/config.json
ai.konduit.serving.class=ai.konduit.serving.syntaxnet.SyntaxNetVerticle
```

Note that the config.json is used for vertx. How you configure this
is relative to the verticle being used. For syntaxnet,
a simple json file with:

```json
{ "httpPort": 8080 }
```

or something similar is all you need. This port is purely for use
by vertx internally. Tomcat or the container will handle
communication with the outside world. This port's requirement
is relative to your IT department's requirements.

## Runtime

At runtime, the servlet:

```$bash
KonduitServlet
```

is run. This will actually contain the instance of vertx and using the configuration in the
web.xml defined above, start the server.

## Usage within tomcat

Before reading this section, please note that you need 2 components:

1. A configured web.xml. This should automatically be handled.
   The only values defined are standard information
   such as the servlet class and the url mapping.

2. A server.properties with the 2 values above defined.

The server.properties can either be put in the classpath
after the war file is deployed under tomcat by looking under:

```bash
 ls $TOMCAT_HOME/webapps/inference/WEB-INF/classes/
```

If this does not have a server.properties in it, put the
configured server.properties under this directory if the
webapp is already expanded.

After the file is loaded, restart tomcat or reload the webapp
to apply the changes.

For accessing the application servlet from a REST api client,
the root URL will be as follows:

```bash
curl http://localhost:8080/CONTEXT_ROOT/APP_ENDPOINT
```

Note that CONTEXT_ROOT here is whatever the name of the war file is
when the war file was built. Right now this is currently inference.
APP_ENDPOINT is how you reach the actual endpoints defined above
such as /process.

A sample curl url would be:

```bash
curl -X POST http://localhost:8080/inference/process
```
