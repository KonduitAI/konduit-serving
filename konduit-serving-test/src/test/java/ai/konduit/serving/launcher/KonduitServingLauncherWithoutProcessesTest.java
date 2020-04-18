/*
 * *****************************************************************************
 * Copyright (c) 2020 Konduit K.K.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ****************************************************************************
 */

package ai.konduit.serving.launcher;

import ai.konduit.serving.InferenceConfiguration;
import ai.konduit.serving.config.ServingConfig;
import ai.konduit.serving.model.*;
import ai.konduit.serving.output.types.NDArrayOutput;
import ai.konduit.serving.pipeline.step.ImageLoadingStep;
import ai.konduit.serving.pipeline.step.ModelStep;
import ai.konduit.serving.pipeline.step.PmmlStep;
import ai.konduit.serving.pipeline.step.PythonStep;
import ai.konduit.serving.settings.constants.EnvironmentConstants;
import ai.konduit.serving.util.LogUtils;
import ai.konduit.serving.util.ObjectMappers;
import ai.konduit.serving.util.PortUtils;
import ch.qos.logback.core.joran.spi.JoranException;
import com.jayway.restassured.response.Response;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.datavec.image.loader.NativeImageLoader;
import org.hamcrest.Matchers;
import org.junit.*;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.Timeout;
import org.nd4j.linalg.io.ClassPathResource;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Pattern;

import static com.jayway.restassured.RestAssured.given;
import static org.junit.Assert.*;

@Slf4j
@NotThreadSafe
public class KonduitServingLauncherWithoutProcessesTest {

    private static final String TEST_SERVER_ID = "konduit_serving_test_server";

    private static String commandOutput = "";

    private static final List<Thread> backgroundThreads = new ArrayList<>();

    @Rule
    public Timeout timeout = new Timeout(120, TimeUnit.SECONDS);

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

    @ClassRule
    public static EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @BeforeClass
    public static void beforeClass() {
        System.setProperty("vertx.cli.usage.prefix", "konduit");
        System.setOut(new LauncherPrintStream(System.out));

        environmentVariables.set(EnvironmentConstants.WORKING_DIR, temporaryFolder.getRoot().getAbsolutePath());
        environmentVariables.set(EnvironmentConstants.FILE_UPLOADS_DIR, temporaryFolder.getRoot().getAbsolutePath());
    }

    @Before
    public void before() throws InterruptedException {
        stopAllProcesses();
    }

    @Test
    public void testMainHelpAndVersion() throws IOException {
        String output = runAndGetOutput("--help"); // Testing with '--help'
        assertThat(output, Matchers.containsString("Usage: konduit [COMMAND] [OPTIONS] [arg...]"));
        assertThat(output, Matchers.stringContainsInOrder(getMainCommandNames()));

        output = runAndGetOutput("help"); // Testing with 'help'
        assertThat(output, Matchers.containsString("Usage: konduit [COMMAND] [OPTIONS] [arg...]"));
        assertThat(output, Matchers.stringContainsInOrder(getMainCommandNames()));

        output = runAndGetOutput("-h"); // Testing with '-h'
        assertThat(output, Matchers.containsString("Usage: konduit [COMMAND] [OPTIONS] [arg...]"));
        assertThat(output, Matchers.stringContainsInOrder(getMainCommandNames()));

        Properties gitProperties = new Properties();
        gitProperties.load(new ClassPathResource("META-INF/konduit-serving-core-git.properties").getInputStream());

        String matcherString = String.format("Konduit serving version: %s%nCommit hash: %s",
                gitProperties.getProperty("git.build.version"),
                gitProperties.getProperty("git.commit.id").substring(0, 8));

        assertThat(runAndGetOutput("version"), Matchers.containsString(matcherString));
        assertThat(runAndGetOutput("--version"), Matchers.containsString(matcherString));
    }

    @Test
    public void testServeForegroundWorkflow() throws IOException, InterruptedException {
        String testCommand = System.getProperty("sun.java.command");
        System.setProperty("sun.java.command", KonduitServingLauncher.class.getCanonicalName());

        // Running in foreground
        String logs = runAndTailOutput(this::serverStartLogInLine, "serve", "-id", TEST_SERVER_ID, "-c", testAndGetImageConfiguration());
        assertThat(runAndGetOutput("list"), Matchers.stringContainsInOrder(Arrays.asList(TEST_SERVER_ID, "started")));
        assertThat(runAndGetOutput("logs", TEST_SERVER_ID).split(System.lineSeparator()).length,
                Matchers.lessThan(logs.split(System.lineSeparator()).length));

        String inspectOutput = runAndGetOutput("inspect", TEST_SERVER_ID);
        InferenceConfiguration inferenceConfiguration = InferenceConfiguration.fromJson(
                inspectOutput.substring(inspectOutput.indexOf("{"))); // Finding the json string

        assertThat(runAndGetOutput("list"), Matchers.stringContainsInOrder(Arrays.asList(TEST_SERVER_ID,
                String.format("%s:%s",
                        inferenceConfiguration.getServingConfig().getListenHost(),
                        inferenceConfiguration.getServingConfig().getHttpPort()),
                "started")));

        String imagePath = new ClassPathResource("/data/5_32x32.png").getFile().getAbsolutePath();

        String predictionOutput = runAndGetOutput("predict", "-it", "IMAGE", TEST_SERVER_ID, imagePath);
        JsonObject outputJson = new JsonObject(predictionOutput.substring(predictionOutput.indexOf("{"), predictionOutput.lastIndexOf("}") + 1));

        NDArrayOutput ndArrayOutput = ObjectMappers.fromJson(outputJson.getJsonObject("default").encode(), NDArrayOutput.class);

        assertEquals(new NativeImageLoader().asMatrix(imagePath), ndArrayOutput.getNdArray());

        System.setProperty("sun.java.command", testCommand);
    }

    @Test
    public void testServeBackgroundWorkflow() throws IOException, InterruptedException {
        String testCommand = System.getProperty("sun.java.command");
        System.setProperty("sun.java.command", KonduitServingLauncher.class.getCanonicalName());

        // Running in foreground
        String configuration = testAndGetImageConfiguration();
        assertThat(runAndGetOutput("serve", "-id", TEST_SERVER_ID, "-c", configuration, "-b"),
                Matchers.stringContainsInOrder(Arrays.asList("For server status, execute:", "konduit", "list",
                        "For logs, execute:", "konduit", "logs", TEST_SERVER_ID)));

        Thread.sleep(10000);

        String logs = runAndTailOutput(this::serverStartLogInLine, "logs", TEST_SERVER_ID, "-f");
        assertThat(runAndGetOutput("logs", TEST_SERVER_ID).split(System.lineSeparator()).length,
                Matchers.lessThanOrEqualTo(logs.split(System.lineSeparator()).length));

        String inspectOutput = runAndGetOutput("inspect", TEST_SERVER_ID);
        InferenceConfiguration inferenceConfiguration = InferenceConfiguration.fromJson(
                inspectOutput.substring(inspectOutput.indexOf("{"))); // Finding the json string

        assertThat(runAndGetOutput("list"), Matchers.stringContainsInOrder(Arrays.asList(TEST_SERVER_ID,
                String.format("%s:%s",
                        inferenceConfiguration.getServingConfig().getListenHost(),
                        inferenceConfiguration.getServingConfig().getHttpPort()),
                "started")));

        String imagePath = new ClassPathResource("/data/5_32x32.png").getFile().getAbsolutePath();

        String predictionOutput = runAndGetOutput("predict", "-it", "IMAGE", TEST_SERVER_ID, imagePath);
        JsonObject outputJson = new JsonObject(predictionOutput.substring(predictionOutput.indexOf("{"), predictionOutput.lastIndexOf("}") + 1));

        NDArrayOutput ndArrayOutput = ObjectMappers.fromJson(outputJson.getJsonObject("default").encode(), NDArrayOutput.class);

        assertEquals(new NativeImageLoader().asMatrix(imagePath), ndArrayOutput.getNdArray());

        System.setProperty("sun.java.command", testCommand);
    }

    @Test
    public void testConfigs() {
        assertEquals(getConfig("image").getSteps().get(0).getClass(), ImageLoadingStep.class);
        assertEquals(getConfig("python").getSteps().get(0).getClass(), PythonStep.class);
        assertEquals(((ModelStep) getConfig("tensorflow").getSteps().get(0)).getModelConfig().getClass(), TensorFlowConfig.class);
        assertEquals(((ModelStep) getConfig("onnx").getSteps().get(0)).getModelConfig().getClass(), OnnxConfig.class);

        ModelStep modelStep = (ModelStep) getConfig("pmml").getSteps().get(0);
        assertEquals(modelStep.getClass(), PmmlStep.class);
        assertEquals(modelStep.getModelConfig().getClass(), PmmlConfig.class);

        assertEquals(((ModelStep) getConfig("dl4j").getSteps().get(0)).getModelConfig().getClass(), DL4JConfig.class);
        assertEquals(((ModelStep) getConfig("keras").getSteps().get(0)).getModelConfig().getClass(), KerasConfig.class);
    }

    @Test
    public void testRunCommand() throws InterruptedException, IOException, JoranException {
        int port = PortUtils.getAvailablePort();

        Thread runCommandThread = new Thread(() -> runCommand("run", "-c",
                new JsonObject(InferenceConfiguration.builder()
                        .servingConfig(ServingConfig.builder()
                                .httpPort(port)
                                .uploadsDirectory(temporaryFolder.getRoot().getAbsolutePath())
                                .build())
                        .step(ImageLoadingStep.builder()
                                .inputName("default")
                                .outputName("default")
                                .build())
                        .build().toJson()).encode(),
                "-i", "1",
                "-s", "inference"));
        runCommandThread.start();
        backgroundThreads.add(runCommandThread);

        boolean isServerStarted = false;
        while(!runCommandThread.isInterrupted()) {
            Thread.sleep(2000);

            try {
                Response response = given().port(port).get("/config").andReturn();
                isServerStarted = response.statusCode() == 200;
            } catch (Exception exception) {
                log.info("Unable to connect to the server. Trying again...");
            }

            if(isServerStarted) {
                break;
            }
        }

        assertTrue(isServerStarted);

        LogUtils.setLoggingFromClassPath();
    }

    @After
    public void after() throws InterruptedException {
        stopAllProcesses();
    }

    @AfterClass
    public static void afterClass() throws IOException, JoranException {
        log.info("\n\nListing running servers. This should report no running servers. If there are any running servers they should be terminated manually." + "\n" +
                "----------------------------------------------------------------------------" +
                "\n\n" +
                runAndGetOutput("list") + "\n" +
                "----------------------------------------------------------------------------");

        LogUtils.setLoggingFromClassPath(); // Resetting logging properties if it was modified by the KonduitServingLauncher commands.
    }

    private String runAndTailOutput(Function<String, Boolean> predicate, String... command) throws InterruptedException {
        Thread backgroundThread = new Thread(() -> runCommand(command));
        backgroundThread.start();
        backgroundThreads.add(backgroundThread);

        boolean end = false;
        String allOutput = "";
        while(!end) {
            allOutput = commandOutput;
            if(allOutput == null) {
                Thread.sleep(100);
            } else {
                end = predicate.apply(allOutput);
            }
        }

        commandOutput = "";

        return allOutput;
    }

    private static void runCommand(String... command) {
        log.info("Running command: {}", String.join(" ", command));
        KonduitServingLauncher.main(command);
    }

    private static String runAndGetOutput(String... command) {
        runCommand(command);
        return getOutput().trim();
    }

    private void stopAllProcesses() throws InterruptedException {
        for(Thread backgroundThread : backgroundThreads) {
            backgroundThread.interrupt();
        }

        runAndGetOutput("stop", TEST_SERVER_ID);
        Thread.sleep(5000);
        assertThat(runAndGetOutput("list"), Matchers.containsString("No konduit servers found."));
    }

    private static String getOutput() {
        String returnOutput = commandOutput;
        commandOutput = "";
        return returnOutput;
    }

    private Collection<String> getMainCommandNames() {
        return new KonduitServingLauncher().setMainCommands().getCommandNames();
    }

    private String testAndGetImageConfiguration() {
        String inferenceConfigurationJson = runAndGetOutput("config", "-t", "image");

        assertEquals(inferenceConfigurationJson, InferenceConfiguration.builder()
                .servingConfig(ServingConfig.builder()
                        .uploadsDirectory(temporaryFolder.getRoot().getAbsolutePath())
                        .build())
                .step(ImageLoadingStep.builder()
                        .inputName("default")
                        .outputName("default")
                        .build())
                .build().toJson());

        return new JsonObject(inferenceConfigurationJson).encode();
    }

    private boolean serverStartLogInLine(String line) {
        return Pattern.compile("Succeeded in deploying verticle").matcher(line).find();
    }

    private InferenceConfiguration getConfig(String type) {
        String configOutput = runAndGetOutput("config", "-t", type);
        return InferenceConfiguration.fromJson(configOutput.substring(configOutput.indexOf("{")));
    }

    private static class LauncherPrintStream extends PrintStream {

        public LauncherPrintStream(PrintStream ps) {
            super(ps);
        }

        @Override
        public void print(String s) {
            commandOutput += s + System.lineSeparator();
            super.print(s);
        }
    }
}
