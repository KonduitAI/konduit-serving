package ai.konduit.serving.pipeline.steps;

import ai.konduit.serving.pipeline.step.TransformProcessStep;
import ai.konduit.serving.train.TrainUtils;
import ai.konduit.serving.util.SchemaTypeUtils;
import org.datavec.api.transform.TransformProcess;
import org.datavec.api.transform.schema.Schema;
import org.junit.Test;

import java.util.Arrays;

import static ai.konduit.serving.train.TrainUtils.getIrisOutputSchema;

public class TransformProcessStepTest {

    @Test
    public void testTransformProcessPipe() throws Exception {

        Schema inputSchema = TrainUtils.getIrisInputSchema();
        Schema outputSchema = getIrisOutputSchema();
        TransformProcess.Builder transformProcessBuilder = new TransformProcess.Builder(inputSchema);
        for(int i = 0; i < inputSchema.numColumns(); i++) {
            transformProcessBuilder.convertToDouble(inputSchema.getName(i));
        }
        TransformProcess tp = transformProcessBuilder.build();

        TransformProcessStep tpps = new TransformProcessStep()
                .step("foo", tp, outputSchema)
                .step("bar", tp, outputSchema);

        assert tpps.getInputNames().contains("foo");
        assert tpps.getInputNames().contains("bar");

        assert tpps.getOutputNames().contains("foo");
        assert tpps.getOutputNames().contains("bar");

        TransformProcessStep tpps2 = new TransformProcessStep(tp, outputSchema);

        assert  tpps2.getTransformProcesses().get("default") == tp;

        assert Arrays.equals(tpps2.getInputSchemas().get("default"), SchemaTypeUtils.typesForSchema(inputSchema));

    }
}
