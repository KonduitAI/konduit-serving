/*
 *
 *  * ******************************************************************************
 *  *  * Copyright (c) 2015-2019 Skymind Inc.
 *  *  * Copyright (c) 2019 Konduit AI.
 *  *  *
 *  *  * This program and the accompanying materials are made available under the
 *  *  * terms of the Apache License, Version 2.0 which is available at
 *  *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  *  * License for the specific language governing permissions and limitations
 *  *  * under the License.
 *  *  *
 *  *  * SPDX-License-Identifier: Apache-2.0
 *  *  *****************************************************************************
 *
 *
 */

package ai.konduit.serving.executioner;

import ai.konduit.serving.InferenceConfiguration;
import ai.konduit.serving.input.conversion.ConverterArgs;
import ai.konduit.serving.model.ModelConfig;
import ai.konduit.serving.model.TensorDataType;
import ai.konduit.serving.model.TensorDataTypesConfig;
import ai.konduit.serving.output.types.BatchOutput;
import ai.konduit.serving.output.types.NDArrayOutput;
import ai.konduit.serving.pipeline.config.ObjectDetectionConfig;
import ai.konduit.serving.pipeline.handlers.converter.JsonArrayMapConverter;
import ai.konduit.serving.config.Output;
import ai.konduit.serving.config.ServingConfig;
import ai.konduit.serving.output.adapter.*;
import ai.konduit.serving.pipeline.*;
import ai.konduit.serving.pipeline.step.ImageLoadingStep;
import ai.konduit.serving.pipeline.step.ModelPipelineStep;
import ai.konduit.serving.pipeline.PipelineStep;
import ai.konduit.serving.util.ArrowUtils;
import ai.konduit.serving.util.ObjectMapperHolder;
import ai.konduit.serving.util.SchemaTypeUtils;
import ai.konduit.serving.pipeline.steps.InferenceExecutionerPipelineStepRunner;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.datavec.api.records.Record;
import org.datavec.api.transform.TransformProcess;
import org.datavec.api.transform.schema.Schema;
import org.datavec.api.writable.Text;
import org.datavec.api.writable.Writable;
import org.datavec.api.writable.WritableType;
import org.datavec.arrow.recordreader.ArrowRecord;
import org.datavec.arrow.recordreader.ArrowWritableRecordBatch;
import org.deeplearning4j.zoo.util.Labels;
import org.nd4j.base.Preconditions;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.shade.jackson.core.JsonProcessingException;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;


/**
 * A handler for parsing binary input
 * and then depending on the model loader
 * and model type, (provided by sub classes)
 * the handler will perform inference
 * and send the binary result back down to
 * the user.
 *
 * @author Adam Gibson
 */
@Slf4j
public class PipelineExecutioner {

    private  Pipeline pipeline;
    @Getter
    protected MultiOutputAdapter multiOutputAdapter;
    protected List<String> inputNames, outputNames;
    protected  Map<String, TensorDataType> inputDataTypes, outputDataTypes;

    private TensorDataTypesConfig tensorDataTypesConfig;
    //the output type for the response: default json

    @Getter
    protected Map<String, ConverterArgs> args;

    @Getter
    protected Labels yoloLabels, ssdLabels;

    protected InferenceConfiguration config;

    private Schema inputSchema = null;
    private Schema outputSchema = null;
    private ModelConfig modelConfig = null;
    private ObjectDetectionConfig objectDetectionConfig = null;
    private JsonArrayMapConverter mapConverter = new JsonArrayMapConverter();

    private ClassificationMultiOutputAdapter classificationMultiOutputAdapter = new ClassificationMultiOutputAdapter();
    private RegressionMultiOutputAdapter regressionMultiOutputAdapter = new RegressionMultiOutputAdapter();
    private RawMultiOutputAdapter rawMultiOutputAdapter = new RawMultiOutputAdapter();

    public PipelineExecutioner(InferenceConfiguration inferenceConfiguration) {
        this.config = inferenceConfiguration;
    }


    public InferenceConfiguration config() {
        return config;
    }


    /**
     * Init the pipeline executioner.
     */
    public void init() {
        ServingConfig servingConfig = config.getServingConfig();
        //initialize input and output data types
        this.pipeline = Pipeline.getPipeline(config.getPipelineSteps());
        for (int i = 0; i < config.getPipelineSteps().size(); i++) {
            PipelineStep pipelineStep = config.getPipelineSteps().get(i);
            PipelineStepRunner pipelineStepRunner = pipeline.getSteps().get(i);
            Preconditions.checkNotNull(pipelineStep,"Pipeline step at " + i + " was null!");
            //only use the first input names that appear in the pipeline
            if (inputNames == null && pipelineStep.getInputNames() != null && !pipelineStep.getInputNames().isEmpty()) {
                inputNames = config.getPipelineSteps().get(i).getInputNames();
            }

            //always have output names change to the last defined names in the pipeline
            if (pipelineStep.getOutputNames() != null && !pipelineStep.getOutputNames().isEmpty()) {
                outputNames = config.getPipelineSteps().get(i).getOutputNames();
            }


            if(inputSchema == null && pipelineStep
                    .getInputSchemas() != null &&
                    !pipelineStep
                            .getInputSchemas().isEmpty() &&
                    pipelineStep
                            .getInputSchemas().get("default") != null &&
                    pipelineStep.getInputColumnNames().get("default") != null) {
                inputSchema = SchemaTypeUtils.toSchema(pipelineStep
                                .getInputSchemas().get("default"),
                        pipelineStep
                                .getInputColumnNames().get("default"));
            }

            if(outputSchema == null && pipelineStep
                    .getOutputSchemas() != null &&
                    pipelineStep
                            .getOutputSchemas().get("default") != null
                    &&
                    !pipelineStep
                            .getOutputSchemas().isEmpty() &&
                    pipelineStep.getInputColumnNames().get("default") != null) {
                outputSchema = SchemaTypeUtils.toSchema(pipelineStep
                                .getOutputSchemas().get("default"),
                        pipelineStep
                                .getOutputColumnNames().get("default"));

            }

            if(pipelineStepRunner instanceof InferenceExecutionerPipelineStepRunner) {
                ModelPipelineStep modelPipelineStepConfig = (ModelPipelineStep) pipelineStep;
                modelConfig = modelPipelineStepConfig.getModelConfig();
                tensorDataTypesConfig = modelConfig.getTensorDataTypesConfig();
            }

            if(pipelineStep instanceof ImageLoadingStep) {
                ImageLoadingStep imageLoadingStepConfig = (ImageLoadingStep) pipelineStep;
                objectDetectionConfig = imageLoadingStepConfig.getObjectDetectionConfig();
            }


        }

        initDataTypes();


        try {
            if(servingConfig.getOutputDataType() == Output.DataType.JSON) {
                multiOutputAdapter = outputAdapterFor(config().serving().getPredictionType(), objectDetectionConfig);
            }
            else {
                log.info("Skipping initialization of multi input adapter due to binary output.");
            }
        } catch (Exception e) {
            log.error("Error initializing output adapter.",e);

        }


        if (servingConfig.getInputDataType() == null) {
            throw new IllegalStateException("Please define an input data type!");
        }

        if (modelConfig != null && modelConfig.getModelConfigType().getModelType() != ModelConfig.ModelType.PMML
                && (inputNames == null || inputNames.isEmpty())) {
            throw new IllegalStateException("No inputs defined! Please specify input names for your verticle via the model configuration.");
        }

    }


    public List<String> inputNames() {
        return inputNames;
    }

    public List<String> outputNames() {
        return outputNames;
    }


    private MultiOutputAdapter outputAdapterFor(Output.PredictionType predictionType, ObjectDetectionConfig objectDetectionConfig) throws Exception {
        MultiOutputAdapter multiOutputAdapter;
        //custom labels input stream for custom labels for yolo and ssd
        InputStream customLabelsInputStream = null;


        if (objectDetectionConfig != null && objectDetectionConfig.getLabelsPath() != null) {
            customLabelsInputStream = new FileInputStream(objectDetectionConfig.getLabelsPath());
            ssdLabels = SSDOutputAdapter.getLabels(customLabelsInputStream, objectDetectionConfig.getNumLabels());
            yoloLabels = ssdLabels;
        }


        switch (predictionType) {
            case CLASSIFICATION:
                multiOutputAdapter = new ClassificationMultiOutputAdapter();
                break;
            case YOLO:
                Preconditions.checkState(objectDetectionConfig != null,
                        "Missing object recognition configuration!");
                multiOutputAdapter = YOLOOutputAdapter.builder()
                        .labels(yoloLabels)
                        .boundingBoxPriors(objectDetectionConfig.getPriors())
                        .inputShape(objectDetectionConfig.getInputShape())
                        .numLabels(objectDetectionConfig.getNumLabels())
                        .threshold(objectDetectionConfig.getThreshold())
                        .build();
                break;
            case SSD:
                Preconditions.checkState(objectDetectionConfig != null,
                        "Missing object recognition configuration!");
                if (customLabelsInputStream == null)
                    multiOutputAdapter = new SSDOutputAdapter(objectDetectionConfig.getThreshold(),
                            objectDetectionConfig.getNumLabels());
                else
                    multiOutputAdapter = new SSDOutputAdapter(objectDetectionConfig.getThreshold(),
                            customLabelsInputStream, objectDetectionConfig.getNumLabels());
                break;
            case RAW:
                multiOutputAdapter = new RawMultiOutputAdapter();
                break;
            case REGRESSION:
                multiOutputAdapter = new RegressionMultiOutputAdapter();
                break;
            default:
                throw new IllegalStateException("Illegal type for output type " + config.serving().getPredictionType());
        }

        return multiOutputAdapter;
    }

    /**
     * Perform inference for the
     * endpoint using the inference executioner.
     * @param ctx                the routing context to use representing the current request
     * @param responseOutputType the {@link Output.DataType} for the output
     * @param inputs             the inputs based on the input data
     */
    public void doInference(io.vertx.ext.web.RoutingContext ctx, Output.DataType responseOutputType, org.datavec.api.records.Record[] inputs) {
        if(inputs == null || inputs.length < 1 || inputs[0] == null) {
            throw new IllegalStateException("No inputs specified!");
        }


        String batchId = UUID.randomUUID().toString();
        long nanos = System.nanoTime();
        INDArray[] execute = pipeline.doPipelineArrays(inputs);
        long after = System.nanoTime();
        if(config.serving().isLogTimings()) {
            long diff = after - nanos;
            long millis = TimeUnit.NANOSECONDS.toMillis(diff);
            log.info("Post internal execution timing in ms " + millis);
        }


        if (multiOutputAdapter != null) {
            log.debug("Performing adaption.");
            Map<String, BatchOutput> adapt = null;
            try {
                adapt = multiOutputAdapter.adapt(execute, outputNames, ctx);
            } catch (Exception e) {
                log.error("Unable to adapt output", e);
                ctx.response().setStatusCode(500);
                ctx.response().setStatusMessage("Was unable to adapt output.");
                ctx.response().end();
                for(int i = 0; i < execute.length; i++) {
                    if(execute[i].closeable())
                        execute[i].close();
                }
                return;
            }

            timedResponse(ctx, responseOutputType, batchId, execute, adapt);

        } else {
            /**
             * Note that this handles binary responses.
             */
            Map<String, BatchOutput> namedBatchOutput = new HashMap<>();
            for (int i = 0; i < outputNames.size(); i++) {
                namedBatchOutput.put(outputNames.get(i), NDArrayOutput.builder().ndArray(execute[i]).build());
            }

            timedResponse(ctx, responseOutputType, batchId, execute, namedBatchOutput);
        }
    }


    /**
     * Perform inference
     * @param ctx the routing context
     * @param outputAdapterType the output adapter
     *                          to use on output
     * @param input the input string (json generally)
     * @param conversionSchema the schema to convert the
     *                         json
     * @param transformProcess the transform process to use
     * @param outputSchema the output schema
     * @param outputDataType the output data type for the pipeline
     */
    public void doInference(RoutingContext ctx,
                            Output.PredictionType outputAdapterType,
                            String input,
                            Schema conversionSchema,
                            TransformProcess transformProcess,
                            Schema outputSchema, Output.DataType outputDataType) {

        Preconditions.checkNotNull(input,"Input data was null!");

        if(input.charAt(0) == '{') {
            //json object
            log.info("Auto converting json object to json array");
            input = "[" + input + "]";
        }

        JsonArray jsonArray = new JsonArray(input);
        ArrowWritableRecordBatch convert = null;
        try {
            convert = mapConverter.convert(conversionSchema, jsonArray, transformProcess);
        }catch(Exception e) {
            log.error("Error performing conversion",e);
            throw e;
        }

        Preconditions.checkNotNull(convert,"Conversion was null!");
        Record[] pipelineInput = new Record[convert.size()];
        for(int i = 0; i < pipelineInput.length; i++) {
            pipelineInput[i] = new ArrowRecord(convert,i,null);
        }

        Record[] records = pipeline.doPipeline(pipelineInput);
        Writable firstWritable = records[0].getRecord().get(0);
        if(firstWritable.getType() == WritableType.NDArray) {
            INDArray[] arrays = SchemaTypeUtils.toArrays(records);
            Map<String, BatchOutput> adapt;
            switch(outputAdapterType) {
                case CLASSIFICATION:
                    adapt = classificationMultiOutputAdapter.adapt(arrays, outputNames(), ctx);
                    break;
                case REGRESSION:
                    adapt = regressionMultiOutputAdapter.adapt(arrays,outputNames(), ctx);
                    break;
                case RAW:
                    adapt = rawMultiOutputAdapter.adapt(arrays,outputNames(),ctx);
                    break;
                default:
                    throw new IllegalStateException("Illegal type for json.");
            }

            writeResponse(adapt, Output.DataType.JSON,UUID.randomUUID().toString(),ctx);

        }
        else if(records.length == 1 &&  records[0].getRecord().get(0) instanceof Text) {
            if(outputDataType == Output.DataType.JSON) {
                JsonObject writeJson = new JsonObject();
                for(int i = 0; i < records[0].getRecord().size(); i++) {
                    Text text = (Text) records[0].getRecord().get(i);

                    if(text.toString().charAt(0) == '{') {
                        JsonObject jsonObject1 = new JsonObject(text.toString());
                        writeJson.put(outputSchema.getName(i),jsonObject1);
                    }
                    else if(text.toString().charAt(0) == ']'){
                        JsonArray jsonObject = new JsonArray(text.toString());
                        writeJson.put(outputSchema.getName(i),jsonObject);
                    }

                    else {
                        writeJson.put(outputSchema.getName(i),text.toString());
                    }
                }

                log.info("Writing json response.");
                String write = writeJson.encodePrettily();
                ctx.response().putHeader("Content-Type", "application/json");
                ctx.response().putHeader("Content-Length", String.valueOf(write.getBytes().length));
                ctx.response().end(write);
            }
            else if(outputDataType == Output.DataType.ARROW){
                writeArrowResponse(ctx, outputSchema, convert);
            }
            else {
                throw new IllegalStateException("Illegal data type response " + outputDataType);
            }


        }
        else if(outputDataType == Output.DataType.JSON) {
            JsonArray newArray = new JsonArray();
            for(Record record : records) {
                JsonObject row = new JsonObject();
                List<Writable> writables = record.getRecord();
                if(outputSchema != null) {
                    for(int i = 0; i < writables.size(); i++) {
                        switch(outputSchema.getType(i)) {
                            case Integer:
                            case Long:
                                row.put(outputSchema.getName(i),writables.get(i).toInt());
                                break;
                            case Float:
                                row.put(outputSchema.getName(i),writables.get(i).toFloat());
                                break;
                            case Double:
                                row.put(outputSchema.getName(i),writables.get(i).toDouble());
                                break;
                            case Boolean:
                                row.put(outputSchema.getName(i),Boolean.parseBoolean(writables.get(i).toString()));
                                break;
                            default:
                                row.put(outputSchema.getName(i),writables.get(i).toString());

                        }
                    }
                }
                else {
                    for(int i = 0; i < writables.size(); i++) {
                        row.put(String.valueOf(i),writables.get(i).toString());
                    }
                }

                newArray.add(row);
            }

            log.info("Writing json response.");
            String write = newArray.encodePrettily();
            ctx.response().putHeader("Content-Type", "application/json");
            ctx.response().putHeader("Content-Length", String.valueOf(write.getBytes().length));
            ctx.response().end(write);

        }
        else if(outputDataType == Output.DataType.ARROW) {
            writeArrowResponse(ctx, outputSchema, convert);
        }
    }


    public void destroy() {
        pipeline.destroy();
    }

    private void writeArrowResponse(RoutingContext ctx, Schema outputSchema, ArrowWritableRecordBatch convert) {
        log.info("Writing arrow response.");
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ArrowUtils.writeRecordBatchTo(convert,outputSchema,byteArrayOutputStream);
        Buffer write = Buffer.buffer(byteArrayOutputStream.toByteArray());
        ctx.response().putHeader("Content-Type", "application/octet-stream");
        ctx.response().putHeader("Content-Length", String.valueOf(write.getBytes().length));
        ctx.response().end(write);
    }

    private void timedResponse(RoutingContext ctx,
                               Output.DataType responseOutputType,
                               String batchId,
                               INDArray[] execute,
                               Map<String, BatchOutput> adapt) {
        long nanos;
        long after;
        nanos = System.nanoTime();
        writeResponse(adapt, responseOutputType, batchId, ctx);
        after = System.nanoTime();
        if(config.serving().isLogTimings()) {
            long diff = after - nanos;
            long millis = TimeUnit.NANOSECONDS.toMillis(diff);
            log.info("Post write response timing in ms " + millis);
        }

        for (INDArray indArray : execute) {
            if (indArray.closeable())
                indArray.close();
        }
    }

    /**
     * Write a response using
     * the multi adapter output.
     * The default is json
     *
     * @param adapt   the multi adapter output
     * @param responseOutputType  the output type to write the response with
     * @param batchId the batch id
     * @param ctx     the routing context
     */
    protected void writeResponse(Map<String, BatchOutput> adapt,
                                 Output.DataType responseOutputType,
                                 String batchId,
                                 RoutingContext ctx) {

        if (responseOutputType == Output.DataType.JSON) {
            if (adapt == null) {
                log.warn(" Adapt output was null!");
                ctx.response().setStatusCode(500);
                ctx.response().end("Adaption output was null! ");
                return;
            }

            JsonObject jsonObject = new JsonObject();
            for (Map.Entry<String, BatchOutput> entry : adapt.entrySet()) {

                entry.getValue().setBatchId(batchId);

                try {
                    jsonObject.put(entry.getKey(),
                            new JsonObject(ObjectMapperHolder.getJsonMapper()
                                    .writeValueAsString(entry.getValue())));
                } catch (JsonProcessingException e) {
                    log.error("Unable to process json for value " + entry.getValue(), e);
                    ctx.response().setStatusCode(500);
                    ctx.response().end("Unable to process json for value " + entry.getValue());
                    return;
                }
            }

            String resp = jsonObject.encodePrettily();
            try {
                ctx.response().putHeader("Content-Type", "application/json");
                ctx.response().putHeader("Content-Length", String.valueOf(resp.getBytes().length));
                ctx.response().end(jsonObject.encodePrettily());
                log.info("Json response end");
            } catch (Exception e) {
                ctx.fail(e);
            }
        } else {
            if (adapt.size() > 1) {
                Buffer buffer = zipBuffer(adapt, responseOutputType);
                writeBinary(buffer, batchId, ctx);
            } else {
                Map.Entry<String, BatchOutput> entry = adapt.entrySet().iterator().next();
                writeBinary(convertBatchOutput(entry.getValue(), responseOutputType), batchId, ctx);
            }
        }

    }


    private void writeBinary(Buffer buffer, String batchId, RoutingContext ctx) {
        try {
            ctx.response().putHeader("Content-Type", "application/octet-stream");
            ctx.response().putHeader("Content-Length", String.valueOf(buffer.length()));
            ctx.response().end(buffer);
        }catch(Exception e) {
            ctx.fail(e);
        }

    }


    private void initDataTypes() {
        if (tensorDataTypesConfig != null && tensorDataTypesConfig.getInputDataTypes() != null) {
            Map<String,TensorDataType> types = tensorDataTypesConfig.getInputDataTypes();
            if(types != null && types.size() >= 1 && inputDataTypes == null)
                inputDataTypes = initDataTypes(inputNames, types, "default");
        }

        if (tensorDataTypesConfig != null && tensorDataTypesConfig.getOutputDataTypes() != null) {
            Map<String,TensorDataType> types = tensorDataTypesConfig.getOutputDataTypes();
            if(types != null && types.size() >= 1 && outputDataTypes == null)
                outputDataTypes = initDataTypes(outputNames, types, "output");
        }

    }

    private Map<String, TensorDataType> initDataTypes(List<String> namesValidation, Map<String, TensorDataType> types, String inputOrOutputType) {
        Preconditions.checkNotNull(namesValidation,"Names validation must not be null!");
        Preconditions.checkNotNull(types,"Types must not be null!");
        Preconditions.checkNotNull(inputOrOutputType,"inputOrOutputType must not be null!");

        Map<String,TensorDataType> ret;
        if (namesValidation == null) {
            log.warn("Unable to validate number of {} data types. No names specified", inputOrOutputType);
        } else if (types.size() != namesValidation.size()) {
            throw new IllegalStateException(String.format("%s names specified does not match number " +
                    "of %s data types specified", namesValidation, inputOrOutputType));
        }

        ret = new LinkedHashMap<>();
        ret.putAll(types);
        return ret;
    }

    /**
     * Create a zip file buffer based on the given
     * adapted output
     * @param adapt the adapted output
     *              from {@link RawMultiOutputAdapter}
     * @param responseOutputType the response type
     * @return the zip file with each output's name being an entry in the zip file.
     */
    public static Buffer zipBuffer(Map<String,BatchOutput> adapt, Output.DataType responseOutputType) {
        try(ByteArrayOutputStream baos = new ByteArrayOutputStream();
            java.util.zip.ZipOutputStream out = new java.util.zip.ZipOutputStream(baos)) {
            for (Map.Entry<String, BatchOutput> outputEntry : adapt.entrySet()) {
                java.util.zip.ZipEntry zipEntry = new java.util.zip.ZipEntry(outputEntry.getKey());
                try {
                    out.putNextEntry(zipEntry);
                    Buffer write = convertBatchOutput(outputEntry.getValue(), responseOutputType);
                    out.write(write.getBytes(), 0, write.getBytes().length);
                    out.closeEntry();
                } catch (java.io.IOException e) {
                    e.printStackTrace();
                }

            }

            out.flush();
            out.close();

            return Buffer.buffer(baos.toByteArray());


        } catch (java.io.IOException e) {
            e.printStackTrace();
        }




        return null;

    }


    /**
     * Convert a batch output {@link NDArrayOutput}
     * given a {@link Output.DataType}
     * @param batchOutput the batch output to convert
     * @param responseOutputType the response type
     * @return converted buffer
     */
    public static Buffer convertBatchOutput(BatchOutput batchOutput, Output.DataType responseOutputType) {
        NDArrayOutput ndArrayOutput = (NDArrayOutput) batchOutput;
        return convertBatchOutput(ndArrayOutput.getNdArray(),responseOutputType);
    }



    /**
     * Convert a {@link INDArray}
     * given a {@link Output.DataType}
     * @param input the batch ndarray to convert
     * @param responseOutputType the response type
     * @return converted buffer
     */
    public static Buffer convertBatchOutput(INDArray input, Output.DataType responseOutputType) {
        Preconditions.checkNotNull(input,"Input was null!");
        Preconditions.checkNotNull(responseOutputType,"Response output type was null!");
        Buffer ret = null;

        switch(responseOutputType) {
            case NUMPY:
                ret = Buffer.buffer(io.netty.buffer.Unpooled.wrappedBuffer(java.nio.ByteBuffer.wrap(org.nd4j.linalg.factory.Nd4j.toNpyByteArray(input))));
                break;
            case ND4J:
                java.nio.ByteBuffer byteBuffer2 = org.nd4j.serde.binary.BinarySerde.toByteBuffer(input);
                ret = Buffer.buffer(io.netty.buffer.Unpooled.wrappedBuffer(byteBuffer2));
                break;
            case ARROW:
                org.apache.arrow.flatbuf.Tensor tensor = org.nd4j.arrow.ArrowSerde.toTensor(input);
                ret = Buffer.buffer(io.netty.buffer.Unpooled.wrappedBuffer(tensor.getByteBuffer()));
                break;

        }

        return ret;
    }


}
