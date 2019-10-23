package ai.konduit.serving.basic;

import ai.konduit.serving.pipeline.TransformProcessPipelineStep;
import ai.konduit.serving.pipeline.steps.TransformProcessPipelineStepRunner;
import org.datavec.api.records.Record;
import org.datavec.api.transform.MathFunction;
import org.datavec.api.transform.MathOp;
import org.datavec.api.transform.TransformProcess;
import org.datavec.api.transform.schema.Schema;
import org.datavec.api.writable.NDArrayWritable;
import org.nd4j.linalg.factory.Nd4j;

import java.util.Arrays;

public class BasicConfiguration {
    public static void main(String[] args) throws Exception {
        /*
         * All the configurations are contained inside the InferenceConfiguration class:
         *
         * It has two items inside it.
         * ---------------------------
         * 1. ServingConfig: That contains configuration related to where the server will run and which
         *    path it will listen to.
         * 2. List<PipelineStep>: This is responsible for containing a list of steps in a Machine Learning Pipeline.
         * ---------------------------
         * PipelineStep is the base of how we will define all of our configuration inside Konduit Serving.
         *
         * Let's get started...
         */

        // Let's create a PipelineStep for a simple transform process that performs mathematical functions on arrays
        Schema inputSchema = new Schema.Builder()
                .addColumnNDArray("x1", new long[]{10, 10})
                .addColumnNDArray("x2", new long[]{5, 5})
                .build();

        Schema outputSchema = new Schema.Builder()
                .addColumnNDArray("y1", new long[]{10, 10})
                .addColumnNDArray("y2", new long[]{5, 5})
                .build();

        TransformProcess transformProcess = new TransformProcess.Builder(inputSchema)
                .ndArrayScalarOpTransform("x1", MathOp.Add, 20.0)
                .ndArrayScalarOpTransform("x2", MathOp.Divide, 20.0)
                .ndArrayMathFunctionTransform("x2", MathFunction.SIN)
                .ndArrayMathFunctionTransform("x2", MathFunction.FLOOR)
                .ndArrayScalarOpTransform("x2", MathOp.Add, 2)
                .build();

        TransformProcessPipelineStep transformProcessPipelineStep = new TransformProcessPipelineStep()
                .step("default", inputSchema, outputSchema, transformProcess);

        TransformProcessPipelineStepRunner transformProcessPipelineStepRunner = new TransformProcessPipelineStepRunner(transformProcessPipelineStep);

        Record[] output = transformProcessPipelineStepRunner.transform(
                new Record[] {
                        new org.datavec.api.records.impl.Record(
                                Arrays.asList(
                                        new NDArrayWritable(Nd4j.rand(10, 10).muli(100)),
                                        new NDArrayWritable(Nd4j.rand(5, 5).muli(100))
                                ), null
                        )
                }
        );

        System.out.println(String.format("%s\n%s", output[0].getRecord().get(0), output[0].getRecord().get(1)));
    }
}
