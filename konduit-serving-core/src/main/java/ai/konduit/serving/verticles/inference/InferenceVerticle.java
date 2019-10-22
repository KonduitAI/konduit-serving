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

package ai.konduit.serving.verticles.inference;


import ai.konduit.serving.InferenceConfiguration;
import ai.konduit.serving.executioner.PipelineExecutioner;
import ai.konduit.serving.input.adapter.InputAdapter;
import ai.konduit.serving.input.conversion.BatchInputParser;
import ai.konduit.serving.metrics.MetricType;
import ai.konduit.serving.metrics.NativeMetrics;
import ai.konduit.serving.pipeline.ModelPipelineStep;
import ai.konduit.serving.pipeline.PipelineStep;
import ai.konduit.serving.pipeline.PythonPipelineStep;
import ai.konduit.serving.pipeline.TransformProcessPipelineStep;
import ai.konduit.serving.pipeline.handlers.converter.multi.converter.impl.arrow.ArrowBinaryInputAdapter;
import ai.konduit.serving.pipeline.handlers.converter.multi.converter.impl.image.VertxBufferImageInputAdapter;
import ai.konduit.serving.pipeline.handlers.converter.multi.converter.impl.nd4j.VertxBufferNd4jInputAdapter;
import ai.konduit.serving.pipeline.handlers.converter.multi.converter.impl.numpy.VertxBufferNumpyInputAdapter;
import ai.konduit.serving.config.Input;
import ai.konduit.serving.config.Output;
import ai.konduit.serving.verticles.VerticleConstants;
import ai.konduit.serving.verticles.base.BaseRoutableVerticle;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.logging.LogbackMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.micrometer.PrometheusScrapingHandler;
import io.vertx.micrometer.backends.BackendRegistries;
import lombok.extern.slf4j.Slf4j;
import org.datavec.api.records.Record;
import org.datavec.api.transform.schema.Schema;
import org.nd4j.base.Preconditions;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;


/**
 * A {@link io.vertx.core.Verticle} that takes multi part file uploads
 * as inputs.
 *
 * Many computation graphs are usually multiple inputs
 * by name: Each part for a multi part file upload
 * should map on to a name. For example:
 * input_1 : part name: input_1
 * input_2 : part name: input_2
 *
 * The handler logic for this verticle is implemented
 * in {@link PipelineExecutioner}
 *
 *
 * @author Adam Gibson
 */
@Slf4j
public class InferenceVerticle extends BaseRoutableVerticle {

    private PipelineExecutioner pipelineExecutioner;
    private InferenceConfiguration inferenceConfiguration;
    //cached for columnar inputs, not used in binary endpoints
    private Schema inputSchema, outputSchema = null;
    private LongTaskTimer inferenceExecutionTimer,batchCreationTimer;


    public List<String> inputNames() {
        return pipelineExecutioner.inputNames();
    }

    public List<String> outputNames() {
        return pipelineExecutioner.outputNames();
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        super.start(startFuture);
    }

    @Override
    public void start() throws Exception {
        super.start();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        log.debug("Stopping model server.");
    }

    @Override
    public void init(Vertx vertx, Context context) {
        this.context = context;
        this.vertx = vertx;

        if(router == null) {
            router = Router.router(vertx);
        }

        try {
            inferenceConfiguration = InferenceConfiguration.fromJson(context.config().encode());
        } catch (IOException e) {
            throw new IllegalStateException("Passed in illegal configuration, unable to read InferenceConfiguration object", e);
        }

        MeterRegistry registry = BackendRegistries.getDefaultNow();
        if(registry != null) {
            log.info("Using metrics registry " + registry.getClass().getName() + " for inference");
            inferenceExecutionTimer = LongTaskTimer
                    .builder("inference")
                    .register(registry);

            batchCreationTimer = LongTaskTimer
                    .builder("batch_creation")
                    .register(registry);
        }

        Preconditions.checkNotNull(inferenceConfiguration.getServingConfig(),"Please define a serving configuration.");

        if(inferenceConfiguration.getServingConfig().getMetricTypes() != null && registry != null) {
            //don't add more than one type
            for(MetricType metricType : new HashSet<>(inferenceConfiguration.getServingConfig().getMetricTypes())) {
                switch(metricType) {
                    case CLASS_LOADER:
                        new ClassLoaderMetrics().bindTo(registry);
                        break;
                    case JVM_MEMORY:
                        new JvmMemoryMetrics().bindTo(registry);
                        break;
                    case JVM_GC:
                        new JvmGcMetrics().bindTo(registry);
                        break;
                    case PROCESSOR:
                        new ProcessorMetrics().bindTo(registry);
                        break;
                    case JVM_THREAD:
                        new JvmThreadMetrics().bindTo(registry);
                        break;
                    case LOGGING_METRICS:
                        new LogbackMetrics().bindTo(registry);
                        break;
                    case NATIVE:
                        new NativeMetrics().bindTo(registry);
                        break;
                    case GPU:
                        try {
                            MeterBinder meterBinder = (MeterBinder) Class.forName("ai.konduit.serving.gpu.GpuMetrics").newInstance();
                            meterBinder.bindTo(registry);
                        } catch (Exception e) {
                            throw new IllegalStateException("Unable to setup gpu metrics. Please ensure the gpu dependency has been properly included in the classpath.");
                        }
                        break;
                }
            }
        }

        router.post().handler(BodyHandler.create()
                .setUploadsDirectory(inferenceConfiguration.getServingConfig().getUploadsDirectory())
                .setDeleteUploadedFilesOnEnd(true)
                .setMergeFormAttributes(true))
                .failureHandler(failureHandler -> {
                    if(failureHandler.statusCode() == 404) {
                        log.warn("404 at route " + failureHandler.request().path());
                    } else if(failureHandler.failed()) {
                        if(failureHandler.failure() != null) {
                            log.error("Request failed with cause ", failureHandler.failure());
                        } else {
                            log.error("Request failed with unknown cause.");
                        }

                    }
                });

        router.get("/healthcheck").handler(ctx -> {
            ctx.response().end("Ok");
        });

        router.get("/metrics").handler(PrometheusScrapingHandler.create())
            .failureHandler(failureHandler -> {
                if(failureHandler.failure() != null) {
                    log.error("Failed to scrape metrics",failureHandler.failure());
                }
            });


        router.post("/:operation/:inputType")
                .consumes("application/json")
                .produces("application/json").handler(ctx -> {
            Output.PredictionType outputAdapterType = Output.PredictionType.valueOf(ctx.pathParam("operation").toUpperCase());
            if(inputSchema == null) {
                for(PipelineStep pipelineStep : inferenceConfiguration.getPipelineSteps()) {
                    if(pipelineStep instanceof ModelPipelineStep) {
                        inputSchema = pipelineStep.inputSchemaForName("default");
                    }
                    if(pipelineStep instanceof PythonPipelineStep) {
                        inputSchema = pipelineStep.inputSchemaForName("default");
                    }
                    if(pipelineStep instanceof TransformProcessPipelineStep) {
                        inputSchema = pipelineStep.inputSchemaForName("default");
                    }
                }
            }

            if(outputSchema == null) {
                for(PipelineStep pipelineStep : inferenceConfiguration.getPipelineSteps()) {
                    if(pipelineStep instanceof ModelPipelineStep) {
                        outputSchema = pipelineStep.outputSchemaForName("default");
                    } else if(pipelineStep instanceof PythonPipelineStep) {
                        outputSchema = pipelineStep.outputSchemaForName("default");
                    }

                    if(pipelineStep instanceof TransformProcessPipelineStep) {
                        outputSchema = pipelineStep.inputSchemaForName("default");
                    }
                }
            }

            try {
                LongTaskTimer.Sample start = null;

                if(inferenceExecutionTimer != null) start = inferenceExecutionTimer.start();

                pipelineExecutioner.doInference(
                        ctx,
                        outputAdapterType,
                        ctx.getBody().toString(),
                        inputSchema,
                        null,
                        outputSchema,
                        inferenceConfiguration.getServingConfig().getOutputDataType());

                if(start != null) start.stop();
            } catch (Exception e) {
                log.error("Unable to perform json inference",e);
                ctx.response().setStatusCode(500);
                ctx.response().setStatusMessage("Failed to perform json inference");
                ctx.response().end();
            }
        });

        router.post("/:operation/:inputType")
                .consumes("multipart/form-data")
                .consumes("multipart/mixed").handler(ctx -> {
            Map<String, InputAdapter<Buffer, ?>> adapters = getAdapterMap(ctx);

            BatchInputParser batchInputParser = BatchInputParser.builder()
                    .converterArgs(pipelineExecutioner.getArgs())
                    .converters(adapters)
                    .inputParts(inputNames()).build();

            ctx.vertx().executeBlocking(handler -> {

                long nanos = System.nanoTime();
                Record[] batch = null;
                try {
                    LongTaskTimer.Sample start = null;
                    if(batchCreationTimer != null) {
                        start = batchCreationTimer.start();
                    }

                    batch =  batchInputParser.createBatch(ctx);
                    if(start != null)
                        start.stop();
                } catch(Exception e) {
                    log.error("Unable to convert data for batch",e);

                }

                long endNanos = System.nanoTime();
                if(inferenceConfiguration.serving().isLogTimings()) {
                    log.info("Timing for batch creation was " + TimeUnit.NANOSECONDS.toMillis((endNanos - nanos)) + " milliseconds");
                }
                if(batch == null) {
                    ctx.response().setStatusCode(400);
                    ctx.response().setStatusMessage("NDArrays failed to de serialize.");
                    handler.complete();
                }
                else {
                    log.debug("Created batch for request " );
                }

                ctx.put(VerticleConstants.CONVERTED_INFERENCE_DATA,batch);
                handler.complete();
            },true, result -> ctx.next());

        });


        router.post("/:operation/:inputType")
                .consumes("multipart/form-data")
                .consumes("multipart/mixed")
                .produces("application/json").handler(ctx -> {
            String transactionUUID = ctx.get(VerticleConstants.TRANSACTION_ID);

            log.debug("Processing transaction id " + transactionUUID);
            Record[] inputs = ctx.get(VerticleConstants.CONVERTED_INFERENCE_DATA);

            if(inputs == null) {
                ctx.response().setStatusCode(400);
                ctx.response().setStatusMessage("NDArrays failed to de serialize.");
                ctx.next();
                return;
            }

            ctx.vertx().executeBlocking(blockingCall -> {
                try {
                    long nanos = System.nanoTime();
                    LongTaskTimer.Sample start = null;
                    if(inferenceExecutionTimer != null)
                        start = inferenceExecutionTimer.start();
                    pipelineExecutioner.doInference(
                            ctx,
                            inferenceConfiguration.serving().getOutputDataType(),
                            inputs);

                    if(start != null)
                        start.stop();
                    long endNanos = System.nanoTime();
                    if(inferenceConfiguration.serving().isLogTimings()) {
                        log.info("Timing for inference was " + TimeUnit.NANOSECONDS.toMillis((endNanos - nanos)) + " milliseconds");
                    }

                    blockingCall.complete();
                } catch(Exception e) {
                    log.error("Failed to do inference ",e);
                    ctx.fail(e);
                    blockingCall.fail(e);
                }

            },true, result -> {
                if(result.failed()) {
                    ctx.fail(result.cause());
                }
            });

        });

        router.post("/:inputType/:predictionType")
                .consumes("multipart/form-data")
                .consumes("multipart/mixed").handler(ctx -> {
            Map<String, InputAdapter<Buffer, ?>> adapters = getAdapterMap(ctx);

            String transactionUUID = UUID.randomUUID().toString();
            ctx.vertx().executeBlocking(handler -> {
                BatchInputParser batchInputParser = BatchInputParser.builder()
                        .converters(adapters)
                        .converterArgs(pipelineExecutioner.getArgs())
                        .inputParts(inputNames())
                        .build();
                try {
                    long nanos = System.nanoTime();
                    LongTaskTimer.Sample start = null;
                    if(batchCreationTimer != null) {
                        start = batchCreationTimer.start();
                    }

                    Record[] batch = batchInputParser.createBatch(ctx);
                    if(start != null)
                        start.stop();
                    long endNanos = System.nanoTime();
                    if(inferenceConfiguration.serving().isLogTimings()) {
                        log.info("Timing for batch creation was " + TimeUnit.NANOSECONDS.toMillis((endNanos - nanos)) + " milliseconds");
                    }
                    if(batch == null) {
                        log.warn("Created invalid null batch.");
                    }

                    ctx.put(VerticleConstants.TRANSACTION_ID,transactionUUID);
                    ctx.put(VerticleConstants.CONVERTED_INFERENCE_DATA,batch);
                    handler.complete();


                } catch (IOException e) {
                    ctx.fail(e);
                    log.error("Unable to convert inputs",e);
                    handler.fail(e);
                }
            },true, result -> ctx.next());
        });


        router.post("/:inputType/:predictionType")
                .consumes("multipart/form-data")
                .consumes("multipart/mixed")
                .produces("application/octet-stream").handler((RoutingContext ctx) -> {
            Record[] inputs = ctx.get(VerticleConstants.CONVERTED_INFERENCE_DATA);
            if(inputs == null) {
                log.warn("No inputs found. Bad request");
                ctx.response().setStatusCode(400);
                ctx.response().end("No  inputs found. Bad request.");
                return;
            }

            String outputType = ctx.pathParam("predictionType");
            Output.DataType outputAdapterType = Output.DataType.valueOf(outputType.toUpperCase());
            ctx.vertx().executeBlocking(handler -> {
                try {
                    long nanos = System.nanoTime();
                    LongTaskTimer.Sample start = null;
                    if(batchCreationTimer != null) {
                        start = batchCreationTimer.start();
                    }
                    pipelineExecutioner.doInference(ctx, outputAdapterType, inputs);
                    if(start != null)
                        start.stop();
                    long endNanos = System.nanoTime();
                    if(inferenceConfiguration.serving().isLogTimings()) {
                        log.info("Timing for inference was " + TimeUnit.NANOSECONDS.toMillis((endNanos - nanos)) + " milliseconds");
                    }
                    handler.complete();
                }catch(Exception e) {
                    log.error("Failed to do inference ",e);
                    ctx.fail(e);
                    handler.fail(e);
                }

            },true, result -> {
            });

        });


        if (pipelineExecutioner == null) {
            log.debug("Initializing inference executioner after starting verticle");


            //note that we initialize this after the verticle is started
            //due to needing to sometime initialize retraining routes
            try {
                pipelineExecutioner = new PipelineExecutioner(inferenceConfiguration);
                pipelineExecutioner.init();
                setupWebServer();

            }catch(Exception e) {
                log.error("Failed to initialize. Shutting down.",e);
            }


        } else {
            log.debug("Web server and endpoint already initialized.");
        }
    }

    private Map<String, InputAdapter<Buffer, ?>> getAdapterMap(RoutingContext ctx) {
        Map<String, InputAdapter<Buffer, ?>> adapters = new HashMap<>();
        Input.DataType inputAdapterType = Input.DataType.valueOf(ctx.pathParam("inputType").toUpperCase());
        InputAdapter<Buffer,?> adapter = getAdapter(inputAdapterType);
        for(String inputName : inputNames()) {
            adapters.put(inputName,adapter);
        }
        return adapters;
    }


    private InputAdapter<Buffer,?> getAdapter(Input.DataType inputDataType) {
        switch(inputDataType) {
            case NUMPY:
                return new VertxBufferNumpyInputAdapter();
            case ND4J:
                return new VertxBufferNd4jInputAdapter();
            case IMAGE:
                return new VertxBufferImageInputAdapter();
            case ARROW:
                return new ArrowBinaryInputAdapter();
            default:
                throw new IllegalStateException("Illegal adapter type!");
        }
    }


    protected void setupWebServer() {
        int portValue = inferenceConfiguration.getServingConfig().getHttpPort();
        if(portValue == 0) {
            String portEnvValue = System.getenv(VerticleConstants.PORT_FROM_ENV);
            if(portEnvValue != null) {
                portValue = Integer.parseInt(portEnvValue);
            }
        }

        final int portValueFinal = portValue;
        vertx.createHttpServer()
                .requestHandler(router)
                .exceptionHandler(Throwable::printStackTrace)
                .listen(portValueFinal, inferenceConfiguration.getServingConfig().getListenHost(), listenResult -> {
                    if (listenResult.failed()) {
                        log.debug("Could not start HTTP server");
                        listenResult.cause().printStackTrace();
                    } else {
                        log.debug("Server started on port " + portValueFinal);
                    }
                });
    }

}
