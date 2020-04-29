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

package ai.konduit.serving.launcher;

import ai.konduit.serving.InferenceConfiguration;
import ai.konduit.serving.config.ServingConfig;
import ai.konduit.serving.output.types.NDArrayOutput;
import ai.konduit.serving.pipeline.step.ImageLoadingStep;
import ai.konduit.serving.settings.constants.EnvironmentConstants;
import ai.konduit.serving.util.LogUtils;
import ai.konduit.serving.util.ObjectMappers;
import ch.qos.logback.core.joran.spi.JoranException;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.datavec.image.loader.NativeImageLoader;
import org.hamcrest.Matchers;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.Timeout;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.io.ClassPathResource;
import org.nd4j.shade.guava.collect.Streams;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@Slf4j
@NotThreadSafe
public class KonduitServingLauncherWithProcessesTest {

    private static final String TEST_SERVER_ID = "konduit_serving_test_server";
    private static final String konduit_classpath = System.getProperty("konduit.test.class.path");
    private static final String system_classpath = System.getProperty("java.class.path");
    private static final String classpath = konduit_classpath == null ? system_classpath : konduit_classpath;

    @Rule
    public Timeout timeout = new Timeout(120, TimeUnit.SECONDS);

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

    @BeforeClass
    public static void beforeClass() {
        log.info("Using classpath: {}", classpath);
    }

    @Before
    public void before() throws IOException, InterruptedException {
        stopAllProcesses();
    }

    @Test
    public void testMainHelpAndVersion() throws IOException, InterruptedException {
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
        // Running in foreground
        List<String> logs = runAndTailOutput(this::serverStartLogInLine, "serve", "-id", TEST_SERVER_ID, "-c", testAndGetImageConfiguration());
        assertThat(runAndGetOutput("list"), Matchers.stringContainsInOrder(Arrays.asList(TEST_SERVER_ID, "started")));
        assertThat(runAndGetOutput("logs", TEST_SERVER_ID).split(System.lineSeparator()).length, Matchers.lessThan(logs.size()));

        String inspectOutput = runAndGetOutput("inspect", TEST_SERVER_ID);
        InferenceConfiguration inferenceConfiguration = InferenceConfiguration.fromJson(
                inspectOutput.substring(inspectOutput.indexOf("{"))); // Finding the json string

        assertThat(runAndGetOutput("list"), Matchers.stringContainsInOrder(Arrays.asList(TEST_SERVER_ID,
                String.format("%s:%s",
                        inferenceConfiguration.getServingConfig().getListenHost(),
                        inferenceConfiguration.getServingConfig().getHttpPort()),
                "started")));

        String imagePath = new ClassPathResource("/data/5_32x32.png").getFile().getAbsolutePath();

        JsonObject outputJson = new JsonObject(runAndGetOutput("predict", "-it", "IMAGE", TEST_SERVER_ID, imagePath));
        NDArrayOutput ndArrayOutput = ObjectMappers.fromJson(outputJson.getJsonObject("default").encode(), NDArrayOutput.class);

        assertEquals(new NativeImageLoader().asMatrix(imagePath), ndArrayOutput.getNdArray());
    }

    @Test
    public void testServeBackgroundWorkflow() throws IOException, InterruptedException {
        Nd4j.create(10, 10);

        // Running in foreground
        String configuration = testAndGetImageConfiguration();
        assertThat(runAndGetOutput("serve", "-id", TEST_SERVER_ID, "-c", configuration, "-b"),
                Matchers.stringContainsInOrder(Arrays.asList("For server status, execute: 'konduit list'",
                        String.format("For logs, execute: 'konduit logs %s'", TEST_SERVER_ID))));

        Thread.sleep(10000);

        List<String> logs = runAndTailOutput(this::serverStartLogInLine, "logs", TEST_SERVER_ID, "-f");
        assertThat(runAndGetOutput("logs", TEST_SERVER_ID).split(System.lineSeparator()).length,
                Matchers.lessThanOrEqualTo(logs.size()));

        String inspectOutput = runAndGetOutput("inspect", TEST_SERVER_ID);
        InferenceConfiguration inferenceConfiguration = InferenceConfiguration.fromJson(
                inspectOutput.substring(inspectOutput.indexOf("{"))); // Finding the json string

        assertThat(runAndGetOutput("list"), Matchers.stringContainsInOrder(Arrays.asList(TEST_SERVER_ID,
                String.format("%s:%s",
                        inferenceConfiguration.getServingConfig().getListenHost(),
                        inferenceConfiguration.getServingConfig().getHttpPort()),
                "started")));

        String imagePath = new ClassPathResource("/data/5_32x32.png").getFile().getAbsolutePath();

        JsonObject outputJson = new JsonObject(runAndGetOutput("predict", "-it", "IMAGE", TEST_SERVER_ID, imagePath));
        NDArrayOutput ndArrayOutput = ObjectMappers.fromJson(outputJson.getJsonObject("default").encode(), NDArrayOutput.class);

        assertEquals(new NativeImageLoader().asMatrix(imagePath), ndArrayOutput.getNdArray());
    }

    @After
    public void after() throws IOException, InterruptedException {
        stopAllProcesses();
    }

    @AfterClass
    public static void afterClass() throws IOException, InterruptedException, JoranException {
        log.info("\n\nListing running servers. This should report no running servers. If there are any running servers they should be terminated manually." + "\n" +
                "----------------------------------------------------------------------------" +
                "\n\n" +
                runAndGetOutput("list") + "\n" +
                "----------------------------------------------------------------------------");

        LogUtils.setLoggingFromClassPath(); // Resetting logging properties if it was modified by the KonduitServingLauncher commands.
    }

    private List<String> runAndTailOutput(Function<String, Boolean> predicate, String... command) throws IOException, InterruptedException {
        BufferedReader reader =  new BufferedReader(new InputStreamReader(runAndGetInputStream(command)));

        boolean end = false;
        List<String> output = new ArrayList<>();
        String line;
        while(!end) {
            line = reader.readLine();
            if(line == null) {
                Thread.sleep(100);
            } else {
                log.info(line);
                output.add(line);
                end = predicate.apply(line);
            }
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

        log.info("Process output: {}", output);
        log.warn("Process errors (ignore if none): '{}'", errorOutput);

        return output.trim();
    }

    private static InputStream runAndGetInputStream(String... command) throws IOException {
        return startProcessFromCommand(command).getInputStream();
    }

    private static Process startProcessFromCommand(String... command) throws IOException {
        return getProcessBuilderFromCommand(command).start();
    }

    private static ProcessBuilder getProcessBuilderFromCommand(String... command) {
        List<String> fullCommand = getCommand(command);
        log.info("Running command (sub): {}", String.join(" ", command));
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
                classpath,
                "-Dvertx.cli.usage.prefix=konduit",
                KonduitServingLauncher.class.getCanonicalName());
    }

    private Collection<String> getMainCommandNames() {
        return new KonduitServingLauncher().setMainCommands().getCommandNames();
    }

    private String testAndGetImageConfiguration() throws IOException, InterruptedException {
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

        if(SystemUtils.IS_OS_WINDOWS) {
            return new JsonObject(inferenceConfigurationJson).encode().replace("\"", "\\\""); // Escaping \" as windows ProcessBuilder removes quotes for some reason.
        } else {
            return new JsonObject(inferenceConfigurationJson).encode();
        }
    }

    private void stopAllProcesses() throws IOException, InterruptedException {
        log.info(runAndGetOutput("stop", TEST_SERVER_ID), StandardCharsets.UTF_8);
        Thread.sleep(5000);
        assertThat(runAndGetOutput("list"), Matchers.containsString("No konduit servers found."));
    }

    private boolean serverStartLogInLine(String line) {
        return Pattern.compile("Succeeded in deploying verticle").matcher(line).find();
    }

    private static String getProcessOutput(Process process) throws IOException {
        return IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
    }

    private static String getProcessErrorOutput(Process process) throws IOException {
        return IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8);
    }
}