package ai.konduit.serving.models.onnx.utils;

import org.bytedeco.onnxruntime.MemoryInfo;
import org.bytedeco.onnxruntime.Value;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import static org.bytedeco.onnxruntime.global.onnxruntime.OrtArenaAllocator;
import static org.bytedeco.onnxruntime.global.onnxruntime.OrtMemTypeDefault;
import static org.junit.Assert.assertEquals;

public class ONNXUtilsTests {


    @Test
    public void testNdArray() {
        INDArray arr = Nd4j.create(2,2);
        MemoryInfo memoryInfo = MemoryInfo.CreateCpu(OrtArenaAllocator, OrtMemTypeDefault);
        Value value = ONNXUtils.getTensor(arr, memoryInfo);
        INDArray convertedArr = ONNXUtils.getArray(value);
        assertEquals(arr,convertedArr);
    }
}
