package ai.konduit.serving.pipeline.steps;

import ai.konduit.serving.pipeline.TransformProcessPipelineStep;
import ai.konduit.serving.train.TrainUtils;
import org.datavec.api.transform.TransformProcess;
import org.datavec.api.transform.schema.Schema;
import org.junit.Test;

import java.util.ArrayList;

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

        TransformProcessPipelineStep tpps = new TransformProcessPipelineStep()
                .step("foo", inputSchema, outputSchema, tp)
                .step("bar", inputSchema, outputSchema, tp);

        assert tpps.getInputNames().contains("foo");
        assert tpps.getInputNames().contains("bar");

    }
}
