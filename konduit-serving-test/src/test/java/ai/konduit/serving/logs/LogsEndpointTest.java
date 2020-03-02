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

import ai.konduit.serving.configprovider.KonduitServingMain;
import ai.konduit.serving.configprovider.KonduitServingMainArgs;
import ai.konduit.serving.train.TestUtils;
import ai.konduit.serving.verticles.inference.InferenceVerticle;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.commons.io.FileUtils;
import org.hamcrest.Matchers;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
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
        FileUtils.write(jsonConfigPath, config.encodePrettily(), Charset.defaultCharset());
    }

    @Test
    public void checkLogs(TestContext testContext) throws InterruptedException {
        mAsync = testContext.async();

        KonduitServingMainArgs args = KonduitServingMainArgs.builder()
                .configStoreType("file").ha(false)
                .multiThreaded(false)
                .verticleClassName(InferenceVerticle.class.getName())
                .configPath(testContext.get(CONFIG_FILE_PATH_KEY))
                .build();

        int numberOfLinesToReadFromLogs = 2;

        mBaseLogDir = System.getProperty("user.dir");

        // Delete previous logs if they exist
        try {
            FileUtils.forceDelete(Paths.get(mBaseLogDir, "main.log").toFile());
        } catch (IOException e) {
            e.printStackTrace();
        }

        KonduitServingMain konduitServingMain = KonduitServingMain.builder()
                .onSuccess(port -> {
                    testContext.assertTrue(Paths.get(mBaseLogDir, "main.log").toFile().exists());

                    given().port(port)
                            .get(String.format("/logs/%s", numberOfLinesToReadFromLogs))
                            .then()
                            .assertThat()
                            .statusCode(200)
                            .and()
                            .assertThat()
                            .body(Matchers.not(Matchers.isEmptyOrNullString()),
                                    new TestUtils.LinesCalculatingMatcher(numberOfLinesToReadFromLogs)); // This would mean that the log file has been found and it was read successfully

                    mAsync.complete();
                })
                .onFailure(testContext::fail)
                .build();


        // Checking before setting environment variables
        konduitServingMain.runMain(args.toArgs());
        mAsync.await();
        mAsync = testContext.async();

        // Now checking with environment variables
        environmentVariables.set("KONDUIT_SERVING_LOG_DIR", folder.getRoot().getAbsolutePath());
        testContext.assertEquals(System.getenv("KONDUIT_SERVING_LOG_DIR"), folder.getRoot().getAbsolutePath());

        mBaseLogDir = System.getenv("KONDUIT_SERVING_LOG_DIR");

        // Checking after setting environment variables
        konduitServingMain.runMain(args.toArgs());
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
        } catch (IOException e) {
            e.printStackTrace();
        }

        KonduitServingMain.builder()
                .onSuccess(port -> {
                    testContext.assertTrue(Paths.get(System.getProperty("user.dir"), "main.log").toFile().exists());

                    RequestSpecification requestSpecification = given().port(port);

                    Response response = requestSpecification.get("/logs/all");

                    response.then().assertThat()
                            .statusCode(200)
                            .and()
                            .assertThat()
                            .body(Matchers.not(Matchers.isEmptyOrNullString()));

                    int responseLines = response.body().print().split(System.lineSeparator()).length;

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
                })
                .onFailure(testContext::fail)
                .build()
                .runMain(args.toArgs());
    }
}
