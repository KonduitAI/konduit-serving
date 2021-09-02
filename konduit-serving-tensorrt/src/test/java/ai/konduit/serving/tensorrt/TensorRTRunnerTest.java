package ai.konduit.serving.tensorrt;

import ai.konduit.serving.models.onnx.step.ONNXStep;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipelineExecutor;
import org.junit.Test;
import org.nd4j.common.resources.Resources;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class TensorRTRunnerTest {

    @Test
    public void testRunner() {
        File f = Resources.asFile("add.onnx");
        INDArray arr = Nd4j.ones(1,1);
        Data data = Data.empty();
        data.put("x",NDArray.create(arr));
        data.put("y",NDArray.create(arr.dup()));

        ONNXStep onnxStep = new ONNXStep()
                .inputNames("x","y")
                .outputNames("z")
                .modelUri(f.getAbsolutePath());
        SequencePipeline onnxRunner = SequencePipeline.builder()
                .add(onnxStep)
                .build();

        PipelineExecutor onnxRunnerExecutor = onnxRunner.executor();

        TensorRTStep tensorRTStep = new TensorRTStep()
                .batchSize(1)
                .inputNames(Arrays.asList("x","y"))
                .outputNames(Arrays.asList("z"))
                .modelUri(f.getAbsolutePath())
                .useFp16(false)
                .maxWorkspaceSize(16 << 20)
                .outputDimensions(new NamedDimensionList(Arrays.asList(NamedDimension.builder().name("z")
                        .dimensions(new long[]{1}).build())));

        SequencePipeline tensorrtRunner = SequencePipeline.builder()
                .add(tensorRTStep)
                .build();

        PipelineExecutor tensorrtExecutor = tensorrtRunner.executor();
        Data exec = onnxRunnerExecutor.exec(data);
        Data exec2 = tensorrtExecutor.exec(data);
        assertEquals(exec.getNDArray("z").getAs(INDArray.class).reshape(1,1),exec2.getNDArray("z").getAs(INDArray.class).reshape(1,1));

    }

}
