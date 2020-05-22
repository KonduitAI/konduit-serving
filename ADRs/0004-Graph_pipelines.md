# Add Graph-Based Pipelines

## Status
ACCEPTED - 22/05/2020

Proposed by: Alex Black (14/05/2020)

Discussed with: (no feedback received)

## Context

In the past, Konduit Serving pipelines have been essentially a 'stack' of pipeline steps - each connected to only to the next: `A -> B -> C` and so on.

However, some use cases require a more complex structure, allowing parallel and/or conditional execution of pipeline steps.

Examples of use cases:
* "Select one of N models" as part of a pipeline
    - A/B split testing (i.e., test different models for different inputs/users)
    - One model per X, selected dynamically (per region, language, time of day, etc)
    - "Select N of M models" might occasionally be useful in some cases (like sensor fusion: image + text + sound in, but these are all optional/unreliable inputs)
* Parallel branches
    - Ensembles of ML models
    - Parallelization: Execute slow steps in parallel to reduce overall pipeline execution time (database access, network communication, etc)
* Fallback models (i.e., "RNN has 100ms to provide a response otherwise we return 'X'"; or "if model fails, return Y")

This ADR proposed the GraphPipeline, which will enable these use cases and more.
The existing SequencePipeline functionality (i.e., "stack of steps" approach) would not be changed by this proposal.

For GraphPipeline, there are a two considerations here:
* Functionality to provide
* API for providing that functionality



## Decision

### Functionality

GraphPipeline will, like SequencePipeline, have a single Data input and a single Data output. This allows for a number of things including:
* Embedding a GraphPipeline within another Pipeline (SequencePipeline or GraphPipeline)
* Same API and serving methods and serving code for SequencePipeline and GraphPipeline
    - i.e., users don't 

Internally, GraphPipeline can have any amount of branching, parallelism, etc.

This single input, single output restriction should not be cause any usability problems due to the fact that Data instances can contain any number of values (i.e., any number of (key,value) pairs). Hence anything we can do with a multi-Data input design can be achieved by combining and splitting Data instances.

It is proposed that we provide support for directed acyclic graphs only - no loops are allowed within graph pipelines.

We provide 5 types of "graph steps"
* Standard (1-to-1): single input, single output - a normal PipelineStep in a graph
* Switch operation (1 to 1-of-N): 1 Data instance in, which is routed to one of N outputs, based on some criteria (value in Data instance, or otherwise)
    - Example use case: A/B testing (switch op selects which model step to use)
- Merge (N-to-1): simply copy the content of all input Data instances into one output Data instance
- Any (N-to-1): simply forwards on the first (possibly only) available Data instance
    - Typically used in conjunction with a switch step, where only one of N branches is executed
- Combine function (N-to-1): An arbitrary N-to-1 function, with or without all the inputs being available first. In addition to allowing custom Java/Python UDFs, we will provide a small number of built-in functions for:
    - Ensemble: allows weighted averaging, etc
    - an integer aggregation selection (`inIdx = argMax(in[i]["score"])`)
    - a timeout condition ("return X if we get the value within N ms, otherwise return Y")

Internally (but not for JSON) Merge and Any will probably be implemented as special cases of CombineFn - so we have really have just 3 types from an implementation perspective (1->1, 1->(1-of-N), N->1).

Examples use cases:
- Any: conditional execution with 2 branches: `in -> Switch(a,b), a->X, b->Y, Merge[Any](X,Y) -> output`. We either execute the left branch (`in->a->X->Merge(ANY)->out`) or the right branch (`in->b->Y->Merge(ANY)->out`)
- CombineFn: Select and return the predictions of the model with the highest probability

We could also introduce a "split" operation, that does 1-to-N by splitting up a single Data instance; in practice we can do this simply by a number of simple "subset" pipeline steps in parallel. For example, `in -> SubsetPipelineStep -> A`, `in -> SubsetPipelineStep -> B`, where SubsetPipelineStep simply copies a subset of the input Data (key,value) pairs to the output Data instance.


Note that routing an input to "N of M" outputs is not easily supported in this proposal (where N changes on each inference step). If needed, it can be added later, or it can be approximated by a series of switch, no-op pipeline steps (returns empty Data), and merge operations.


### API (Java)

The goal of the API is to make it as easy as passible to create graph pipelines, that do exactly (and unambiguously) what users expect.

At least two options exist:
* Functional-style API
* Builder style API (like DL4J ComputationGraphConfiguration)

It is propesd to use a semi-functional API, as follows:

```java
GraphBuilder b = new GraphBuilder();
GraphStep input = b.input();

//Standard PipelineStep:
GraphStep myStep = input.then("myStep", new SomePipelineStep());    //Always require a name

//Merge:
GraphStep merged = myStep.mergeWith("myMerged", input);             //Name is optional 

//Any:
GraphStep any = b.any(step1, step2);                                //Name is optional

//Combine
CombineFn fn = ...
GraphStep combined = b.combine(fn, step1, step2);                   //Name is optional

//Switch: note exact API here is TBD, but it's essentially a Function<List<Data>,Integer> with a numOutputs() method
SwitchFn sf = ...
GraphStep[] switched = b.switch(fn, myStep)


//Construct the final GraphPipeline
Pipeline p = b.build(combined);                                     //Build method takes the final output step

```

Assuming we go with the functional-style design, there's not too many design decisions here, mainly related to naming:

* The method name for adding a step - "then", "call", "followedBy", "inputTo", and probably a lot more are possible
* The method name for merging: "merge", "mergeWith", etc
* The method name for any: "any", "merge", "first", etc
* The method name for combine: "combine", "combineFn", "combineFunction", "aggregate", etc

There's also the concern that "merge" and "combine" are too close in name/meaning, to confuse people. (Suggestions here are welcome)

### API (Python)

In Python, it will be almost idestical, other than being a true functional interface for pipeline steps
```python
b = GraphBuilder()
input = b.input()

# Stardard PipelineStep:
myStep = input("myStep", SomePipelineStep())

# Merge:
merged = myStep.mergeWith("myMerged", input)

# Any:
any = b.any(step1, step2)

# CombineFn
fn = ...
combined = b.combine(fn, step1, step2)

# Switch
sf = ...
switched = b.switch(sf, myStep)

# Construct final GraphPipeline:
p = b(combined)                     #OR: b.build(combined)?

```


### JSON

We should consider JSON part of the public API also - we want people to be able to write graph steps using JSON/YAML by hand.

For SequencePipeline, defining steps is simple: Users just provide an array/list of steps, like so:
```json
{
  "steps" : [ 
      {
        "@type" : "<step type>",
        "config1" : "value1",
        "config2" : "value2"
      },
      {
        "@type" : "<step type>",
        "config1" : "value1",
        "config2" : "value2"
      },
  ]
}
```

For Graph pipelines, we have to encode extra information:
* Graph structure - i.e., names and inputs
* Graph components other than just PipelineSteps

Proposal: We stay as close to the SequnencePipeline representation as possible, changing only the following:
* "steps": Becomes an object (map) not a list. Object keys are step names.
* We add an "@input" (alias: "@inputs") field within each pipeline step
    * For pipeline steps: can take a single value, or a size 1 list
    * For Merge/Any/CombineFn - take a list/array

By calling the field `@input` / `@inputs` we avoid to avoid ambiguity / clashing names when it comes to JSON serialization time for arbitrary configuration classes. That is, if we called it `input` and the user had a field called `input` in their configuration class, we have a problem.



Example JSON for graph pipelines, with 1 pipeline step (connected to pipeline input), and one merge step (connected to input and the step "myStep")
```json
{
    "steps" : {
        "myStep" : {
            "@input" : "input",
            "@type" : "<step type>",
            "config1" : "value1",
            "config2" : "value2"
        },
        "myMerged" : {
            "@input" : ["input", "myStep"],
            "@type" : "MERGE",
        }
    }
}
```


## Consequences

### Advantages

* Should allow us to build the most of complex types of pipelines we need, in Java, Python, JSON and YAML
* Should be simple enough for users to understand and use the 5 types of graph steps (standard/pipelinestep, switch, merge, any, combine)
* After pipeline construction, there's no difference (in terms of API) for SequencePipeline vs. GraphPipeline - i.e., the exact same client and API methods can be used for performing inference on both types of pipeline.
* We can (at a later date) easily add embedded pipelines - i.e., "GraphPipeline in a SequencePipeline" for example

### Disadvantages

* The "input" / "$input" and "type" / "$type" issue mentioned earlier
* The multiple N-to-1 types could be non-obvious at first glance (until users read docs)? (merge vs. any vs. combine)