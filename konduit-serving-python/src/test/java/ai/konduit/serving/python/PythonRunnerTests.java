package ai.konduit.serving.python;

import ai.konduit.serving.data.nd4j.data.ND4JNDArray;
import ai.konduit.serving.model.PythonConfig;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import org.datavec.python.PythonType;
import org.junit.Test;
import org.nd4j.linalg.factory.Nd4j;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class PythonRunnerTests {

    @Test
    public void testAllInputTypes() {
        PythonType.TypeName[] values = PythonType.TypeName.values();
        PythonConfig.PythonConfigBuilder builder = PythonConfig.builder();
        StringBuffer codeBuffer = new StringBuffer();

        Data data = Data.empty();
        Data assertion = Data.empty();
        for(int i = 0; i < values.length; i++) {
            String varName = "i" + String.valueOf(i);
            String codeSnippet = varName + " += 1\n";
            switch(values[i]) {
                case FLOAT:
                    builder.pythonInput(String.valueOf(i),values[i].name());
                    builder.pythonOutput(varName,values[i].name());
                    codeBuffer.append(codeSnippet);
                    data.put(varName,1.0f);
                    assertion.put(varName,2.0f);
                    break;
                case INT:
                    builder.pythonInput(varName,values[i].name());
                    builder.pythonOutput(varName,values[i].name());
                    codeBuffer.append(codeSnippet);
                    data.put(varName,1);
                    assertion.put(varName,2);
                    break;
                case DICT:
                    break;
                case STR:
                    builder.pythonInput(varName,values[i].name());
                    builder.pythonOutput(varName,values[i].name());
                    codeBuffer.append(varName + " += '1'\n");
                    data.put(varName,String.valueOf(1));
                    assertion.put(varName,"11");
                    break;
                case NDARRAY:
                    builder.pythonInput(varName,values[i].name());
                    builder.pythonOutput(varName,values[i].name());
                    codeBuffer.append(codeSnippet);
                    data.put(varName,new ND4JNDArray(Nd4j.scalar(1.0)));
                    assertion.put(varName,new ND4JNDArray(Nd4j.scalar(2.0)));
                    break;
                case BYTES:
                   /* builder.pythonInput(varName,values[i].name());
                    builder.pythonOutput(varName,values[i].name());
                    codeBuffer.append(varName + ".append(1)");
                    data.put(varName,new byte[]{1});*/
                    break;
         /*       case LIST:
                    break;*/
                case BOOL:
                    builder.pythonInput(varName,values[i].name());
                    builder.pythonOutput(varName,values[i].name());
                    data.put(varName,true);
                    codeBuffer.append(varName + " = True\n");
                    assertion.put(varName,true);
                    break;
            }
        }

        builder.pythonCode(codeBuffer.toString());

        PythonConfig pythonConfig = builder.build();
        PythonStep pythonStep = new PythonStep()
                .pythonConfig(pythonConfig);
        SequencePipeline sequencePipeline = SequencePipeline.builder()
                .add(pythonStep)
                .build();

        PipelineExecutor executor = sequencePipeline.executor();
        Data exec = executor.exec(data);
        assertEquals(exec.keys().size(),pythonConfig.getPythonOutputs().size());
        assertEquals(assertion,exec);


    }
}
