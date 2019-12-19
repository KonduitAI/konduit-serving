package ai.konduit.serving.pipeline.generator.impl;

import org.junit.Test;

public class PythonPipelineGeneratorTests {

    @Test
    public void testConversionCode() {
        //tests all type conversion combinations to ensure basic errors don't happen
        //PythonPipelineGenerator pythonPipelineGenerator = PythonPipelineGenerator.builder()
        //        .seed(42)
        //        .build();
        //for(Type inputType : Type.values()) {
        //    for(Type outputType : Type.values()) {
        //        String value = PythonPipelineGenerator.conversionCode(
        //                inputType,
        //                outputType,
        //                pythonPipelineGenerator.randomObjectForType(inputType),
        //                "outputVariableName");
        //        System.out.println("Input type " + inputType + " Output Type " + outputType + " with conversion code " + value);
        //    }
        //}
    }

    @Test
    public void testGetConfig() {
        //PythonPipelineGenerator pythonPipelineGenerator = PythonPipelineGenerator.builder()
        //        .seed(42)
        //        .build();
        //PythonConfig pythonConfig = pythonPipelineGenerator.generate().;
        //PythonExecutioner.exec(pythonConfig.getPythonCode());
        //assertEquals(pythonConfig.getPythonInputs().size(),pythonConfig.getPythonOutputs().size());
        //Assert.assertNotNull(pythonConfig.getPythonCode());
    }
}
