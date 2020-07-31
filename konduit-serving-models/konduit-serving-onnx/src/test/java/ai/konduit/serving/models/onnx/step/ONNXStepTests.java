package ai.konduit.serving.models.onnx.step;

import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import org.junit.Test;
import org.nd4j.common.resources.Resources;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.io.File;

import static org.junit.Assert.assertNotNull;

public class ONNXStepTests {

    @Test
    public void testOnnxMnist() {
        File f = Resources.asFile("mnist-8.onnx");
        ONNXStep onnxStep = new ONNXStep()
                .inputNames("Input3")
                .outputNames("Plus214_Output_0")
                .modelUri(f.getAbsolutePath());

        SequencePipeline sequencePipeline =SequencePipeline.builder()
                .add(onnxStep)
                .build();

        PipelineExecutor e = sequencePipeline.executor();


        INDArray arr = Nd4j.rand(DataType.FLOAT, 1,28,28);
        Data d = Data.singleton("Input3", NDArray.create(arr));
        Data out = e.exec(d);
        INDArray outRet = out.getNDArray("Plus214_Output_0").getAs(INDArray.class);
        assertNotNull(outRet);
        System.out.println(outRet);
    }

}
