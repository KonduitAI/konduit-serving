/*
 *  ******************************************************************************
 *  *
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Apache License, Version 2.0 which is available at
 *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  *  See the NOTICE file distributed with this work for additional
 *  *  information regarding copyright ownership.
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  * License for the specific language governing permissions and limitations
 *  * under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *  *****************************************************************************
 */
package ai.konduit.serving.configcreator.integrationtest;

import ai.konduit.serving.configcreator.StepCreator;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.python.models.AppendType;
import ai.konduit.serving.pipeline.api.python.models.PythonConfigType;
import ai.konduit.serving.pipeline.util.ObjectMappers;
import ai.konduit.serving.python.PythonStep;
import ai.konduit.serving.vertx.config.InferenceConfiguration;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import picocli.CommandLine;

import java.io.File;
import java.nio.charset.Charset;
import java.text.DateFormat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EndToEndTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testHelp() {
        CLITestCase cliTestCase = new CLITestCase("-h");
        cliTestCase.exec(2);
    }

    @Test
    public void testIntegration() throws Exception {
        CLITestCase cliTestCase = new CLITestCase("step-create python --fileFormat=json --pythonConfig=\"pythonConfigType=JAVACPP,appendType=NONE\"");
        cliTestCase.exec(0);
        String s = cliTestCase.getCliOutput();
        PythonStep pythonStep = ObjectMappers.json().readValue(s,PythonStep.class);
        assertEquals(PythonConfigType.JAVACPP,pythonStep.pythonConfig().getPythonConfigType());
        assertEquals(AppendType.NONE,pythonStep.pythonConfig().getAppendType());

        File tmpJson = temporaryFolder.newFile("pipeline.json");
        FileUtils.write(tmpJson,s, Charset.defaultCharset());

        CLITestCase pipelineTestCase = new CLITestCase("sequence-pipeline-creator --file-format=json --pipeline=" + tmpJson.getAbsolutePath());
        pipelineTestCase.exec(0);
        String pipelineOutput = pipelineTestCase.getCliOutput();
        Pipeline pipeline = ObjectMappers.fromJson(pipelineOutput,Pipeline.class);
        assertNotNull(pipeline);

        File pipelineFile = temporaryFolder.newFile("inference-pipeline.json");
        FileUtils.write(pipelineFile,pipelineOutput, Charset.defaultCharset());


        CLITestCase inferenceServerCreate = new CLITestCase("inference-server-create --pipeline=" + pipelineFile.getAbsolutePath());
        inferenceServerCreate.exec(0);

        String inferenceServerJson = inferenceServerCreate.getCliOutput();
        InferenceConfiguration inferenceConfiguration = InferenceConfiguration.fromJson(inferenceServerJson);
        assertNotNull(inferenceConfiguration);
        assertEquals(1,inferenceConfiguration.pipeline().size());

        ObjectMappers.json().setDateFormat(DateFormat.getDateInstance());
        File inferenceConfigurationFile = temporaryFolder.newFile("inference-server.json");
        FileUtils.writeStringToFile(inferenceConfigurationFile,inferenceServerJson,Charset.defaultCharset());

    }

}
