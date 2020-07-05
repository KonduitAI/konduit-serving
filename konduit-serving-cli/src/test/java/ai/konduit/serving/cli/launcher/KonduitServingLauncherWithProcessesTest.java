/*
 *  ******************************************************************************
 *  * Copyright (c) 2020 Konduit K.K.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Apache License, Version 2.0 which is available at
 *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  * License for the specific language governing permissions and limitations
 *  * under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *  *****************************************************************************
 */

package ai.konduit.serving.cli.launcher;

import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import ai.konduit.serving.pipeline.impl.step.logging.LoggingStep;
import ai.konduit.serving.vertx.config.InferenceConfiguration;
import ai.konduit.serving.vertx.settings.constants.EnvironmentConstants;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.hamcrest.Matchers;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.Timeout;
import org.nd4j.common.io.ClassPathResource;
import org.nd4j.shade.guava.collect.Streams;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@Slf4j
@NotThreadSafe
@Ignore("This is temporary. CI keeps failing this test due to finding a konduit-serving-cli module in the .m2 repo when it's " +
        "impossible to have it there without installing it first (which shouldn't happen before running tests. Any suggestion " +
        "to fix this is welcomed. The problem occurs due to the custom build tool requiring konduit-serving-cli module as a " +
        "dependency.")
public class KonduitServingLauncherWithProcessesTest {

    private static final String TEST_SERVER_ID = "konduit_serving_test_server";
    private static final String KONDUIT_CLASSPATH = System.getProperty("konduit.test.class.path");
    private static final String SYSTEM_CLASSPATH = System.getProperty("java.class.path");
    private static final String CLASSPATH = KONDUIT_CLASSPATH == null ? SYSTEM_CLASSPATH : KONDUIT_CLASSPATH;
    private static final String STARTED_STRING = "started";
    @Rule
    public Timeout timeout = new Timeout(120, TimeUnit.SECONDS);

    @ClassRule
    public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @BeforeClass
    public static void beforeClass() {
        log.error("Using classpath: {}", CLASSPATH);
    }

    @Before
    public void before() throws IOException, InterruptedException {
        stopAllProcesses();
    }

    @Test
    public void testPredictCommandWithGrpc() throws IOException, InterruptedException {
        Data input = Data.singleton("key", "value");
        String grpcConfig = runAndGetOutput("config", "-p", "logging", "-m", "-pr", "grpc");
        runAndTailOutput(this::serverStartLogInLine, "serve", "-id", TEST_SERVER_ID, "-c",
                SystemUtils.IS_OS_WINDOWS ? grpcConfig.replace("\"", "\\\"") : grpcConfig); // Escaping \" as windows ProcessBuilder removes quotes for some reason.
        String output = runAndGetOutput("predict", TEST_SERVER_ID, "-it", "binary", "-ot", "binary", "-p", "GRPC",
                new String(input.asBytes()));
        assertEquals(input, Data.fromBytes(output.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void testMainHelpAndVersion() throws IOException, InterruptedException {
        String usageString = "Usage: konduit [COMMAND] [OPTIONS] [arg...]";
        String output = runAndGetOutput("--help"); // Testing with '--help'
        assertThat(output, Matchers.containsString(usageString));
        assertThat(output, Matchers.stringContainsInOrder(getMainCommandNames()));

        output = runAndGetOutput("-h"); // Testing with '-h'
        assertThat(output, Matchers.containsString(usageString));
        assertThat(output, Matchers.stringContainsInOrder(getMainCommandNames()));

        Properties gitProperties = new Properties();
        gitProperties.load(new ClassPathResource("META-INF/konduit-serving-cli-git.properties").getInputStream());

        String matcherString = String.format("Version: %s%nCommit hash: %s%nCommit time: %s%nBuild time: %s",
                gitProperties.getProperty("git.build.version"),
                gitProperties.getProperty("git.commit.id").substring(0, 8),
                gitProperties.getProperty("git.commit.time"),
                gitProperties.getProperty("git.build.time"));

        assertThat(runAndGetOutput("version"), Matchers.containsString(matcherString));
        assertThat(runAndGetOutput("--version"), Matchers.containsString(matcherString));
    }

    @Test
    public void testServeForegroundWorkflow() throws IOException, InterruptedException {
        // Running in foreground
        List<String> logs = runAndTailOutput(this::serverStartLogInLine, "serve", "-id", TEST_SERVER_ID, "-c", testAndGetImageConfiguration());
        assertThat(runAndGetOutput("list"), Matchers.stringContainsInOrder(Arrays.asList(TEST_SERVER_ID, STARTED_STRING)));
        assertThat(runAndGetOutput("logs", TEST_SERVER_ID, "-l", "-1").split(System.lineSeparator()).length, Matchers.lessThan(logs.size()));

        String inspectOutput = runAndGetOutput("inspect", TEST_SERVER_ID);
        InferenceConfiguration inferenceConfiguration = InferenceConfiguration.fromJson(
                inspectOutput.substring(inspectOutput.indexOf('{'))); // Finding the json string

        assertThat(runAndGetOutput("list"), Matchers.stringContainsInOrder(Arrays.asList(TEST_SERVER_ID,
                String.format("%s:%s",
                        inferenceConfiguration.host(),
                        inferenceConfiguration.port()),
                STARTED_STRING)));

        Data input = Data.singleton("key", "value");
        String json = new JsonObject(input.toJson()).encode();

        if(SystemUtils.IS_OS_WINDOWS) {
            json = json.replace("\"", "\\\""); // Escaping \" as windows ProcessBuilder removes quotes for some reason.
        }

        assertEquals(input, Data.fromJson(runAndGetOutput("predict", "-it", "json", TEST_SERVER_ID, json)));
    }

    @Test
    public void testServeBackgroundWorkflow() throws IOException, InterruptedException {
        // Running in background
        String configuration = testAndGetImageConfiguration();
        assertThat(runAndGetOutput("serve", "-id", TEST_SERVER_ID, "-c", configuration, "-b"),
                Matchers.stringContainsInOrder(Arrays.asList("For server status, execute: 'konduit list'",
                        String.format("For logs, execute: 'konduit logs %s'", TEST_SERVER_ID))));

        Thread.sleep(10000);

        List<String> logs = runAndTailOutput(this::serverStartLogInLine, "logs", TEST_SERVER_ID, "-f", "-l", "-1");
        assertThat(runAndGetOutput("logs", TEST_SERVER_ID, "-l", "-1").split(System.lineSeparator()).length,
                Matchers.lessThanOrEqualTo(logs.size()));

        String inspectOutput = runAndGetOutput("inspect", TEST_SERVER_ID);
        InferenceConfiguration inferenceConfiguration = InferenceConfiguration.fromJson(
                inspectOutput.substring(inspectOutput.indexOf('{'))); // Finding the json string

        assertThat(runAndGetOutput("list"), Matchers.stringContainsInOrder(Arrays.asList(TEST_SERVER_ID,
                String.format("%s:%s",
                        inferenceConfiguration.host(),
                        inferenceConfiguration.port()),
                STARTED_STRING)));

        Data input = Data.singleton("key", "value");
        String json = new JsonObject(input.toJson()).encode();

        if(SystemUtils.IS_OS_WINDOWS) {
            json = json.replace("\"", "\\\""); // Escaping \" as windows ProcessBuilder removes quotes for some reason.
        }

        assertEquals(input, Data.fromJson(runAndGetOutput("predict", "-it", "json", TEST_SERVER_ID, json)));
    }

    @After
    public void after() throws IOException, InterruptedException {
        stopAllProcesses();
    }

    @AfterClass
    public static void afterClass() throws IOException, InterruptedException {
        log.error("\n\nListing running servers. This should report no running servers. If there are any running servers they should be terminated manually." + "\n" +
                "----------------------------------------------------------------------------" +
                "\n\n" +
                runAndGetOutput("list") + "\n" +
                "----------------------------------------------------------------------------");
    }

    private List<String> runAndTailOutput(Predicate<String> predicate, String... command) throws IOException, InterruptedException {
        Process process = startProcessFromCommand(command);
        BufferedReader reader =  new BufferedReader(new InputStreamReader(process.getInputStream()));

        boolean end = false;
        List<String> output = new ArrayList<>();
        String line;
        while(!end) {
            line = reader.readLine();
            if(line == null) {
                Thread.sleep(100);
            } else {
                log.error(line);
                output.add(line);
                end = predicate.test(line);
            }
        }

        if(command[0].equals("logs")) {
            process.destroyForcibly();
        }

        return output;
    }

    private static String runAndGetOutput(String... command) throws IOException, InterruptedException {
        Process process = startProcessFromCommand(command);

        String output = getProcessOutput(process);
        String errorOutput = getProcessErrorOutput(process);

        assertEquals(
                "Process exited with non-zero exit code. Details: \n" +
                        output + "\n" +
                        errorOutput,
                0, process.waitFor()
        );

        log.error("Process output: {}", output);
        log.error("Process errors (ignore if none): '{}'", errorOutput);

        if("predict".equals(command[0])) {
            return output;
        } else {
            return output.trim();
        }
    }

    private static Process startProcessFromCommand(String... command) throws IOException {
        return getProcessBuilderFromCommand(command).start();
    }

    private static ProcessBuilder getProcessBuilderFromCommand(String... command) {
        List<String> fullCommand = getCommand(command);
        log.error("Running command (sub): {}", String.join(" ", command));
        ProcessBuilder processBuilder = new ProcessBuilder(fullCommand);
        Map<String, String> environment = processBuilder.environment();
        environment.put(EnvironmentConstants.WORKING_DIR, temporaryFolder.getRoot().getAbsolutePath());
        environment.put(EnvironmentConstants.FILE_UPLOADS_DIR, temporaryFolder.getRoot().getAbsolutePath());
        return processBuilder;
    }

    private static List<String> getCommand(String... command) {
        return Streams.concat(getBaseCommand().stream(), Arrays.stream(command)).collect(Collectors.toList());
    }

    private static List<String> getBaseCommand() {
        return Arrays.asList("java",
                "-cp",
                CLASSPATH,
                "-Dvertx.cli.usage.prefix=konduit",
                KonduitServingLauncher.class.getCanonicalName());
    }

    private Collection<String> getMainCommandNames() {
        KonduitServingLauncher konduitServingLauncher = new KonduitServingLauncher();
        konduitServingLauncher.setMainCommands();
        return konduitServingLauncher.getCommandNames();
    }

    private String testAndGetImageConfiguration() throws IOException, InterruptedException {
        String inferenceConfigurationJson = runAndGetOutput("config", "-p", "logging");

        assertEquals(inferenceConfigurationJson, new InferenceConfiguration()
                .pipeline(SequencePipeline.builder()
                        .add(new LoggingStep()
                                .log(LoggingStep.Log.KEYS_AND_VALUES)
                                )
                        .build())
                .toJson());

        if(SystemUtils.IS_OS_WINDOWS) {
            return new JsonObject(inferenceConfigurationJson).encode().replace("\"", "\\\""); // Escaping \" as windows ProcessBuilder removes quotes for some reason.
        } else {
            return new JsonObject(inferenceConfigurationJson).encode();
        }
    }

    private void stopAllProcesses() throws IOException, InterruptedException {
        log.error(runAndGetOutput("stop", TEST_SERVER_ID), StandardCharsets.UTF_8);
        Thread.sleep(5000);
        assertThat(runAndGetOutput("list"), Matchers.containsString("No konduit servers found."));
    }

    private boolean serverStartLogInLine(String line) {
        return Pattern.compile("server started on port").matcher(line).find();
    }

    private static String getProcessOutput(Process process) throws IOException {
        return IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
    }

    private static String getProcessErrorOutput(Process process) throws IOException {
        return IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8);
    }
}
