package ai.konduit.serving.pipeline.generator.impl;

import ai.konduit.serving.util.python.PythonVariables.Type;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PythonPipelineGeneratorTests {

    @Test
    public void testConversionCode() {
        assertEquals("outputVariableName = True\n", PythonPipelineGenerator.conversionCode(Type.BOOL, Type.BOOL, true, "outputVariableName"));
    }
}
