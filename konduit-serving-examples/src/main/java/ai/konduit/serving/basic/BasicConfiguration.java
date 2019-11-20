package ai.konduit.serving.basic;

import ai.konduit.serving.pipeline.step.TransformProcessStep;
import org.datavec.api.transform.MathFunction;
import org.datavec.api.transform.MathOp;
import org.datavec.api.transform.TransformProcess;
import org.datavec.api.transform.schema.Schema;
import org.datavec.api.writable.Writable;
import org.nd4j.linalg.factory.Nd4j;

import java.util.Arrays;

public class BasicConfiguration {
    public static void main(String[] args) throws Exception {
        // A Pipeline Step for a simple transform process that performs mathematical functions on arrays

        // First we'll define the input and output schema with two array inputs (x1 and x2)
        Schema inputSchema = new Schema.Builder()
                .addColumnNDArray("x1", new long[]{10, 10}) // x1: 10x10 array
                .addColumnNDArray("x2", new long[]{5, 5}) // x2: 5x5 array
                .addColumnNDArray("x3", new long[]{2, 2})
                .build();

        /*
         * Since we'll not start a server and do transformation without it, we don't really need to define
         * the output schema. But still doing so for completeness. Let's call them y1 and y2.
         */
        Schema outputSchema = new Schema.Builder()
                .addColumnNDArray("y1", new long[]{10, 10}) // y1: 10x10 array
                .addColumnNDArray("y2", new long[]{5, 5}) // y2: 5x5 array
                .addColumnNDArray("y3", new long[]{2, 2}) // y3: 2x2 array
                .build();

        /*
         * Then we'll define a transform process that operates on the defined inputs.
         */
        TransformProcess transformProcess = new TransformProcess.Builder(inputSchema)
                .ndArrayScalarOpTransform("x1", MathOp.Add, 20.0) // Adds 20.0 to array elements
                .ndArrayScalarOpTransform("x2", MathOp.Divide, 20.0) // Divides each element by 20.0
                .ndArrayMathFunctionTransform("x2", MathFunction.SIN) // Applies sin() function to each element
                .ndArrayMathFunctionTransform("x2", MathFunction.FLOOR) // Floors each element
                .ndArrayScalarOpTransform("x2", MathOp.Add, 2) // Adds 2 to each element
                .ndArrayScalarOpTransform("x1", MathOp.ScalarMin, 50)
                .ndArrayScalarOpTransform("x3", MathOp.Multiply, 232)
                .ndArrayScalarOpTransform("x3", MathOp.Modulus, 15)
                .build();

        /*
         * Now we'll create the pipeline step for the transform process
         */
        TransformProcessStep transformProcessStep = new TransformProcessStep()
                .step("input1", transformProcess, outputSchema)
                .step("input2", transformProcess, outputSchema);

        /*
         * Now creating a test record input
         */
        Writable[][] output = transformProcessStep.getRunner().transform(
                new Object[] {
                        Nd4j.rand(10, 10).muli(100),
                        Nd4j.rand(5, 5).muli(100),
                        Nd4j.ones(2, 2)
                }, new Object[] {
                        Nd4j.rand(10, 10).muli(100),
                        Nd4j.rand(5, 5).muli(100),
                        Nd4j.ones(2, 2)
                }
        );

        System.out.println(Arrays.deepToString(output));
    }
}