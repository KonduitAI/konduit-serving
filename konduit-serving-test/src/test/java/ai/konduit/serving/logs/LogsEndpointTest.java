/* ******************************************************************************
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
 ******************************************************************************/

package ai.konduit.serving.logs;

import ai.konduit.serving.InferenceConfiguration;
import ai.konduit.serving.configprovider.KonduitServingMain;
import ai.konduit.serving.configprovider.KonduitServingMainArgs;
import ai.konduit.serving.configprovider.KonduitServingNodeConfigurer;
import ai.konduit.serving.train.TestUtils;
import ai.konduit.serving.verticles.inference.InferenceVerticle;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Strings;
import org.hamcrest.Matchers;
import org.junit.*;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import static com.jayway.restassured.RestAssured.given;

@RunWith(VertxUnitRunner.class)
public class LogsEndpointTest {

    private String mBaseLogDir = null;
    private Async mAsync = null;

    public static String CONFIG_FILE_PATH_KEY = "configFilePathKey";

    @Rule
    public Timeout rule = Timeout.seconds(240);

    @ClassRule
    public static TemporaryFolder folder = new TemporaryFolder();

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @BeforeClass
    public static void beforeClass(TestContext testContext) throws Exception {
        File jsonConfigPath = folder.newFile("config.json");
        testContext.put(CONFIG_FILE_PATH_KEY, jsonConfigPath.getAbsolutePath());

        JsonObject config = TestUtils.getConfig(folder);
        FileUtils.write(jsonConfigPath, config.encodePrettily(), StandardCharsets.UTF_8);
    }

    @Test
    public void testLogs(TestContext testContext) {
        mAsync = testContext.async();

        KonduitServingMainArgs args = KonduitServingMainArgs.builder()
                .configStoreType("file").ha(false)
                .multiThreaded(false)
                .verticleClassName(InferenceVerticle.class.getName())
                .configPath(testContext.get(CONFIG_FILE_PATH_KEY))
                .build();

        int numberOfLinesToReadFromLogs = 2;

        // Delete previous logs if they exist
        try {
            FileUtils.forceDelete(Paths.get(System.getProperty("user.dir"), "main.log").toFile());
        } catch (IOException ignore) {}

        mBaseLogDir = System.getProperty("user.dir");

        KonduitServingMain konduitServingMain = KonduitServingMain.builder()
                .eventHandler(handler -> {
                    if(handler.succeeded()) {
                        testContext.assertTrue(Paths.get(mBaseLogDir, "main.log").toFile().exists());

                        given().port(handler.result().getServingConfig().getHttpPort())
                                .get(String.format("/logs/%s", numberOfLinesToReadFromLogs))
                                .then()
                                .assertThat()
                                .statusCode(200)
                                .and()
                                .assertThat()
                                .body(Matchers.not(Matchers.isEmptyOrNullString()),
                                        new TestUtils.LinesCalculatingMatcher(numberOfLinesToReadFromLogs)); // This would mean that the log file has been found and it was read successfully

                        mAsync.complete();
                    } else {
                        testContext.fail(handler.cause());
                    }
                })
                .build();


        // Checking before setting environment variables
        konduitServingMain.runMain(args.toArgs());
        mAsync.await();
        mAsync = testContext.async();

        // Now checking with environment variables
        environmentVariables.set("KONDUIT_SERVING_LOG_DIR", folder.getRoot().getAbsolutePath());
        mBaseLogDir = System.getenv("KONDUIT_SERVING_LOG_DIR");
        testContext.assertEquals(mBaseLogDir, folder.getRoot().getAbsolutePath());

        // Checking after setting environment variables
        konduitServingMain.runMain(args.toArgs());
        mAsync.await();
    }

    @Test
    public void testWithBadLogsDirectory(TestContext testContext) {
        mAsync = testContext.async();

        KonduitServingNodeConfigurer konduitServingNodeConfigurer = KonduitServingNodeConfigurer.builder()
                .createPidFile(false)
                .configStoreType("file").ha(false)
                .verticleClassName(InferenceVerticle.class.getName())
                .configPath(testContext.get(CONFIG_FILE_PATH_KEY))
                .vertxOptions(new VertxOptions())
                .build();

        KonduitServingMain konduitServingMain = KonduitServingMain.builder()
                .eventHandler(handler -> {
                    if(handler.succeeded()) {
                        testContext.fail("Logs file cannot be placed at an invalid directory.");
                    } else {
                        Throwable throwable = handler.cause();
                        if(!Strings.isNullOrEmpty(System.getenv("KONDUIT_SERVING_LOG_DIR")) &&
                                throwable.getMessage().contains("environment variable KONDUIT_SERVING_LOG_DIR") &&
                                throwable.getMessage().contains("doesn't exist or is an invalid directory.")) {
                            mAsync.countDown();
                        } else if(throwable.getMessage().contains("system property user.dir") &&
                                throwable.getMessage().contains("doesn't exist or is an invalid directory.")) {
                            mAsync.countDown();
                        } else {
                            testContext.fail(handler.cause());
                        }
                    }
                })
                .build();

        // Testing with a bad system property
        environmentVariables.set("KONDUIT_SERVING_LOG_DIR", folder.getRoot().getAbsolutePath() + "/nonExistentDirectory");
        konduitServingMain.runMain(konduitServingNodeConfigurer);
        mAsync.await();
        mAsync = testContext.async();

        // Testing with a bad Environment variable
        System.setProperty("user.dir", folder.getRoot().getAbsolutePath() + "/nonExistentDirectory");
        konduitServingMain.runMain(konduitServingNodeConfigurer);
        mAsync.await();
    }

    @Test
    public void testLogsWithStdOutAndStdErr(TestContext testContext) {
        Async async = testContext.async();

        KonduitServingMainArgs args = KonduitServingMainArgs.builder()
                .configStoreType("file").ha(false)
                .multiThreaded(false)
                .verticleClassName(InferenceVerticle.class.getName())
                .configPath(testContext.get(CONFIG_FILE_PATH_KEY))
                .build();

        // Delete previous logs if they exist
        try {
            FileUtils.forceDelete(Paths.get(System.getProperty("user.dir"), "main.log").toFile());
        } catch (IOException ignore) {}

        KonduitServingMain.builder()
                .eventHandler(handler -> {
                    if(handler.succeeded()) {
                        testContext.assertTrue(Paths.get(System.getProperty("user.dir"), "main.log").toFile().exists());

                        RequestSpecification requestSpecification = given().port(handler.result().getServingConfig().getHttpPort());

                        Response response = requestSpecification.get("/logs/all");

                        response.then().assertThat()
                                .statusCode(200)
                                .and()
                                .assertThat()
                                .body(Matchers.not(Matchers.isEmptyOrNullString()));

                        int timesPrinted = 10;

                        for (int i = 0; i < timesPrinted; i++) {
                            System.out.println("Stdout check: " + i);
                            System.err.println("Stderr check: " + i);
                        }

                        Response responseWithStdOutAndStdErr = requestSpecification.get("/logs/all");
                        responseWithStdOutAndStdErr.then().assertThat()
                                .statusCode(200)
                                .and()
                                .body(Matchers.not(Matchers.isEmptyOrNullString()),
                                        new TestUtils.ContainsNumberOfInstancesMatcher(timesPrinted, "Stdout check"),
                                        new TestUtils.ContainsNumberOfInstancesMatcher(timesPrinted, "Stderr check"));

                        async.complete();
                    } else {
                        testContext.fail(handler.cause());
                    }
                })
                .build()
                .runMain(args.toArgs());
    }

    /**
     * Todo: For now this is just a utility test for trying out UI manually. Later on make a proper test which is responsible for testing out the web UI in a browser.
     * @param testContext test context for vertx unit test runner.
     */
    @Test
    @Ignore
    public void testLogsUI(TestContext testContext) {
        KonduitServingMain.builder()
                .eventHandler(handler -> {
                    if(handler.succeeded()) {
                        try {
                            InferenceConfiguration inferenceConfiguration = handler.result();
                            String url = String.format("http://%s:%s/logs",
                                    inferenceConfiguration.getServingConfig().getListenHost(),
                                    inferenceConfiguration.getServingConfig().getHttpPort());
                            Desktop.getDesktop().browse(new URL(url).toURI());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        testContext.fail(handler.cause());
                    }
                }).build()
                .runMain(KonduitServingMainArgs.builder()
                        .configStoreType("file").ha(false)
                        .multiThreaded(false)
                        .verticleClassName(InferenceVerticle.class.getName())
                        .configPath(testContext.get(CONFIG_FILE_PATH_KEY)).build().toArgs());

        testContext.async().await();
    }
}
