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
package ai.konduit.serving.python;

import ai.konduit.serving.annotation.runner.CanRun;
import ai.konduit.serving.data.nd4j.data.ND4JNDArray;
import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.*;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import ai.konduit.serving.python.util.KonduitPythonUtils;
import lombok.SneakyThrows;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.nd4j.common.base.Preconditions;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.python4j.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.nd4j.python4j.PythonTypes.*;

@CanRun(PythonStep.class)
@Slf4j
public class PythonRunner implements PipelineStepRunner {

    private PythonStep pythonStep;
    private KonduitPythonJob konduitPythonJob;

    @SneakyThrows
    public PythonRunner(PythonStep pythonStep) {
        this.pythonStep = pythonStep;
        String code = pythonStep.pythonConfig().getPythonCode();
        if (code == null) {
            try {
                code = FileUtils.readFileToString(new File(pythonStep.pythonConfig().getPythonCodePath()), StandardCharsets.UTF_8);
            } catch (IOException e) {
                log.error("Unable to read code from " + pythonStep.pythonConfig().getPythonCodePath());
            }
            log.info("Resolving code from " + pythonStep.pythonConfig().getPythonCodePath());
        }

        konduitPythonJob = KonduitPythonJob.builder()
                .name(pythonStep.pythonConfig().getJobSuffix())
                .setupRunMode(pythonStep.pythonConfig().isSetupAndRun())
                .code(code)
                .useGil(pythonStep.pythonConfig().isUseGil())
                .build();
    }


    @Override
    public void close() {

    }

    @Override
    public PipelineStep getPipelineStep() {
        return pythonStep;
    }

    @SneakyThrows
    @Override
    public Data exec(Context ctx, Data data) {
        PythonVariables pythonVariables = new PythonVariables();
        Data ret = Data.empty();
        for(String key : data.keys()) {
            switch(data.type(key)) {
                case NDARRAY:
                    NDArray ndArray = data.getNDArray(key);
                    INDArray arr = ndArray.getAs(INDArray.class);
                    pythonVariables.add(key, NumpyArray.INSTANCE,arr);
                    break;
                case BYTES:
                    byte[] bytes = data.getBytes(key);
                    pythonVariables.add(key, BYTES,bytes);
                    break;
                case DOUBLE:
                    double aDouble = data.getDouble(key);
                    pythonVariables.add(key,PythonTypes.FLOAT,aDouble);
                    break;
                case LIST:
                    Preconditions.checkState(pythonStep.pythonConfig().getListTypesForVariableName().containsKey(key),"No input type specified for list with key " + key);
                    ValueType valueType = pythonStep.pythonConfig().getListTypesForVariableName().get(key);
                    List<Object> list = data.getList(key, valueType);
                    pythonVariables.add(key, LIST,list);
                    break;
                case INT64:
                    long aLong = data.getLong(key);
                    pythonVariables.add(key,PythonTypes.INT,aLong);
                    break;
                case BOOLEAN:
                    boolean aBoolean = data.getBoolean(key);
                    pythonVariables.add(key, BOOL,aBoolean);
                    break;
                case STRING:
                    String string = data.getString(key);
                    pythonVariables.add(key,PythonTypes.STR,string);
                    break;
                case IMAGE:
                    Image image = data.getImage(key);
                    KonduitPythonUtils.addImageToPython(pythonVariables,key,image);
                    break;
                case BOUNDING_BOX:
                    BoundingBox boundingBox = data.getBoundingBox(key);
                    Map<String,Object> boundingBoxValues = DictUtils.toBoundingBoxDict(boundingBox);
                    pythonVariables.add(key,KonduitPythonUtils.pythonTypeFor(Map.class),boundingBoxValues);
                    break;
                case POINT:
                    Point point = data.getPoint(key);
                    Map<String,Object> pointerValue = DictUtils.toPointDict(point);
                    pythonVariables.add(key,KonduitPythonUtils.pythonTypeFor(Map.class),pointerValue);
                    break;
                case DATA:
                    throw new IllegalArgumentException("Illegal type " + data.type(key));

            }
        }

        PythonVariables outputs = new PythonVariables();
        Map<String, String> pythonOutputs = pythonStep.pythonConfig().getPythonOutputs();
        for(Map.Entry<String,String> entry : pythonOutputs.entrySet()) {
            outputs.add(new PythonVariable(entry.getKey(),PythonTypes.get(entry.getValue())));
        }

        konduitPythonJob.exec(pythonVariables,outputs);

        for(PythonVariable variable : outputs) {
            switch(variable.getType().getName().toLowerCase()) {
                case "bool":
                    ret.put(variable.getName(),KonduitPythonUtils.getWithType(outputs,variable.getName(),Boolean.class));
                    break;
                case "list":
                    Preconditions.checkState(pythonStep.pythonConfig().getListTypesForVariableName().containsKey(variable.getName()),"No input type specified for list with key " + variable);
                    List listValue = KonduitPythonUtils.getWithType(outputs,variable.getName(),List.class);
                    ValueType valueType = pythonStep.pythonConfig().getListTypesForVariableName().get(variable.getName());
                    KonduitPythonUtils.insertListIntoData(ret, variable.getName(), listValue, valueType);
                    break;
                case "bytes":
                    KonduitPythonUtils.insertBytesIntoPythonVariables(
                            ret,
                            outputs,
                            variable.getName(),
                            pythonStep.pythonConfig());
                    break;
                case "numpy.ndarray":
                    ret.put(variable.getName(),new ND4JNDArray(KonduitPythonUtils.getWithType(outputs,variable.getName(),INDArray.class)));
                    break;
                case "str":
                    ret.put(variable.getName(),KonduitPythonUtils.getWithType(outputs,variable.getName(),String.class));
                    break;
                case "dict":
                    ValueType dictValueType = pythonStep.pythonConfig().getTypeForDictionaryForOutputVariableNames().get(variable.getName());
                    Map<String,Object> items = (Map<String, Object>) KonduitPythonUtils.getWithType(outputs,variable.getName(),Map.class);
                    switch(dictValueType) {
                        case POINT:
                            ret.put(variable.getName(),DictUtils.fromPointDict(items));
                            break;
                        case BOUNDING_BOX:
                            ret.put(variable.getName(),DictUtils.boundingBoxFromDict(items));
                            break;
                        default:
                            throw new IllegalArgumentException("Limited support for de serializing dictionaries. Invalid type " + dictValueType);

                    }
                    break;
                case "int":
                    ret.put(variable.getName(),KonduitPythonUtils.getWithType(outputs,variable.getName(),Long.class));
                    break;
                case "float":
                    ret.put(variable.getName(),KonduitPythonUtils.getWithType(outputs,variable.getName(),Double.class));
                    break;

            }
        }

        return ret;
    }

}
