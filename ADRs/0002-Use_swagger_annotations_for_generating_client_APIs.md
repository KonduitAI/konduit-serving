# Use Swagger annotations to generate and maintain konduit-serving clients.

## Status
ACCEPTED

Proposed by: Shams Ul Azeem (18-03-2020)

Discussed with: Paul Dubs, Alex Black

## Context

Considering only inferences, we have two endpoints that are responsible to take inputs for prediction.
- application/json
  - /:predictionType/:inputDataFormat # For JSON inputs
- multipart/form-data
  - /:predictionType/:inputDataFormat # For multipart inputs (FILES)

Given those two endpoints we have done a ton of work on creating a python client that (as of right now) needs more proper documentation, examples and maintenance planning for making the APIs more adaptable.

Since, we're planning to have APIs in multiple languages (python, java, C# and others), it might get difficult to maintain and document them separately in the future. 

## Decision

### Using swagger annotations to generate and document client APIs.

[Swagger annotations](https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Annotations) are a quick and easy way to generate openapi specifications from the source code. Annotations apply to classes, methods and arguments. Using this way, it's easier to get rid of client APIs maintenance (generation, documentation and packaging) in different languages.

#### How it works?

By refactoring konduit-serving source code into classes that contain APIs for different types of verticles. For example:

```java
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class InferenceApi {

    @GET
    @Path("/config")
    public InferenceConfiguration getConfig() {
        return new InferenceConfiguration();
    }

    @POST
    @Path("/{predictionType}/{inputDataFormat}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Get inference result with multipart data.",
            tags = {"inference"},
            description = "You can send multipart data for inference where the input names will be the names of the inputs of a transformation process. " +
                    "or a model input and the corresponding files containing the data for each input.",
            responses = {
                    @ApiResponse(description = "Batch output data",
                            responseCode = "200",
                            content = @Content(schema = @Schema(oneOf = {
                                    ClassifierOutput.class,
                                    RegressionOutput.class,
                                    DetectedObjectsBatch.class,
                                    ManyDetectedObjects.class
                            }))
                    ),
            }
    )
    public BatchOutput predict(@PathParam("predictionType") Output.PredictionType predictionType,
                               @PathParam("inputDataFormat") Input.DataFormat inputDataFormat,
                               @Parameter(description = "An array of files to upload.") File[] multipartInput) {
        return new ClassifierOutput();
    }
}
```

This will require similar refactoring for the other verticles and their respective routers. Currently, the following classes will have to be refactored based on the above details: 
- PipelineRouteDefiner
- MemMapRouteDefiner
- ConverterInferenceVerticle
- ClusteredVerticle

#### How it would look at the end?

Having an API that looks like the class above will generate an API specification that will look like: 

```yaml
openapi: 3.0.1
info:
  title: Konduit Serving REST API
  description: RESTful API for various operations inside konduit-serving
  contact:
    name: Konduit AI
    url: https://konduit.ai/contact
    email: hello@konduit.ai
  license:
    name: Apache 2.0
    url: https://github.com/KonduitAI/konduit-serving/blob/master/LICENSE
  version: 0.1.0-SNAPSHOT
externalDocs:
  description: Online documentation
  url: https://serving.oss.konduit.ai
tags:
- name: inference
  description: Tag for grouping inference server operations
- name: convert
  description: Tag for grouping converter operations
- name: memmap
  description: Tag for grouping memory mapping operations
paths:
  /config:
    get:
      operationId: getConfig
      responses:
        default:
          description: default response
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/InferenceConfiguration'
  /{predictionType}/{inputDataFormat}:
    post:
      tags:
      - inference
      summary: Get inference result with multipart data.
      description: You can send multipart data for inference where the input names
        will be the names of the inputs of a transformation process. or a model input
        and the corresponding files containing the data for each input.
      operationId: predict
      parameters:
      - name: predictionType
        in: path
        required: true
        schema:
          type: string
          enum:
          - CLASSIFICATION
          - YOLO
          - SSD
          - RCNN
          - RAW
          - REGRESSION
      - name: inputDataFormat
        in: path
        required: true
        schema:
          type: string
          enum:
          - NUMPY
          - JSON
          - ND4J
          - IMAGE
          - ARROW
      requestBody:
        description: An array of files to upload.
        content:
          multipart/form-data:
            schema:
              type: array
              items:
                type: string
                format: binary
      responses:
        "200":
          description: Batch output data
          content:
            application/json:
              schema:
                oneOf:
                - $ref: '#/components/schemas/ClassifierOutput'
                - $ref: '#/components/schemas/RegressionOutput'
                - $ref: '#/components/schemas/DetectedObjectsBatch'
                - $ref: '#/components/schemas/ManyDetectedObjects'
components:
  schemas:
    InferenceConfiguration:
      type: object
      properties:
        steps:
          type: array
          items:
            $ref: '#/components/schemas/PipelineStep'
        servingConfig:
          $ref: '#/components/schemas/ServingConfig'
        memMapConfig:
          $ref: '#/components/schemas/MemMapConfig'
    MemMapConfig:
      type: object
      properties:
        arrayPath:
          type: string
        unkVectorPath:
          type: string
        initialMemmapSize:
          type: integer
          format: int64
        workSpaceName:
          type: string
    PipelineStep:
      type: object
      properties:
        input:
          $ref: '#/components/schemas/PipelineStep'
        output:
          $ref: '#/components/schemas/PipelineStep'
        outputColumnNames:
          type: object
          additionalProperties:
            type: array
            items:
              type: string
        inputColumnNames:
          type: object
          additionalProperties:
            type: array
            items:
              type: string
        inputSchemas:
          type: object
          additionalProperties:
            type: array
            items:
              type: string
              enum:
              - String
              - Integer
              - Long
              - Double
              - Float
              - Categorical
              - Time
              - Bytes
              - Boolean
              - NDArray
              - Image
        outputSchemas:
          type: object
          additionalProperties:
            type: array
            items:
              type: string
              enum:
              - String
              - Integer
              - Long
              - Double
              - Float
              - Categorical
              - Time
              - Bytes
              - Boolean
              - NDArray
              - Image
        outputNames:
          type: array
          items:
            type: string
        inputNames:
          type: array
          items:
            type: string
    ServingConfig:
      type: object
      properties:
        httpPort:
          type: integer
          format: int32
        listenHost:
          type: string
        outputDataFormat:
          type: string
          enum:
          - NUMPY
          - JSON
          - ND4J
          - ARROW
        uploadsDirectory:
          type: string
        logTimings:
          type: boolean
        includeMetrics:
          type: boolean
        metricTypes:
          type: array
          items:
            type: string
            enum:
            - CLASS_LOADER
            - JVM_MEMORY
            - JVM_GC
            - PROCESSOR
            - JVM_THREAD
            - LOGGING_METRICS
            - NATIVE
            - GPU
    ClassifierOutput:
      type: object
      properties:
        decisions:
          type: array
          items:
            type: integer
            format: int32
        probabilities:
          type: array
          items:
            type: array
            items:
              type: number
              format: double
        labels:
          type: array
          items:
            type: string
        batchId:
          type: string
    RegressionOutput:
      type: object
      properties:
        values:
          type: array
          items:
            type: array
            items:
              type: number
              format: double
        batchId:
          type: string
    DetectedObjectsBatch:
      type: object
      properties:
        centerX:
          type: number
          format: float
        centerY:
          type: number
          format: float
        width:
          type: number
          format: float
        height:
          type: number
          format: float
        predictedClassNumbers:
          type: array
          items:
            type: integer
            format: int32
        predictedClasses:
          type: array
          items:
            type: string
        confidences:
          type: array
          items:
            type: number
            format: float
        batchId:
          type: string
    ManyDetectedObjects:
      type: object
      properties:
        detectedObjectsBatches:
          type: array
          items:
            $ref: '#/components/schemas/DetectedObjectsBatch'
        batchId:
          type: string
          writeOnly: true
    BatchOutput:
      type: object
      properties:
        batchId:
          type: string
          writeOnly: true
```

And from this yaml the clients will be generated using [openapi-generator](https://github.com/OpenAPITools/openapi-generator). For example: 

```bash
java -jar openapi-generator-cli.jar generate -i openapi.yaml -g python -o python_api_client
```

The above command will generate the python clients for us and the related docs for using the API in python.
 This will be a similar process for other languages as well.

## Consequences 

### Advantages
- Documentation and maintenance will be easier.
- Ability to create clients for any language that's supported by openapi-generator.
- Getting rid of konduit-serving codegen for generating python clients.
  
### Disadvantages
- Required code refactoring will take time.
- Strictly adhering to what openapi allows for APIs to be defined, we'll lose a bit of flexibility there.
- Since some generated APIs might be lacking, so we'll have to write a small layer of wrapper APIs on top of the generated code and docs. That will take additional time.

## Discussion

### 01. How are we going to integrate the generated endpoint API docs into Gitbook?
        
Gitbook doesn't have a way to integrate open api specification like readme [did](https://preview.readme.io/), but it's [open-source](https://github.com/readmeio/api-explorer). We can use that to showcase our REST APIs. Other way we can do is to generate [static html](https://raw.githack.com/swagger-api/swagger-codegen/master/samples/html/index.html) pages and update them through CSS. Another option is to use [swagger-ui](https://petstore.swagger.io/?_ga=2.10633204.1573157324.1584626439-1599309769.1584031022). Gitbook does has an option to define endpoint APIs as documented [here](https://docs.gitbook.com/editing-content/rich-content/with-command-palette#api-methods). We'll look into how much we can use Gitbook's functionality to publish our documents first and then look at the other mentioned options.
    
### 02. What are the restrictions we'll face with the usage of swagger-annotations?

One thing is not being able to freely define functions, and their overloads since we'll have to make sure they fit together nicely with swagger annotations. Also, the generated APIs in different languages should be able to adapt our API framework. We can't use all kind of objects since some of those won't make sense for OpenApi. For example, using `Map<String, File>` as this won't work in sending multipart requests because swagger expects a `File[]` instead (these are the small things we'll have to be wary of). Also, we'll have to makes sure the generated APIs are easily extendable.

### 03. What are we going to use wrapper layers for on top of the generated clients?
 If we want numpy/INDArray types in our client APIs, there's no way to specify that in the openapi generator. The custom API layers on top of the generated clients will take care of that. These wrapper layers would be easier to maintain as compared to a fully maintained API client package for a specific language.