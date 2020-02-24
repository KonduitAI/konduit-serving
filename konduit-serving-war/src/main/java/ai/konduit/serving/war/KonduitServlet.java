package ai.konduit.serving.war;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.VerticleFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.CountDownLatch;

/**
 * Servlet for running VertX and pipeline servers.
 *
 * @author Adam Gibson
 */
@Slf4j
public class KonduitServlet extends HttpServlet {

    public final static String CLASS_NAME = "ai.konduit.serving.class";
    public final static String CONFIG_JSON = "ai.konduit.serving.configpath";
    public final static int DEFAULT_HTTP_PORT = 8081;
    private Vertx vertx;
    private HttpClient httpClient;
    private JsonObject vertxConfig;

    public KonduitServlet() {
        super();
    }


    @Override
    public void init() throws ServletException {
        super.init();
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        vertx = Vertx.vertx();
        httpClient = vertx.createHttpClient();

        String configStorePath = System.getProperty(CONFIG_JSON);
        JsonObject config1 = new JsonObject();
        config1.put("path", configStorePath);
        ConfigStoreOptions httpStore = new ConfigStoreOptions()
                .setType("file")
                .setOptional(true)
                .setConfig(config1);

        CountDownLatch countDownLatch = new CountDownLatch(2);
        ConfigRetrieverOptions options = new ConfigRetrieverOptions()
                .addStore(httpStore);
        ConfigRetriever retriever = ConfigRetriever.create(vertx, options);
        String verticleClassName = System.getProperty(CLASS_NAME);
        retriever.getConfig(ar -> {
            if (ar.failed() || !new File(configStorePath).exists()) {
                log("Unable to find configuration. Continuing without.");
                log.debug("Unable to find configuration. Continuing without.");
                vertxConfig = new JsonObject().put("httpPort", DEFAULT_HTTP_PORT);
            } else {
                JsonObject config2 = ar.result();
                vertxConfig = config2;

            }

            log.debug("Attempting to deploy verticle " + verticleClassName);
            log("Attempting to deploy verticle " + verticleClassName);
            DeploymentOptions deploymentOptions = new DeploymentOptions()
                    .setConfig(vertxConfig).setWorker(true)
                    .setHa(false).setInstances(1)
                    .setMultiThreaded(false)
                    .setWorkerPoolSize(1);
            String[] split = verticleClassName.split("\\.");
            vertx.registerVerticleFactory(new VerticleFactory() {
                @Override
                public String prefix() {
                    return split[split.length - 1];
                }

                @Override
                public Verticle createVerticle(String s, ClassLoader classLoader) throws Exception {
                    Object verticle = classLoader.loadClass(verticleClassName).newInstance();
                    Verticle syntaxNetVerticle = (Verticle) verticle;
                    countDownLatch.countDown();
                    return syntaxNetVerticle;
                }
            });


            vertx.deployVerticle(verticleClassName, deploymentOptions, handler -> {
                if (handler.failed()) {
                    log.error("Unable to deploy verticle", handler.cause());
                    log("Unable to deploy verticle", handler.cause());
                } else {
                    log.debug("Deployed verticle");
                    log("Deployed verticle");
                    countDownLatch.countDown();

                }

            });

        });

        log("Initializing server");
        log.debug("Initializing server");
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            log("Interrupting await call for servlet start", e.getCause());
            Thread.currentThread().interrupt();
        }

        log("Initialized server");
        log.debug("Initialized server");

    }


    @Override
    public void destroy() {
        super.destroy();
        if (httpClient != null) {
            httpClient.close();
        }
        if (vertx != null) {
            vertx.close(result -> {
                if (result.failed()) {
                    log("Failed to close down server", result.cause());
                } else {
                    log("Shut down server");

                }
            });
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("org.apache.catalina.ASYNC_SUPPORTED", true);
        AsyncContext aCtx = req.startAsync(req, resp);
        addLoggingListenerToCtx(aCtx);

        ServletInputStream inputStream = req.getInputStream();
        byte[] streamContent = IOUtils.toByteArray(inputStream);
        inputStream.close();

        String reqUrl = req.getRequestURL().toString();
        String baseUrl = reqUrl.replace(req.getContextPath() + "//", req.getContextPath() + "/");
        String url = baseUrl.replace(req.getContextPath(), "")
                .replace(String.valueOf(req.getLocalPort()),
                        String.valueOf(vertxConfig.getInteger("httpPort")));


        HttpClientRequest request = httpClient.request(HttpMethod.GET, url, httpClientResponse -> {
            httpClientResponse.exceptionHandler(exception -> {
                log("Error occurred", exception.getCause());
            });

            httpClientResponse.bodyHandler(body -> {
                try {
                    final PrintWriter writer = aCtx.getResponse().getWriter();
                    writer.write(body.toString());
                    writer.flush();
                    writer.close();
                    aCtx.complete();
                } catch (IOException e) {
                    log("Error occurred", e);
                }
            });
        });


        request.putHeader("Content-Type", req.getHeader("Content-Type"));
        request.setChunked(false);
        request.end(Buffer.buffer(streamContent));


    }


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("org.apache.catalina.ASYNC_SUPPORTED", true);
        AsyncContext aCtx = req.startAsync(req, resp);
        addLoggingListenerToCtx(aCtx);
        ServletInputStream inputStream = req.getInputStream();
        byte[] streamContent = IOUtils.toByteArray(inputStream);
        inputStream.close();

        if (streamContent != null && streamContent.length > 0) {
            //proxy through to vertx backend
            String reqUrl = req.getRequestURL().toString();
            String baseUrl = reqUrl.replace(req.getContextPath() + "//", req.getContextPath() + "/");
            String url = baseUrl.replace(req.getContextPath(), "")
                    .replace(String.valueOf(req.getLocalPort()),
                            String.valueOf(vertxConfig.getInteger("httpPort")));

            HttpClientRequest request = httpClient.requestAbs(HttpMethod.POST, url, httpClientResponse -> {
                httpClientResponse.exceptionHandler(exception -> {
                    log("Error occurred", exception.getCause());
                });

                httpClientResponse.bodyHandler(body -> {
                    try {
                        final PrintWriter writer = aCtx.getResponse().getWriter();
                        aCtx.getResponse().setContentType(httpClientResponse.getHeader("Content-Type"));
                        writer.write(body.toString());
                        writer.flush();
                        writer.close();
                        aCtx.complete();
                    } catch (IOException e) {
                        log("Error occurred", e);
                    }
                });
            });

            request.setChunked(false);
            request.putHeader("Content-Type", req.getHeader("Content-Type"));
            request.putHeader("Content-Length", String.valueOf(streamContent.length));
            request.end(Buffer.buffer(streamContent));
        } else {
            try {
                final PrintWriter writer = aCtx.getResponse().getWriter();
                aCtx.getResponse().setContentType("application/json");
                writer.write(new JsonObject().put("status", "empty body").toString());
                writer.flush();
                writer.close();
                aCtx.complete();
            } catch (IOException e) {
                log("Error occurred", e);
            }
        }

    }


    private void addLoggingListenerToCtx(AsyncContext aCtx) {
        aCtx.addListener(new AsyncListener() {
            @Override
            public void onComplete(AsyncEvent event) throws IOException {
                log("Completed request");
            }

            @Override
            public void onTimeout(AsyncEvent event) throws IOException {
                log("Timeout on request");

            }

            @Override
            public void onError(AsyncEvent event) throws IOException {
                log("Error request", event.getThrowable());

            }

            @Override
            public void onStartAsync(AsyncEvent event) throws IOException {
                log("Started request");
            }
        });

    }
}
