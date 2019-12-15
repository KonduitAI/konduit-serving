package ai.konduit.serving.pipeline.generator.impl;

import ai.konduit.serving.model.PythonConfig;
import ai.konduit.serving.util.python.PythonVariables.Type;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PythonPipelineGeneratorTests {

    @Test
    public void testConversionCode() {
        //tests all type conversion combinations
        PythonPipelineGenerator pythonPipelineGenerator = PythonPipelineGenerator.builder()
                .seed(42)
                .build();
        for(Type inputType : Type.values()) {
            for(Type outputType : Type.values()) {
                String value = PythonPipelineGenerator.conversionCode(
                        inputType,
                        outputType,
                        pythonPipelineGenerator.randomObjectForType(inputType),
                        "outputVariableName");
                System.out.println("Input type " + inputType + " Output Type " + outputType + " with conversion code " + value);
            }
        }

    }

    @Test
    public void testGetConfig() {
        PythonPipelineGenerator pythonPipelineGenerator = PythonPipelineGenerator.builder()
                .seed(42)
                .build();
        PythonConfig pythonConfig = pythonPipelineGenerator.createRandomConfiguration();
        assertEquals(pythonConfig.getPythonInputs().size(),pythonConfig.getPythonOutputs().size());


    }


}
