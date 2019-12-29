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

import ai.konduit.serving.InferenceConfiguration;
import ai.konduit.serving.config.Input;
import ai.konduit.serving.config.Output;
import ai.konduit.serving.config.Output.PredictionType;
import ai.konduit.serving.executioner.PipelineExecutioner;
import ai.konduit.serving.input.adapter.InputAdapter;
import ai.konduit.serving.input.conversion.BatchInputParser;
import ai.konduit.serving.metrics.MetricType;
import ai.konduit.serving.metrics.NativeMetrics;
import ai.konduit.serving.pipeline.PipelineStep;
import ai.konduit.serving.pipeline.handlers.converter.multi.converter.impl.arrow.ArrowBinaryInputAdapter;
import ai.konduit.serving.pipeline.handlers.converter.multi.converter.impl.image.VertxBufferImageInputAdapter;
import ai.konduit.serving.pipeline.handlers.converter.multi.converter.impl.nd4j.VertxBufferNd4jInputAdapter;
import ai.konduit.serving.pipeline.handlers.converter.multi.converter.impl.numpy.VertxBufferNumpyInputAdapter;
import ai.konduit.serving.pipeline.step.ModelStep;
import ai.konduit.serving.pipeline.step.PythonStep;
import ai.konduit.serving.pipeline.step.TransformProcessStep;
import ai.konduit.serving.util.SchemaTypeUtils;
import ai.konduit.serving.verticles.VerticleConstants;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.LongTaskTimer.Sample;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.logging.LogbackMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.micrometer.backends.BackendRegistries;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.datavec.api.records.Record;
import org.datavec.api.transform.schema.Schema;
import org.nd4j.base.Preconditions;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Handles setting up a router for doing pipeline based inference.
 *
 * @author Adam Gibson
 */
@Slf4j
@Getter
public class PipelineRouteDefiner {

    protected PipelineExecutioner pipelineExecutioner;
    protected InferenceConfiguration inferenceConfiguration;
    //cached for columnar inputs, not used in binary endpoints
    protected Schema inputSchema, outputSchema = null;
    protected LongTaskTimer inferenceExecutionTimer, batchCreationTimer;
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
     * in {@link InferenceConfiguration}.
     * Note this will also initialize the {@link PipelineExecutioner}
     * property on this class. If you need access to any of the internals,
     * they are available as getters.
     * <p>
     * Metric definitions  get defined
     * relative to what was configured in the {@link InferenceConfiguration}
     * Everything implementing the {@link MeterBinder}
     * interface can be configured here.
     * Of note are a few specific ones for machine learning including:
     * {@link NativeMetrics}
     * which covers off heap memory allocation among other things.
     * <p>
     * Health checks are automatically added at /healthcheck endpoints
     *
     * @param vertx                  the input vertx instance for setting up
     *                               the returned {@link Router} instance and endpoints
     * @param inferenceConfiguration the configuration to use for the {@link PipelineExecutioner}
     * @return the router with the endpoints defined
     * @see <a href="https://vertx.io/docs/vertx-health-check/java/">Vertx health checks</a>
     */
    public Router defineRoutes(Vertx vertx, InferenceConfiguration inferenceConfiguration) {
        Router router = Router.router(vertx);

        MeterRegistry registry = BackendRegistries.getDefaultNow();
        if (registry != null) {
            log.info("Using metrics registry " + registry.getClass().getName() + " for inference");
            inferenceExecutionTimer = LongTaskTimer
                    .builder("inference")
                    .register(registry);

            batchCreationTimer = LongTaskTimer
                    .builder("batch_creation")
                    .register(registry);
        }

        if (inferenceConfiguration.getServingConfig().getMetricTypes() != null && registry != null) {
            //don't add more than one type
            for (MetricType metricType : new HashSet<>(inferenceConfiguration.getServingConfig().getMetricTypes())) {
                switch (metricType) {
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
                            log.error("Unable to setup gpu metrics. Please ensure the gpu dependency has been properly included in the classpath.", e);
                        }
                        break;
                }

            }
        }

        healthCheckHandler = HealthCheckHandler.create(vertx);

        /**
         * Get a basic health check for a running Konduit server.
         * If a server is up, this endpoint will return status of 204.
         */
        router.get("/healthcheck*").handler(healthCheckHandler);

        /**
         * Get the Konduit server configuration in raw JSON format
         */
        router.get("/config")
                .produces("application/json").handler(ctx -> {
            try {
                ctx.response().putHeader("Content-Type", "application/json");
                ctx.response().end(vertx.getOrCreateContext().config().encode());
            } catch (Exception e) {
                ctx.fail(500, e);
            }
        });

        /**
         * Get the Konduit server configuration in formatted, "pretty" JSON format
         */
        router.get("/config/pretty")
                .produces("application/json").handler(ctx -> {
            try {
                ctx.response().putHeader("Content-Type", "application/json");
                ctx.response().end(vertx.getOrCreateContext().config().encodePrettily());
            } catch (Exception e) {
                ctx.fail(500, e);
            }
        });

        /**
         * Get prometheus metrics from this endpoint.
         */
        router.get("/metrics").handler(io.vertx.micrometer.PrometheusScrapingHandler.create())
                .failureHandler(failureHandler -> {
                    if (failureHandler.failure() != null) {
                        log.error("Failed to scrape metrics", failureHandler.failure());
                    }
                });


        Preconditions.checkNotNull(inferenceConfiguration.getServingConfig(), "Please define a serving configuration.");
        router.post().handler(BodyHandler.create()
                .setUploadsDirectory(inferenceConfiguration.getServingConfig().getUploadsDirectory())
                .setDeleteUploadedFilesOnEnd(true)
                .setMergeFormAttributes(true))
                .failureHandler(failureHandlder -> {
                    if (failureHandlder.statusCode() == 404) {
                        log.warn("404 at route " + failureHandlder.request().path());
                    } else if (failureHandlder.failed()) {
                        if (failureHandlder.failure() != null) {
                            log.error("Request failed with cause ", failureHandlder.failure());
                        } else {
                            log.error("Request failed with unknown cause.");
                        }
                    }
                });



        /**
         * Get the output of a pipeline for a given prediction type for JSON input data format.
         */
        // TODO: this json specific route assumes a single input and output called "default".
        //  That seems very restrictive. Also, using this for a data format other than JSON does not make sense.
        //  Consider renaming this route to "/:predictionType/JSON" for clarity.
        router.post("/:predictionType/:inputDataFormat")
                .consumes("application/json")
                .produces("application/json").handler(ctx -> {
            PredictionType predictionType = PredictionType.valueOf(ctx.pathParam("predictionType").toUpperCase());
            initializeSchemas(inferenceConfiguration, true);

            try {
                LongTaskTimer.Sample start = null;
                if (inferenceExecutionTimer != null) {
                    start = inferenceExecutionTimer.start();
                }
                String jsonString = ctx.getBody().toString();
                pipelineExecutioner.doInference(
                        ctx,
                        predictionType,
                        jsonString,
                        inputSchema,
                        null,
                        outputSchema,
                        inferenceConfiguration.getServingConfig().getOutputDataFormat());
                if (start != null)
                    start.stop();
            } catch (Exception e) {
                log.error("Unable to perform json inference", e);
                ctx.response().setStatusCode(500);
                ctx.response().setStatusMessage("Failed to perform json inference");
                ctx.response().end();
            }
        });


        /**
         * Multi-part request for pipeline outputs of given predictionType for
         */
        // TODO: predictionType is unused, why put it into the route?
        router.post("/:predictionType/:inputDataFormat")
                .consumes("multipart/form-data")
                .consumes("multipart/mixed").handler(ctx -> {
            Map<String, InputAdapter<io.vertx.core.buffer.Buffer, ?>> adapters = getInputAdapterMap(ctx);

            BatchInputParser batchInputParser = BatchInputParser.builder()
                    .converterArgs(pipelineExecutioner.getArgs())
                    .converters(adapters)
                    .inputParts(inputNames()).build();

            ctx.vertx().executeBlocking(handler -> {

                long nanos = System.nanoTime();
                Record[] batch = null;
                try {
                    LongTaskTimer.Sample start = null;
                    if (batchCreationTimer != null) {
                        start = batchCreationTimer.start();
                    }

                    batch = batchInputParser.createBatch(ctx);
                    if (start != null)
                        start.stop();
                } catch (Exception e) {
                    log.error("Unable to convert data for batch", e);

                }

                long endNanos = System.nanoTime();
                if (inferenceConfiguration.serving().isLogTimings()) {
                    log.info("Timing for batch creation was " + TimeUnit.NANOSECONDS.toMillis((endNanos - nanos)) + " milliseconds");
                }
                if (batch == null) {
                    ctx.response().setStatusCode(400);
                    ctx.response().setStatusMessage("NDArrays failed to de serialize.");
                    handler.complete();
                } else {
                    log.debug("Created batch for request ");
                }

                ctx.put(VerticleConstants.CONVERTED_INFERENCE_DATA, batch);
                handler.complete();
            }, true, result -> ctx.next());

        });

        // TODO: predictionType is unused, why put it into the route?
        router.post("/:predictionType/:inputDataFormat")
                .consumes("multipart/form-data")
                .consumes("multipart/mixed")
                .produces("application/json").handler(ctx -> {
            String transactionUUID = ctx.get(VerticleConstants.TRANSACTION_ID);
            //need to initialize schemas for output
            initializeSchemas(inferenceConfiguration, false);

            log.debug("Processing transaction id " + transactionUUID);
            Record[] inputs = ctx.get(VerticleConstants.CONVERTED_INFERENCE_DATA);

            if (inputs == null) {
                ctx.response().setStatusCode(400);
                ctx.response().setStatusMessage("NDArrays failed to de serialize.");
                ctx.next();
                return;
            } else if (!SchemaTypeUtils.recordsAllArrayType(inputs)) {
                ctx.response().setStatusCode(400);
                ctx.response().setStatusMessage("Invalid inputs found. All types must be valid numpy or nd4j arrays.");
                ctx.next();
                return;
            }

            ctx.vertx().executeBlocking(blockingCall -> {
                try {
                    long nanos = System.nanoTime();
                    LongTaskTimer.Sample start = null;
                    if (inferenceExecutionTimer != null)
                        start = inferenceExecutionTimer.start();

                    pipelineExecutioner.doInference(
                            ctx,
                            inferenceConfiguration.getServingConfig().getPredictionType(),
                            inputs,
                            inputSchema,
                            null,
                            outputSchema,
                            inferenceConfiguration.getServingConfig().getOutputDataFormat());

                    if (start != null)
                        start.stop();
                    long endNanos = System.nanoTime();
                    if (inferenceConfiguration.serving().isLogTimings()) {
                        log.info("Timing for inference was " + TimeUnit.NANOSECONDS.toMillis((endNanos - nanos)) + " milliseconds");
                    }

                    blockingCall.complete();
                } catch (Exception e) {
                    log.error("Failed to do inference ", e);
                    ctx.fail(e);
                    blockingCall.fail(e);
                }

            }, true, result -> {
                if (result.failed()) {
                    ctx.fail(result.cause());
                }
            });

        });

        router.post("/:predictionType/:inputDataFormat")
                .consumes("multipart/form-data")
                .consumes("multipart/mixed").handler(ctx -> {
            Map<String, InputAdapter<Buffer, ?>> adapters = getInputAdapterMap(ctx);

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
                    if (batchCreationTimer != null) {
                        start = batchCreationTimer.start();
                    }

                    Record[] batch = batchInputParser.createBatch(ctx);
                    if (start != null)
                        start.stop();
                    long endNanos = System.nanoTime();
                    if (inferenceConfiguration.serving().isLogTimings()) {
                        log.info("Timing for batch creation was " + TimeUnit.NANOSECONDS.toMillis((endNanos - nanos)) + " milliseconds");
                    }
                    if (batch == null) {
                        log.warn("Created invalid null batch.");
                    }

                    ctx.put(VerticleConstants.TRANSACTION_ID, transactionUUID);
                    ctx.put(VerticleConstants.CONVERTED_INFERENCE_DATA, batch);
                    handler.complete();


                } catch (IOException e) {
                    ctx.fail(e);
                    log.error("Unable to convert inputs", e);
                    handler.fail(e);
                }
            }, true, result -> ctx.next());
        });

        router.post("/:outputDataFormat/:inputDataFormat")
                .consumes("multipart/form-data")
                .consumes("multipart/mixed")
                .produces("application/octet-stream").handler((RoutingContext ctx) -> {
            Record[] inputs = ctx.get(VerticleConstants.CONVERTED_INFERENCE_DATA);
            if (inputs == null) {
                log.warn("No inputs found. Bad request");
                ctx.response().setStatusCode(400);
                ctx.response().end("No  inputs found. Bad request.");
                return;
            }

            String outputDataFormatString = ctx.pathParam("outputDataFormat");
            Output.DataFormat dataFormat = Output.DataFormat.valueOf(outputDataFormatString.toUpperCase());
            ctx.vertx().executeBlocking(handler -> {
                try {
                    long nanos = System.nanoTime();
                    Sample start = null;
                    if (batchCreationTimer != null) {
                        start = batchCreationTimer.start();
                    }
                    pipelineExecutioner.doInference(ctx, dataFormat, inputs);
                    if (start != null)
                        start.stop();
                    long endNanos = System.nanoTime();
                    if (inferenceConfiguration.serving().isLogTimings()) {
                        log.info("Timing for inference was " + TimeUnit.NANOSECONDS.toMillis((endNanos - nanos))
                                + " milliseconds");
                    }
                    handler.complete();
                } catch (Exception e) {
                    log.error("Failed to do inference ", e);
                    ctx.fail(e);
                    handler.fail(e);
                }

            }, true, result -> {
            });

        });


        if (pipelineExecutioner == null) {
            log.debug("Initializing inference executioner after starting verticle");
            //note that we initialize this after the verticle is started
            //due to needing to sometime initialize retraining routes
            try {
                pipelineExecutioner = new PipelineExecutioner(inferenceConfiguration);
                pipelineExecutioner.init();

            } catch (Exception e) {
                log.error("Failed to initialize. Shutting down.", e);
            }

        } else {
            log.debug("Web server and endpoint already initialized.");
        }

        return router;
    }

    private void initializeSchemas(InferenceConfiguration inferenceConfiguration, boolean inputRequired) {
        if (inputSchema == null && inputRequired) {
            for (PipelineStep pipelineStep : inferenceConfiguration.getSteps()) {
                if (pipelineStep instanceof ModelStep || pipelineStep instanceof  PythonStep || pipelineStep
                        instanceof TransformProcessStep) {
                    inputSchema = pipelineStep.inputSchemaForName("default");
                }
            }
        }

        if (outputSchema == null) {
            for (PipelineStep pipelineStep : inferenceConfiguration.getSteps()) {
                if (pipelineStep instanceof ModelStep || pipelineStep instanceof  PythonStep || pipelineStep
                        instanceof TransformProcessStep) {
                    outputSchema = pipelineStep.outputSchemaForName("default");
                }
            }
        }
    }

    private Map<String, InputAdapter<Buffer, ?>> getInputAdapterMap(RoutingContext ctx) {
        Map<String, InputAdapter<Buffer, ?>> adapters = new HashMap<>();
        Input.DataFormat inputAdapterType = Input.DataFormat.valueOf(ctx.pathParam("inputDataFormat").toUpperCase());
        InputAdapter<Buffer,?> adapter = getInputAdapter(inputAdapterType);
        for(String inputName : inputNames()) {
            adapters.put(inputName,adapter);
        }
        return adapters;
    }


    /**
     * Get an {@link InputAdapter} for an input data format
     * @param inputDataFormat input data format
     * @return input adapter
     */
    private InputAdapter<Buffer,?> getInputAdapter(Input.DataFormat inputDataFormat) {
        switch(inputDataFormat) {
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
