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

import ai.konduit.serving.model.PythonConfig;
import ai.konduit.serving.model.PythonIO;
import ai.konduit.serving.pipeline.api.data.ValueType;
import ai.konduit.serving.pipeline.api.python.models.PythonConfigType;
import picocli.CommandLine;

public class PythonConfigTypeConverter implements CommandLine.ITypeConverter<PythonConfig> {
    @Override
    public PythonConfig convert(String value) throws Exception {
        String[] split = value.split(",");
        PythonConfig.PythonConfigBuilder builder = PythonConfig.builder();
        for(String keyVals : split) {
            String[] keyVal = keyVals.split("=");
            switch(keyVal[0]) {
                case "pythonPath":
                    builder.pythonPath(keyVal[1]);
                    break;
                case "pythonConfigType":
                    builder.pythonConfigType(PythonConfigType.valueOf(keyVal[1].toUpperCase()));
                    break;
                case  "pythonCode":
                    builder.pythonCode(keyVal[1]);
                    break;
                case "pythonCodePath":
                    builder.pythonCodePath(keyVal[1]);
                    break;
                case "returnAllInputs":
                    builder.returnAllInputs(Boolean.parseBoolean(keyVal[1]));
                    break;
                case "setupAndRun":
                    builder.setupAndRun(Boolean.parseBoolean(keyVal[1]));
                    break;
                case "pythonLibrariesPath":
                    builder.pythonLibrariesPath(keyVal[1]);
                    break;
                case "ioInput":
                    PythonIO.PythonIOBuilder pythonIOBuilder = PythonIO.builder();
                    String[] ioDescriptor = keyVal[1].split(" ");
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
                    String[] ioDescriptorOut = keyVal[1].split(" ");
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
