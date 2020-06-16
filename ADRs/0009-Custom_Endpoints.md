# Custom REST Endpoints

## Status
ACCEPTED

Proposed by: Alex Black (12/06/2020)

Discussed with: Paul

## Context

Currently, we have the "/predict" endpoint for performing inference - this takes a Konduit Serving `Data` instance in,
returns a `Data` instance out, in either JSON or binary (protobuf) format. We also have the ability ot create Grafana dashboards
based on our metrics functionality/endpoints. 

Grafana dashboards are totally fine for some use cases (simple passive visualization). However, in we might want:  
(a) more flexibility and control than Grafana is able to provide for displaying predictions/metrics (Grafana doesn't really
    support images or video, other than maybe via custom plugins),  
(b) The ability to provide interactive dashboards, that can also take input, not just show outputs  
(c) the ability to provide input/output in some "application specific" format (for example, allow users to post a raw PDF
    to a custom endpoint, instead of having to do preprocessing on the client side post create image/text via the Konduit
    Serving Data).

Additionally, for developing real apps we might need to (for example) provide a configuration endpoint, or a debugging
endpoint, etc that hosts an actual HTML page the user can view and interact with. 
 

## Decision

This proposal suggests the addition of custom endpoint functionality for Konduit Serving.

The idea: to add a custom endpoint (get/post etc, with or without inteference), users need to:
1. Provide a class that implements an interface - i.e., `MyCustomEndpoint implements HttpEndpoint`
2. Provide the fully qualified class name in their pipeline configuration - i.e., `"customEndpoint" : "com.company.MyCustomEndpoint`  
3. When launching the server, they provide a JAR with their custom code (using the "additionalDependencies" and "additionalClasspath"
   mechanism already proposed for the CLI / build tool) - i.e., the JAR containing the custom endpoint code is provided 
   via a JAR location, or via GAV coordinates


The `HttpEndpoint` interface is defined with just one method:  
`endpoints(Pipeline, PipelineExecutor) : List<Endpoint>`  
This gives the endpoint access to the pipeline/executor for use in the endpoints

The `Endpoint` interface then has the following methods:  
`type() : io.vertx.core.http.HttpMethod` - i.e., GET, POST, etc  
`path() : String` - i.e., "/myEndpoint" - may optionally include path parameters etc
`consumes() : List<String>` - MIME types for the input  
`produces() : List<String>` - MIME types of the response  
`handler() : Handler<RoutingContext>` - i.e., the actual endpoint-handling code.

Note that the endpoints don't necessarily have to call the underlying pipeline / pipeline executor, though some will.  
For example, two use cases supported by this design include:
(a) Static HTML page serving - that may have the option to call another endpoint (example: a form that allows uploading
    of an image to another endpoint)
(b) POST endpoints for proving for example a PDF, that does conversion, calls the executor, and returns a `Data` instance
    or some other type 


## Consequences 

### Advantages

* Allows for apps, dashboards, etc to be easily added on top of Konduit Serving  
  Other than the extra dependency, app deployment and pipeline deployment become basically the same thing
* Greater flexibility compared to 
* Embedded - single process (unlike Grafana which currently is separate process/container hence memory overhead)
* Reasonably simple to support/implement/maintain
* Can still use Grafana as before (for use cases where Grafana is more appropriate - Grafana can still be used)
  
### Disadvantages

* Usable with only Vert.x (not a major problem for now)
* HTTP only (not usable with GRPC etc, but also not a significant problem for now)

## Discussion

Discussion with Paul: 
Spring MVC-style (i.e., annotations) is a possible (better?) approach for defining the path, consumes/produces, etc.  
We also agree to mark it as experimental / "unstable API" - we'll see how this goes in practice, and maybe change the API,
design or details after we've used it in practice in a few projects.

An alternative to this approach is "embedded" Konduit Serving - i.e., instead of running custom endpoints in Konduit Serving,
run Konduit Serving embedded within say a Spring Boot app. Note that this has (for the new API) been something that is planned
to be supported (hence why konduit-serving-pipeline has the core pipeline abstraction, but nothing to do with serving, Vert.x,
etc). This will still be an option in the future.
