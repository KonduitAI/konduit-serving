# Async Pipelines

## Status
ACCEPTED (18/06/2020)

Proposed by: Alex Black (17/06/2020)

Discussed with: Shams, Paul

## Context

In some machine learning deployments, we need to perform inference periodically, regardless of whether anyone is actively
querying the pipeline from a REST/GRPC etc endpoint.

Examples 1: When processing video, we want to perform inference on each frame, or N times per second, etc regardless of
whether someone calls a REST endpoint or not.

Example 2: When processing video, we want the number of pipeline executions to be independent of the number of clients
querying the REST endpoint (i.e., we should run at 20 FPS and return the latest output/prediction available, regardless of
 whether there is 1 or 100 clients querying per second).

Example 3: anomaly detection use case without any sort of REST/GRPC/MQTT etc server. Imagine we want to perform
inference every second, and (conditionally, if a fault is detected) execute a "HTTP post" pipeline step to report the fault.

Example 4: "I want to perform inference at most 5 times per second. If no requests occur, we shouldn't perform any
inference. If more than 5 requests per second occur, we want to return the last cached result instead"

Example 5: "periodic push based" pipeline execution (microservices style). Suppose we want to perform inference on a camera
feed at 5 FPS and for every frame post the prediction (for example, predicted class or detected objects) to a HTTP endpoint,
a Kafka queue, an MQTT endoint, or simply writing to a file (where these are implemented as pipeline steps).

These use cases aren't yet supported in Konduit Serving.

## Decision

We introduce an additional Pipeline type, `AsyncPipeline`. It implements the `Pipeline` interface (same as `SequencePipeline`
and `GraphPipeline`) so from an API and execution point of view it is the same as the other Pipeline types.

This AsyncPipeline is a Decorator/Wrapper pattern - i.e.,
```java
Pipeline p = SequencePipeline.builder()... .build();
Pipeline asyncPipeline = new AsyncPipeline(p, Trigger);
```

The AsyncPipeline does two things:
* Performs execution of the underlying Pipeline based on some sort of trigger
* (Usually) stores the last output of the underlying pipeline, and returns it when query() is called

Consider an AsyncPipeline set to perform inference of the underlying pipeline once per second. If we query the AsyncPipeline
100 times per second (for example, by 100 difference users all querying the same REST endpoint), we get the same result
returned 100 times, not 100 independent (potentially redundant) execution of the underlying pipeline.  



The "trigger" for the is configurable. It allows different ways of performing inference on the underlying model.
The `Trigger` interface would have the following API:
```text
query(Data) : Data                       - Called when the AsyncPipelineExecutor.exec(...) is called. Returns either a
                                           cached Data instance, or optionally blocks for perform a new inference.
setCallback(Function<Data,Data>) : void  - The function provided here is used by the Trigger to perform execution of the
                                           underlying Pipeline whenever the Trigger wants to (with the provided Data), 
                                           irrespective of whether there is a query() call or not
```

The idea is the Trigger would call the function whenever it wants inference to be performed, whether or not the external
Pipeline/PipelineExecutor has been called or not (i.e., irrespective of whether query(Data) is called or not).
Note that the Trigger instances should be thread safe.

Built-in implementations would initially include: 
* `SimpleLoopTrigger`: performs inference in a loop as fast as possible, unless an optional configuration `frequencyMs`
   option is set (in which case, it calls the underlying pipeline every `frequencyMs` milliseconds).
* `TimeLoopTrigger`: calls every N `TimeUnit`s, with some offset. For example, "Every hour, at the start of the hour", or
  "3 hours past the start of the day, every day", etc 
* `CachingTrigger`: performs inference "at most every N milliseconds". For example, if we say "at most once per 1000ms",
   and we get a query(Data) call at T=0, we block and call the `Function<Data,Data>`. For all subsequent queries up to T=1000ms,
   we return the cached value from the T=0 call. The next call immediately after T=1000ms results in another blocking call
   and an update of the cached value (until T=2000ms, and so on).


### JSON Format

The JSON format for SequencePipeline and GraphPipeline is something that users are supposed to be able to understand, edit
and potentially even write from scratch if they so desire. It is also (with only a few exceptions) programming language
independent.

The SequencePipeline vs. GraphPipeline is differentiated by the form of the "steps" field: if it's a list, it's a SequencePipeline;
if it's an object/map, it's a GraphPipeline.

We can do a simple extension to this idea: Again noting that the AsyncPipeline has a decorator pattern, we either have a
AsyncPipeline(SequencePipeline) or a AsyncPipeline(GraphPipeline).  
In either case, we can simply add a new field  - `@AsyncTrigger`, and otherwise leave the existing JSON format unchanged.
i.e., the presence of this field means that we have a AsyncPipeline decorator. 


## Alternatives

* We use a "setting" style instead of decorator style - i.e., SequencePipeline.setTrigger(Trigger)
  This has the downside that all other pipeline steps need to implement loop execution and caching functionality, which
  is definitely not ideal.
* Trigger is set in InferenceConfiguration (but this doesn't serve the needs of non-serving cases like example 3)


## Consequences 

### Advantages

* We can support all of the use cases described at the beginning of this ADR, and more, reasonably easily
* Serving and embedding Pipelines in other programs doesn't need to know anything about looping, caching, etc etc.
* This design allows us to build general purpose pipelines (image classifiers, object detectors, etc etc) and, only if
  we need it, add control 
* Solves the "redundant executions" and "inference latency" problems for pipelines relying exclusively on video/sensor type
  data (and not external data) 
  
### Disadvantages

* JSON format: Additional decorators (like Something(LoopPipeline(SequencePipeline))) will require rethinking of this 
  "additional field"approach.
  However, I'm not yet aware of any other functionality or use cases that could make use of a decorator-style design

## Discussion

Originally it was proposed to call this "LoopPipeline" but eventually we settled on AsyncPipeline. BackgroundPipeline was
also proposed.

Renamed: LoopTrigger --> Trigger

A decorating executor would probably make more sense in some respects (i.e., LoopPipelineExecutor(SequencePipelineExecutor))
but this has a disadvantage when it comes to serialization - i.e., we need to specify the executor to use (and its config)
with the pipeline, which isn't ideal from a design point of view. 

> For the setCallBackFunction(Function<Data, Data>) how can this be defined into json configuration?
  
This wouldn't need to be serialized in JSON, it would be set by the executor upon initialization. So only the Trigger fields, excluding the function, needs to be JSON serializable.

> Another use case that comes to my mind is security cameras. Maybe we can more step types for real-time streaming protocols
> (RTSP) or ONVIF profiles as they are quiet popular. The idea would be to detect anomalies in the video streams and then
> reporting them into files, databases or other types of outputs. For this we would need additional steps that define the reporting style.

I (Alex) was originally thinking of polling-based via a frame capture step, but some sort of external streaming setup (i.e.,
the Trigger feeds in the image periodically, rather than the pipeline extracting the image as part of execution) would also
work and be supported by this design.