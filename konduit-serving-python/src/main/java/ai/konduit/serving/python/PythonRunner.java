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
import ai.konduit.serving.python.util.PythonUtils;
import lombok.SneakyThrows;
import org.bytedeco.javacpp.BytePointer;
import org.datavec.python.*;
import org.nd4j.common.base.Preconditions;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.util.List;
import java.util.Map;

@CanRun(PythonStep.class)
public class PythonRunner implements PipelineStepRunner {

    private PythonStep pythonStep;
    private PythonJob pythonJob;

    @SneakyThrows
    public PythonRunner(PythonStep pythonStep) {
        this.pythonStep = pythonStep;
        pythonJob = PythonJob.builder()
                .setupRunMode(pythonStep.pythonConfig().isSetupAndRun())
                .code(pythonStep.pythonConfig().getPythonCode())
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
        PythonContextManager.deleteNonMainContexts();
        PythonVariables pythonVariables = new PythonVariables();
        Data ret = Data.empty();
        for(String key : data.keys()) {
            switch(data.type(key)) {
                case NDARRAY:
                    NDArray ndArray = data.getNDArray(key);
                    INDArray arr = ndArray.getAs(INDArray.class);
                    pythonVariables.addNDArray(key,arr);
                    break;
                case BYTES:
                    byte[] bytes = data.getBytes(key);
                    BytePointer bytePointer = new BytePointer(bytes);
                    pythonVariables.addBytes(key,bytePointer);
                    break;
                case DOUBLE:
                    double aDouble = data.getDouble(key);
                    pythonVariables.addFloat(key,aDouble);
                    break;
                case LIST:
                    Preconditions.checkState(pythonStep.pythonConfig().getListTypesForVariableName().containsKey(key),"No input type specified for list with key " + key);
                    ValueType valueType = pythonStep.pythonConfig().getListTypesForVariableName().get(key);
                    List<Object> list = data.getList(key, valueType);
                    pythonVariables.addList(key,list.toArray(new Object[list.size()]));
                    break;
                case INT64:
                    long aLong = data.getLong(key);
                    pythonVariables.addInt(key,aLong);
                    break;
                case BOOLEAN:
                    boolean aBoolean = data.getBoolean(key);
                    pythonVariables.addBool(key,aBoolean);
                    break;
                case STRING:
                    String string = data.getString(key);
                    pythonVariables.addStr(key,string);
                    break;
                case IMAGE:
                    Image image = data.getImage(key);
                    PythonUtils.addImageToPython(pythonVariables,key,image);
                    break;
                case BOUNDING_BOX:
                    BoundingBox boundingBox = data.getBoundingBox(key);
                    Map<String,Object> boundingBoxValues = DictUtils.toBoundingBoxDict(boundingBox);
                    pythonVariables.addDict(key,boundingBoxValues);
                    break;
                case POINT:
                    Point point = data.getPoint(key);
                    Map<String,Object> pointerValue = DictUtils.toPointDict(point);
                    pythonVariables.addDict(key,pointerValue);
                    break;
                case DATA:
                    throw new IllegalArgumentException("Illegal type " + data.type(key));

            }
        }

        PythonVariables outputs = new PythonVariables();
        Map<String, String> pythonOutputs = pythonStep.pythonConfig().getPythonOutputs();
        for(Map.Entry<String,String> entry : pythonOutputs.entrySet()) {
            outputs.add(entry.getKey(), PythonType.valueOf(entry.getValue()));
        }

        pythonJob.exec(pythonVariables,outputs);

        for(String variable : outputs.getVariables()) {
            switch(outputs.getType(variable).getName()) {
                case BOOL:
                    ret.put(variable,outputs.getBooleanValue(variable));
                    break;
                case LIST:
                    Preconditions.checkState(pythonStep.pythonConfig().getListTypesForVariableName().containsKey(variable),"No input type specified for list with key " + variable);
                    List listValue = outputs.getListValue(variable);
                    ValueType valueType = pythonStep.pythonConfig().getListTypesForVariableName().get(variable);
                    PythonUtils.insertListIntoData(ret, variable, listValue, valueType);
                    break;
                case BYTES:
                    PythonUtils.insertBytesIntoPythonVariables(
                            ret,
                            outputs,
                            variable,
                            pythonStep.pythonConfig());
                    break;
                case NDARRAY:
                    ret.put(variable,new ND4JNDArray(outputs.getNDArrayValue(variable)));
                    break;
                case STR:
                    ret.put(variable,outputs.getStrValue(variable));
                    break;
                case DICT:
                    ValueType dictValueType = pythonStep.pythonConfig().getTypeForDictionaryForOutputVariableNames().get(variable);
                    Map<String,Object> items = (Map<String, Object>) outputs.getDictValue(variable);
                    switch(dictValueType) {
                        case POINT:
                            ret.put(variable,DictUtils.fromPointDict(items));
                            break;
                        case BOUNDING_BOX:
                            ret.put(variable,DictUtils.boundingBoxFromDict(items));
                            break;
                        default:
                            throw new IllegalArgumentException("Limited support for de serializing dictionaries. Invalid type " + dictValueType);

                    }
                    break;
                case INT:
                    ret.put(variable,outputs.getIntValue(variable));
                    break;
                case FLOAT:
                    ret.put(variable,outputs.getFloatValue(variable));
                    break;

            }
        }

        return ret;
    }

}
