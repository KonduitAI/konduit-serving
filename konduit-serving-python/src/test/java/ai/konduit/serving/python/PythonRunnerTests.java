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

        for(int i = 0; i < values.length; i++) {
            switch(values[i]) {
                case FLOAT:
                    builder.pythonInput(String.valueOf(i),values[i].name());
                    data.put("i" + String.valueOf(i),1.0f);
                    break;
                case INT:
                    builder.pythonInput(String.valueOf(i),values[i].name());
                    data.put("i" + String.valueOf(i),1);
                    break;
           /*     case DICT:
                    builder.pythonInput(String.valueOf(i),values[i].name());
                    data.put(String.valueOf(i), Collections.singletonMap(String.valueOf(i),1));
                    break;*/
                case STR:
                    builder.pythonInput(String.valueOf(i),values[i].name());
                    data.put("i" + String.valueOf(i),String.valueOf(1));
                    break;
                case NDARRAY:
                    builder.pythonInput(String.valueOf(i),values[i].name());
                    data.put("i" + String.valueOf(i),new ND4JNDArray(Nd4j.scalar(1.0)));
                    break;
                case BYTES:
                    builder.pythonInput("i" + String.valueOf(i),values[i].name());
                    break;
         /*       case LIST:
                    break;*/
                case BOOL:
                    builder.pythonInput(String.valueOf(i),values[i].name());
                    data.put("i" + String.valueOf(i),true);
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
    }
}
