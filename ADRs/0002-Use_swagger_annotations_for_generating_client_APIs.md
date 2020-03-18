# Use Swagger annotations to generate and maintain konduit-serving clients.

## Status
PROPOSAL

Proposed by: Shams Ul Azeem (18-03-2020)

## Context

Considering only inferences, we have two endpoints that are responsible to take inputs for prediction.
- application/json
  - /:predictionType/:inputDataFormat # For JSON inputs
- multipart/form-data
  - /:predictionType/:inputDataFormat # For multipart inputs (FILES)

Given those two endpoints we have done a ton of work on creating a python client that (as of right now) needs more proper documentation, examples and maintenance planning for getting the APIs to be easily adaptable.

Since, we're planning to have APIs in multiple languages, it might get difficult to maintain and document them seperately in the future. 

## Proposal

Using swagger annotations to solve most of the documentation and client APIs issues.

[Swagger annotations](https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Annotations) are a quick and easy way to generate openapi specifications from the source code. Annotations apply to classes, methods and arguments. Using this way, we'll be able to get ourselves out of maintaning client APIs in different languages.

### How it works?

We'll have to update our source code into classes that contain APIs for different types of verticles. For example:

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

We'll have to do similar changes for the other verticles and their respective routers. Currently, we have the following classes to refactor based on the above details: 
- PipelineRouteDefiner
- MemMapRouteDefiner
- ConverterInferenceVerticle
- ClusteredVerticle

# How it would look at the end?

If we have an API that looks like the class above then we can generate an API specification that will look like: 

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

And from this yaml we can generate our clients using [openapi-generator](https://github.com/OpenAPITools/openapi-generator) by running something like: 

```bash
java -jar openapi-generator-cli.jar generate -i openapi.yaml -g python -o python_api_client
```

The above command will generate the python clients for us and also the related docs for using the API in python. 

## Consequences 

### Advantages
- Documentation and maintenance will be easier.
- Ability to create clients for any language that's supported by openapi-generator.
- We won't have to maintain our own codegen for generating python clients.
  
### Disadvantages
- Required code refactoring and can take time.
- We'll strictly have to adhere to how openapi wants us to define our APIs so, we'll lose a bit of flexibility there.
- Since some of the generated APIs might be lacking so we'll have to write a small layer of wrapper APIs on top of the generate code and docs. So, that will take additional time.