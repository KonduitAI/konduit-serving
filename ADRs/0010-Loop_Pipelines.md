# Loop Pipelines

## Status
PROPOSED

Proposed by: Alex Black (17/06/2020)

Discussed with: 

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

## Proposal

We introduce an additional Pipeline type, `LoopPipeline`. It implements the `Pipeline` interface (same as `SequencePipeline`
and `GraphPipeline`) so from an API and execution point of view it is the same as the other Pipeline types.

This LoopPipeline is a Decorator/Wrapper pattern - i.e.,
```java
Pipeline p = SequencePipeline.builder()... .build();
Pipeline loopPipeline = new LoopPipeline(p, LoopTrigger);
```

The LoopPipeline does two things:
* Performs execution of the underlying Pipeline based on some sort of trigger
* Stores the last output of the underlying pipeline, and returns it.

Consider a LoopPipeline set to perform inference once per second. If we query the LoopPipeline 100 times per second (for
example, by 100 difference users all querying the same REST endpoint), we get the same result returned 100 times.  



The "trigger" for the is configurable. It allows different ways of performing inference.
The `LoopTrigger` interface would have the following API:
```text
query(Data) : Data                       - Called when the LoopPipeline PipelineExecutor.exec(...) is called. Returns either
                                           cached Data instance, or blocks for perform a new inference.
setCallback(Function<Data,Data>) : void  - This is used to trigger execution of the underlying Pipeline whenever the LoopTrigger
                                           wants to (with the provided Data), irrespective of whether there is a query() call
                                           or not
```

The idea is the LoopTrigger would call the function whenever it wants inference to be performed, whether or not the external
Pipeline/PipelineExecutor has been called or not (i.e., irrespective of whether query(Data) is called or not).
Note that the LoopTrigger instances should be thread safe.

Built-in implementations would initially include: 
* `SimpleLoopTrigger`: performs inference in a loop as fast as possible, unless an optional configuration `frequencyMs`
   option is set (in which case, it calls the underlying pipeline every `frequencyMs` milliseconds).
* `TimeLoopTrigger`: calls every N `TimeUnit`s, with some offset. For example, "Every hour, at the start of the hour", or
  "3 hours past the start of the day, every day", etc 
* `CachingLoopTrigger`: performs inference "at most every N milliseconds". For example, if we say "at most once per 1000ms",
   and we get a query(Data) call at T=0, we block and call the `Function<Data,Data>`. For all subsequent queries up to T=1000ms,
   we return the cached value from the T=0 call. The next call immediately after T=1000ms results in another blocking call
   and an update of the cached value (until T=2000ms, and so on).


### JSON Format

The JSON format for SequencePipeline and GraphPipeline is something that users are supposed to be able to understand, edit
and potentially even write from scratch if they so desire. It is also (with only a few exceptions) programming language
independent.

The SequencePipeline vs. GraphPipeline is differentiated by the form of the "steps" field: if it's a list, it's a SequencePipeline;
if it's an object/map, it's a GraphPipeline.

We can do a simple extension to this idea: Again noting that the LoopPipeline has a decorator pattern, we either have a
LoopPipeline(SequencePipeline) or a LoopPipeline(GraphPipeline).  
In either case, we can simply add a new field  - `@LoopTrigger`, and otherwise leave the existing JSON format unchanged.
i.e., the presence of this field means that we have a LoopPipeline decorator. 


## Alternatives

* We use a "setting" style instead of decorator style - i.e., SequencePipeline.setLoopTrigger(LoopTrigger)
  This has the downside that all other pipeline steps need to implement loop execution and caching functionality, which
  is definitely not ideal.
* LoopTrigger is set in InferenceConfiguration (but this doesn't serve the needs of non-serving cases like example 3)


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

