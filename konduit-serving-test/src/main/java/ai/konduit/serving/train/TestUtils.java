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

package ai.konduit.serving.train;

import ai.konduit.serving.InferenceConfiguration;
import ai.konduit.serving.config.ServingConfig;
import ai.konduit.serving.pipeline.step.ModelStep;
import ai.konduit.serving.pipeline.step.model.Dl4jStep;
import ai.konduit.serving.util.SchemaTypeUtils;
import lombok.AllArgsConstructor;
import org.datavec.api.transform.schema.Schema;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.rules.TemporaryFolder;
import org.nd4j.common.primitives.Pair;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestUtils {

    @AllArgsConstructor
    public static class LinesCalculatingMatcher extends BaseMatcher<String> {

        private int numberOfLinesToReadFromLogs;

        @Override
        public boolean matches(Object logs) {
            return ((String) logs).split(System.lineSeparator()).length == numberOfLinesToReadFromLogs;
        }

        @Override
        public void describeTo(Description description) { }

        @Override
        public void describeMismatch(Object logs, Description description) {
            description.appendText("Expected number of lines were ")
                    .appendValue(numberOfLinesToReadFromLogs)
                    .appendText("was ")
                    .appendValue(((String) logs).split(System.lineSeparator()).length)
                    .appendText("logs were ")
                    .appendValue(logs);
        }
    }

    @AllArgsConstructor
    public static class ContainsNumberOfInstancesMatcher extends BaseMatcher<String> {

        private int numberOfInstances;
        private String regex;

        /**
         * Finds number of instances found in a string
         * @return the number of occurred instances
         */
        private int findNumberOfMatches(String input) {
            // Use Matcher class of java.util.regex
            // to match the character
            Matcher matcher
                    = Pattern.compile(regex).matcher(input);

            int res = 0;

            // for every presence of character ch
            // increment the counter res by 1
            while (matcher.find()) {
                res++;
            }

            return res;
        }

        @Override
        public boolean matches(Object logs) {
            return findNumberOfMatches((String) logs) == numberOfInstances;
        }

        @Override
        public void describeTo(Description description) { }

        @Override
        public void describeMismatch(Object logs, Description description) {
            description.appendText("Expected number of instances were ")
                    .appendValue(numberOfInstances)
                    .appendText("was ")
                    .appendValue(findNumberOfMatches((String) logs))
                    .appendText("logs were ")
                    .appendValue(logs);
        }
    }

    public static InferenceConfiguration getConfig(TemporaryFolder trainDir) throws Exception {
        Pair<MultiLayerNetwork, DataNormalization> multiLayerNetwork = TrainUtils.getTrainedNetwork();
        File modelSave = trainDir.newFile("model.zip");
        ModelSerializer.writeModel(multiLayerNetwork.getFirst(), modelSave, false);

        Schema.Builder schemaBuilder = new Schema.Builder();
        schemaBuilder.addColumnDouble("petal_length")
                .addColumnDouble("petal_width")
                .addColumnDouble("sepal_width")
                .addColumnDouble("sepal_height");
        Schema inputSchema = schemaBuilder.build();

        Schema.Builder outputSchemaBuilder = new Schema.Builder();
        outputSchemaBuilder.addColumnDouble("setosa");
        outputSchemaBuilder.addColumnDouble("versicolor");
        outputSchemaBuilder.addColumnDouble("virginica");
        Schema outputSchema = outputSchemaBuilder.build();

        ServingConfig servingConfig = new ServingConfig()
                .createLoggingEndpoints(true);

        Dl4jStep modelPipelineStep = Dl4jStep.builder()
                .inputName("default")
                .inputColumnName("default", SchemaTypeUtils.columnNames(inputSchema))
                .inputSchema("default", SchemaTypeUtils.typesForSchema(inputSchema))
                .outputSchema("default", SchemaTypeUtils.typesForSchema(outputSchema))
                .path(modelSave.getAbsolutePath())
                .outputColumnName("default", SchemaTypeUtils.columnNames(outputSchema))
                .build();

        return InferenceConfiguration.builder()
                .servingConfig(servingConfig)
                .step(modelPipelineStep)
                .build();
    }
}
