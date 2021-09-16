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
package ai.konduit.serving.configcreator.converter;

import ai.konduit.serving.configcreator.StringSplitter;
import ai.konduit.serving.model.PythonConfig;
import ai.konduit.serving.model.PythonIO;
import ai.konduit.serving.pipeline.api.data.ValueType;
import ai.konduit.serving.pipeline.api.python.models.PythonConfigType;
import picocli.CommandLine;

import java.util.Map;

public class PythonConfigTypeConverter implements CommandLine.ITypeConverter<PythonConfig> {
    @Override
    public PythonConfig convert(String value) throws Exception {
        StringSplitter stringSplitter = new StringSplitter(",");
        Map<String, String> stringStringMap = stringSplitter.splitResult(value);
        String[] split = value.split(",");
        PythonConfig.PythonConfigBuilder builder = PythonConfig.builder();
        for(Map.Entry<String,String> entry : stringStringMap.entrySet()) {
            switch(entry.getKey()) {
                case "pythonPath":
                    builder.pythonPath(entry.getValue());
                    break;
                case "pythonConfigType":
                    builder.pythonConfigType(PythonConfigType.valueOf(entry.getValue().toUpperCase()));
                    break;
                case  "pythonCode":
                    builder.pythonCode(entry.getValue());
                    break;
                case "pythonCodePath":
                    builder.pythonCodePath(entry.getValue());
                    break;
                case "returnAllInputs":
                    builder.returnAllInputs(Boolean.parseBoolean(entry.getValue()));
                    break;
                case "setupAndRun":
                    builder.setupAndRun(Boolean.parseBoolean(entry.getValue()));
                    break;
                case "pythonLibrariesPath":
                    builder.pythonLibrariesPath(entry.getValue());
                    break;
                case "ioInput":
                    PythonIO.PythonIOBuilder pythonIOBuilder = PythonIO.builder();
                    String[] ioDescriptor = entry.getValue().split(" ");
                    pythonIOBuilder.name(ioDescriptor[0]);
                    if(ioDescriptor.length > 1)
                        pythonIOBuilder.pythonType(ioDescriptor[1]);
                    if(ioDescriptor.length > 2)
                        pythonIOBuilder.type(ValueType.valueOf(ioDescriptor[2]));
                    if(ioDescriptor.length > 3)
                        pythonIOBuilder.secondaryType(ValueType.valueOf(ioDescriptor[3]));
                    builder.ioInput(ioDescriptor[0],pythonIOBuilder
                            .build());
                    break;
                case "ioOutput":
                    PythonIO.PythonIOBuilder pythonIOBuilderOut = PythonIO.builder();
                    String[] ioDescriptorOut = entry.getValue().split(" ");
                    pythonIOBuilderOut.name(ioDescriptorOut[0]);
                    if(ioDescriptorOut.length > 1)
                        pythonIOBuilderOut.pythonType(ioDescriptorOut[1]);
                    if(ioDescriptorOut.length > 2)
                        pythonIOBuilderOut.type(ValueType.valueOf(ioDescriptorOut[2]));
                    if(ioDescriptorOut.length > 3)
                        pythonIOBuilderOut.secondaryType(ValueType.valueOf(ioDescriptorOut[3]));
                    builder.ioOutput(ioDescriptorOut[0],pythonIOBuilderOut.build());
                    break;
            }
        }

        return builder.build();
    }
}
