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

package ai.konduit.serving.verticles.ndarray;

import ai.konduit.serving.InferenceConfiguration;
import ai.konduit.serving.config.ServingConfig;
import ai.konduit.serving.pipeline.step.ModelStep;
import ai.konduit.serving.pipeline.step.TransformProcessStep;
import ai.konduit.serving.pipeline.step.model.Dl4jStep;
import ai.konduit.serving.train.TrainUtils;
import ai.konduit.serving.util.PortUtils;
import ai.konduit.serving.verticles.inference.InferenceVerticle;
import com.jayway.restassured.http.ContentType;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.datavec.api.transform.TransformProcess;
import org.datavec.api.transform.schema.Schema;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nd4j.common.primitives.Pair;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.factory.Nd4j;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.File;

import static ai.konduit.serving.train.TrainUtils.getIrisOutputSchema;
import static ai.konduit.serving.train.TrainUtils.getTrainedNetwork;
import static com.jayway.restassured.RestAssured.given;

/**
 * Example of an asynchronous JUnit test for a Verticle.
 */
@RunWith(VertxUnitRunner.class)
@NotThreadSafe
public class ColumnarTransformProcessesTest extends BaseDl4JVerticalTest {

    private Schema inputSchema;

    @Before
    public void before(TestContext context) throws Exception {
        port = PortUtils.getAvailablePort();

        DeploymentOptions options = new DeploymentOptions()
                .setWorker(true)
                .setWorkerPoolSize(1)
                .setInstances(1)
                .setConfig(getConfigObject());

        vertx = Vertx.vertx();
        vertx.deployVerticle(InferenceVerticle.class.getName(), options, context.asyncAssertSuccess());
    }

    @After
    public void after(TestContext context) {
        if (vertx != null) {
            vertx.close(context.asyncAssertSuccess());
        }
    }

    @Override
    public JsonObject getConfigObject() throws Exception {
        Pair<MultiLayerNetwork, DataNormalization> multiLayerNetwork = getTrainedNetwork();
        File modelSave = new File(temporary.getRoot(), "model.zip");
        ModelSerializer.writeModel(multiLayerNetwork.getFirst(), modelSave, true);

        inputSchema = TrainUtils.getIrisInputSchema();
        Schema outputSchema = getIrisOutputSchema();
        Nd4j.getRandom().setSeed(42);

        TransformProcess.Builder transformProcessBuilder = new TransformProcess.Builder(inputSchema);
        for (int i = 0; i < inputSchema.numColumns(); i++) {
            transformProcessBuilder.convertToDouble(inputSchema.getName(i));
        }

        TransformProcess transformProcess = transformProcessBuilder.build();

        TransformProcessStep transformStep = new TransformProcessStep(transformProcess, outputSchema);

        ServingConfig servingConfig = ServingConfig.builder()
                .httpPort(port)
                .build();

        ModelStep modelStepConfig = Dl4jStep.builder().path(modelSave.getAbsolutePath()).build()
                .setInput(inputSchema)
                .setOutput(outputSchema);

        InferenceConfiguration inferenceConfiguration = InferenceConfiguration.builder()
                .servingConfig(servingConfig)
                .step(transformStep)
                .step(modelStepConfig)
                .build();

        System.out.println(inferenceConfiguration.toJson());
        return new JsonObject(inferenceConfiguration.toJson());
    }

    @Test(timeout = 60000)
    public void testInferenceResult(TestContext context) {
        JsonArray jsonArray = new JsonArray();
        double[] vals = {5.1, 3.5, 1.4, 0.2};
        for (int i = 0; i < 4; i++) jsonArray.add(vals[i]);

        JsonObject wrapper = new JsonObject();
        for (int i = 0; i < vals.length; i++) {
            wrapper.put(inputSchema.getName(i), vals[i]);
        }

        given().contentType(ContentType.JSON)
                .body(wrapper.toString())
                .port(port)
                .post("/classification/json")
                .then().statusCode(200);
    }
}