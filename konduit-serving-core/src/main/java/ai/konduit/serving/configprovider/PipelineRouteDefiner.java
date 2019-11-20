/*
 *
 *  * ******************************************************************************
 *  *
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

package ai.konduit.serving.configprovider;
import ai.konduit.serving.metrics.MetricType;
import ai.konduit.serving.executioner.PipelineExecutioner;
import ai.konduit.serving.InferenceConfiguration;
import io.micrometer.core.instrument.LongTaskTimer;
import  io.micrometer.core.instrument.MeterRegistry;
import  ai.konduit.serving.metrics.NativeMetrics;
import ai.konduit.serving.pipeline.PipelineStep;
import  ai.konduit.serving.input.conversion.BatchInputParser;
import ai.konduit.serving.input.adapter.InputAdapter;
import ai.konduit.serving.config.Output.PredictionType;
import ai.konduit.serving.verticles.VerticleConstants;
import ai.konduit.serving.pipeline.step.ModelStep;
import ai.konduit.serving.pipeline.step.PythonStep;
import ai.konduit.serving.pipeline.step.TransformProcessStep;
import ai.konduit.serving.config.Output;
import ai.konduit.serving.config.Input;
import ai.konduit.serving.pipeline.handlers.converter.multi.converter.impl.numpy.VertxBufferNumpyInputAdapter;
import ai.konduit.serving.pipeline.handlers.converter.multi.converter.impl.nd4j.VertxBufferNd4jInputAdapter;
import ai.konduit.serving.pipeline.handlers.converter.multi.converter.impl.image.VertxBufferImageInputAdapter;
import ai.konduit.serving.pipeline.handlers.converter.multi.converter.impl.arrow.ArrowBinaryInputAdapter;

import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.logging.LogbackMetrics;
import io.micrometer.core.instrument.LongTaskTimer.Sample;
import io.vertx.micrometer.backends.BackendRegistries;

import  io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.healthchecks.HealthCheckHandler;

import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.List;
import org.datavec.api.transform.schema.Schema;
import  org.datavec.api.records.Record;

import  org.nd4j.base.Preconditions;

/**
 * Handles setting up a router for doing pipeline based inference.
 *
 * @author Adam Gibson
 */
@lombok.extern.slf4j.Slf4j
@lombok.Getter
public class PipelineRouteDefiner {

    protected PipelineExecutioner pipelineExecutioner;
    protected InferenceConfiguration inferenceConfiguration;
    //cached for columnar inputs, not used in binary endpoints
    protected Schema inputSchema,outputSchema = null;
    protected LongTaskTimer inferenceExecutionTimer,batchCreationTimer;
    protected HealthCheckHandler healthCheckHandler;

    public List<String> inputNames() {
        return pipelineExecutioner.inputNames();
    }

    public List<String> outputNames() {
        return pipelineExecutioner.outputNames();
    }


    /**
     * Define the routes and initialize the internal
     * {@link PipelineExecutioner} based on the passed
     *  in {@link InferenceConfiguration}.
     *  Note this will also initialize the {@link PipelineExecutioner}
     *  property on this class. If you need access to any of the internals,
     *  they are available as getters.
     *
     * Metric definitions  get defined
     * relative to what was configured in the {@link InferenceConfiguration}
     * Everything implementing the {@link MeterBinder}
     * interface can be configured here.
     * Of note are a few specific ones for machine learning including:
     * {@link NativeMetrics}
     * which covers off heap memory allocation among other things.
     *
     * Health checks are automatically added at /healthcheck endpoints
     * @see <a href="https://vertx.io/docs/vertx-health-check/java/">Vertx health checks</a>
     *
     * @param vertx the input vertx instance for setting up
     *              the returned {@link Router} instance and endpoints
     * @param inferenceConfiguration the configuration to use for the {@link PipelineExecutioner}
     * @return the router with the endpoints defined
     */
    public Router defineRoutes(Vertx vertx, InferenceConfiguration inferenceConfiguration) {
        Router router = Router.router(vertx);

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
                            log.error("Unable to setup gpu metrics. Please ensure the gpu dependency has been properly included in the classpath.",e);
                        }
                        break;
                }

            }
        }

        healthCheckHandler = HealthCheckHandler.create(vertx);
        router.get("/healthcheck*").handler(healthCheckHandler);

        router.get("/metrics").handler(io.vertx.micrometer.PrometheusScrapingHandler.create())
                .failureHandler(failureHandler -> {
                    if(failureHandler.failure() != null) {
                        log.error("Failed to scrape metrics",failureHandler.failure());
                    }
                });



        Preconditions.checkNotNull(inferenceConfiguration.getServingConfig(),"Please define a serving configuration.");
        router.post().handler(BodyHandler.create()
                .setUploadsDirectory(inferenceConfiguration.getServingConfig().getUploadsDirectory())
                .setDeleteUploadedFilesOnEnd(true)
                .setMergeFormAttributes(true))
                .failureHandler(failureHandlder -> {
                    if(failureHandlder.statusCode() == 404) {
                        log.warn("404 at route " + failureHandlder.request().path());
                    }
                    else if(failureHandlder.failed()) {
                        if(failureHandlder.failure() != null) {
                            log.error("Request failed with cause ",failureHandlder.failure());
                        }
                        else {
                            log.error("Request failed with unknown cause.");
                        }
                    }
                });



        router.post("/:operation/:inputType")
                .consumes("application/json")
                .produces("application/json").handler(ctx -> {
            PredictionType outputAdapterType = PredictionType.valueOf(ctx.pathParam("operation").toUpperCase());
            if(inputSchema == null) {
                for(PipelineStep pipelineStep : inferenceConfiguration.getPipelineSteps()) {
                    if(pipelineStep instanceof ModelStep) {
                        inputSchema = pipelineStep.inputSchemaForName("default");
                    }
                    if(pipelineStep instanceof PythonStep) {
                        inputSchema = pipelineStep.inputSchemaForName("default");
                    }
                    if(pipelineStep instanceof TransformProcessStep) {
                        inputSchema = pipelineStep.inputSchemaForName("default");
                    }
                }
            }

            if(outputSchema == null) {
                for(PipelineStep pipelineStep : inferenceConfiguration.getPipelineSteps()) {
                    if(pipelineStep instanceof ModelStep) {
                        outputSchema = pipelineStep.outputSchemaForName("default");
                    }
                    else if(pipelineStep instanceof PythonStep) {
                        outputSchema = pipelineStep.outputSchemaForName("default");
                    }
                    if(pipelineStep instanceof TransformProcessStep) {
                        outputSchema = pipelineStep.inputSchemaForName("default");
                    }
                }
            }


            try {
                LongTaskTimer.Sample start = null;
                if(inferenceExecutionTimer != null) {
                    start = inferenceExecutionTimer.start();
                }
                pipelineExecutioner.doInference(
                        ctx,
                        outputAdapterType,
                        ctx.getBody().toString(),
                        inputSchema,
                        null,
                        outputSchema,
                        inferenceConfiguration.getServingConfig().getOutputDataType());
                if(start != null)
                    start.stop();
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
            Map<String, InputAdapter<io.vertx.core.buffer.Buffer, ?>> adapters = getAdapterMap(ctx);

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

                ctx.put(ai.konduit.serving.verticles.VerticleConstants.CONVERTED_INFERENCE_DATA,batch);
                handler.complete();
            },true, result -> ctx.next());

        });

        router.post("/:operation/:inputType")
                .consumes("multipart/form-data")
                .consumes("multipart/mixed")
                .produces("application/json").handler(ctx -> {
            String transactionUUID = ctx.get(ai.konduit.serving.verticles.VerticleConstants.TRANSACTION_ID);

            log.debug("Processing transaction id " + transactionUUID);
            org.datavec.api.records.Record[] inputs = ctx.get(ai.konduit.serving.verticles.VerticleConstants.CONVERTED_INFERENCE_DATA);

            if(inputs == null) {
                ctx.response().setStatusCode(400);
                ctx.response().setStatusMessage("NDArrays failed to de serialize.");
                ctx.next();
                return;
            }

            ctx.vertx().executeBlocking(blockingCall -> {
                try {
                    long nanos = System.nanoTime();
                    io.micrometer.core.instrument.LongTaskTimer.Sample start = null;
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
            Map<String, InputAdapter<io.vertx.core.buffer.Buffer, ?>> adapters = getAdapterMap(ctx);

            String transactionUUID = java.util.UUID.randomUUID().toString();
            ctx.vertx().executeBlocking(handler -> {
                BatchInputParser batchInputParser = BatchInputParser.builder()
                        .converters(adapters)
                        .converterArgs(pipelineExecutioner.getArgs())
                        .inputParts(inputNames())
                        .build();
                try {
                    long nanos = System.nanoTime();
                    io.micrometer.core.instrument.LongTaskTimer.Sample start = null;
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


                } catch (java.io.IOException e) {
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
                    Sample start = null;
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

            } catch(Exception e) {
                log.error("Failed to initialize. Shutting down.",e);
            }

        } else {
            log.debug("Web server and endpoint already initialized.");
        }

        return router;
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



}
