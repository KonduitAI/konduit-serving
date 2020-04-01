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
import ai.konduit.serving.deploy.DeployKonduitServing;
import ai.konduit.serving.train.TestUtils;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
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
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;

import static com.jayway.restassured.RestAssured.given;

@RunWith(VertxUnitRunner.class)
public class LogsEndpointTest {

    private String mBaseLogDir = null;
    private Async mAsync = null;

    public static String CONFIG_KEY = "configKey";

    @Rule
    public Timeout rule = Timeout.seconds(240);

    @ClassRule
    public static TemporaryFolder folder = new TemporaryFolder();

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @BeforeClass
    public static void beforeClass(TestContext testContext) throws Exception {
        testContext.put(CONFIG_KEY, TestUtils.getConfig(folder));
    }

    @Test
    public void testLogs(TestContext testContext) {
        mAsync = testContext.async();

        int numberOfLinesToReadFromLogs = 2;

        // Delete previous logs if they exist
        try {
            FileUtils.forceDelete(Paths.get(System.getProperty("user.dir"), "main.log").toFile());
        } catch (IOException ignore) {}

        mBaseLogDir = System.getProperty("user.dir");

        Handler<AsyncResult<InferenceConfiguration>> eventHandler = handler -> {
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
        };

        // Checking before setting environment variables
        DeployKonduitServing.deployInference(getConfig(testContext), eventHandler);
        mAsync.await();
        mAsync = testContext.async();

        // Now checking with environment variables
        environmentVariables.set("KONDUIT_SERVING_LOG_DIR", folder.getRoot().getAbsolutePath());
        mBaseLogDir = System.getenv("KONDUIT_SERVING_LOG_DIR");
        testContext.assertEquals(mBaseLogDir, folder.getRoot().getAbsolutePath());

        // Checking after setting environment variables
        DeployKonduitServing.deployInference(getConfig(testContext), eventHandler);
        mAsync.await();
    }

    @Test
    public void testWithBadLogsDirectory(TestContext testContext) {
        mAsync = testContext.async();

        Handler<AsyncResult<InferenceConfiguration>> eventHandler = handler -> {
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
        };

        // Testing with a bad system property
        environmentVariables.set("KONDUIT_SERVING_LOG_DIR", folder.getRoot().getAbsolutePath() + "/nonExistentDirectory");
        DeployKonduitServing.deployInference(getConfig(testContext), eventHandler);
        mAsync.await();
        mAsync = testContext.async();

        // Testing with a bad Environment variable
        System.setProperty("user.dir", folder.getRoot().getAbsolutePath() + "/nonExistentDirectory");
        DeployKonduitServing.deployInference(getConfig(testContext), eventHandler);
        mAsync.await();
    }

    @Test
    public void testLogsWithStdOutAndStdErr(TestContext testContext) {
        Async async = testContext.async();

        // Delete previous logs if they exist
        try {
            FileUtils.forceDelete(Paths.get(System.getProperty("user.dir"), "main.log").toFile());
        } catch (IOException ignore) {}

        DeployKonduitServing.deployInference(getConfig(testContext),
                handler -> {
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
                });
    }

    /**
     * Todo: For now this is just a utility test for trying out UI manually. Later on make a proper test which is responsible for testing out the web UI in a browser.
     * @param testContext test context for vertx unit test runner.
     */
    @Test
    @Ignore
    public void testLogsUI(TestContext testContext) {
        DeployKonduitServing.deployInference(getConfig(testContext),
                handler -> {
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
                });

        testContext.async().await();
    }

    private InferenceConfiguration getConfig(TestContext testContext) {
        return testContext.get(CONFIG_KEY);
    }
}
