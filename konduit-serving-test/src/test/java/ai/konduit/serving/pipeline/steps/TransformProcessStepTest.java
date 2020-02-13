package ai.konduit.serving.pipeline.steps;

import ai.konduit.serving.pipeline.step.TransformProcessStep;
import ai.konduit.serving.train.TrainUtils;
import ai.konduit.serving.util.SchemaTypeUtils;
import org.datavec.api.transform.TransformProcess;
import org.datavec.api.transform.schema.Schema;
import org.junit.Test;

import static ai.konduit.serving.train.TrainUtils.getIrisOutputSchema;
import static org.junit.Assert.*;

public class TransformProcessStepTest {

    @Test
    public void testTransformProcessPipe() throws Exception {

        Schema inputSchema = TrainUtils.getIrisInputSchema();
        Schema outputSchema = getIrisOutputSchema();
        TransformProcess.Builder transformProcessBuilder = new TransformProcess.Builder(inputSchema);
        for (int i = 0; i < inputSchema.numColumns(); i++) {
            transformProcessBuilder.convertToDouble(inputSchema.getName(i));
        }
        TransformProcess tp = transformProcessBuilder.build();

        TransformProcessStep tpps = new TransformProcessStep()
                .step("foo", tp, outputSchema)
                .step("bar", tp, outputSchema);

        assertTrue(tpps.getInputNames().contains("foo"));
        assertTrue(tpps.getInputNames().contains("bar"));

        assertTrue(tpps.getOutputNames().contains("foo"));
        assertTrue(tpps.getOutputNames().contains("bar"));

        TransformProcessStep tpps2 = new TransformProcessStep(tp, outputSchema);

        assertSame(tpps2.getTransformProcesses().get("default"), tp);

        assertArrayEquals(tpps2.getInputSchemas().get("default"), SchemaTypeUtils.typesForSchema(inputSchema));
    }
}
