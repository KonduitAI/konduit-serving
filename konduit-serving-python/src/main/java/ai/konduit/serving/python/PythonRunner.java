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
import ai.konduit.serving.model.PythonIO;
import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.ValueType;
import ai.konduit.serving.pipeline.api.python.models.AppendType;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import ai.konduit.serving.python.util.KonduitPythonUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.nd4j.common.base.Preconditions;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.python4j.PythonExecutioner;
import org.nd4j.python4j.PythonGIL;
import org.nd4j.python4j.PythonVariable;
import org.nd4j.python4j.PythonVariables;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.bytedeco.cpython.global.python.PyGILState_Check;

@CanRun(PythonStep.class)
@Slf4j
public class PythonRunner implements PipelineStepRunner {

    private PythonStep pythonStep;
    private String code;


    @SneakyThrows
    public PythonRunner(PythonStep pythonStep) {
        this.pythonStep = pythonStep;
        String code = pythonStep.pythonConfig().getPythonCode();

        AppendType appendType = this.pythonStep.pythonConfig().getAppendType();
        String pythonLibrariesPath = this.pythonStep.pythonConfig().getPythonLibrariesPath();

        if(pythonLibrariesPath == null) pythonLibrariesPath = this.pythonStep.pythonConfig().resolvePythonLibrariesPath();

        log.info("Over riding python path " + pythonLibrariesPath);
        System.setProperty("org.eclipse.python4j.path", pythonLibrariesPath);
        System.setProperty("org.eclipse.python4j.path.append", appendType == null ?
                AppendType.BEFORE.name() :
                appendType.name().toLowerCase());

        new PythonExecutioner();

        if (code == null) {
            try {
                this.code = FileUtils.readFileToString(new File(pythonStep.pythonConfig().getPythonCodePath()), StandardCharsets.UTF_8);
            } catch (IOException e) {
                log.error("Unable to read code from " + pythonStep.pythonConfig().getPythonCodePath(), e);
            }
            log.info("Resolving execution code from " + pythonStep.pythonConfig().getPythonCodePath());
        }
        else
            this.code = code;

        String importCode = pythonStep.pythonConfig().getImportCode();
        String importCodePath = pythonStep.pythonConfig().getImportCodePath();
        if (importCode == null && importCodePath != null) {
            try {
                importCode = FileUtils.readFileToString(new File(importCodePath), StandardCharsets.UTF_8);
            } catch (IOException e) {
                log.error("Unable to read code from " + pythonStep.pythonConfig().getImportCodePath(), e);
            }

            log.info("Resolving import code from " + pythonStep.pythonConfig().getImportCodePath());
        }

        if(importCode != null) {
            try(PythonGIL ignored = PythonGIL.lock()) {
                PythonExecutioner.exec(importCode);
            }
        }
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
        Data ret = Data.empty();
        PythonVariables outputs = KonduitPythonUtils.createOutputVariables(pythonStep.pythonConfig());
        PythonVariables pythonVariables = KonduitPythonUtils.createPythonVariablesFromDataInput(data, pythonStep.pythonConfig());
        try(PythonGIL ignored = PythonGIL.lock()) {
            log.debug("Thread " + Thread.currentThread().getId() + " has the GIL. Name of thread " + Thread.currentThread().getName());
            log.debug("Py gil state " + (PyGILState_Check() > 0));
            runExec(ret, outputs, pythonVariables);
        }

        return ret;
    }

    private void runExec(Data ret, PythonVariables outputs, PythonVariables pythonVariables) throws IOException {
        PythonExecutioner.exec(code, pythonVariables, outputs);
        Preconditions.checkNotNull(outputs,"No outputs found!");
        for(PythonVariable variable : outputs) {
            PythonIO pythonIO = pythonStep.pythonConfig().getIoOutputs().get(variable.getName());
            Preconditions.checkNotNull(pythonIO,"No variable found for " + variable.getName());
            switch(variable.getType().getName().toLowerCase()) {
                case "bool":
                    ret.put(variable.getName(),KonduitPythonUtils.getWithType(outputs,variable.getName(),Boolean.class));
                    break;
                case "list":
                    Preconditions.checkState(pythonIO.isListWithType(),"No output type specified for list with key " + variable);
                    List<Object> listValue = KonduitPythonUtils.getWithType(outputs,variable.getName(),List.class);
                    ValueType valueType = pythonIO.secondaryType();
                    Preconditions.checkNotNull(listValue,"List value returned null for output named " + variable.getName() + " type should have been list of " + valueType);
                    List<Object> convertedInput = KonduitPythonUtils.createValidListForPythonVariables(listValue,valueType);
                    KonduitPythonUtils.insertListIntoData(ret, variable.getName(), convertedInput, valueType);
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
                    ValueType dictValueType = pythonIO.type();
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
                default:
                    break;
            }
        }
    }

}
