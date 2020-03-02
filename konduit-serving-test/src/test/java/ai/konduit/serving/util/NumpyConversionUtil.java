package ai.konduit.serving.util;

import org.apache.commons.io.FileUtils;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.serde.binary.BinarySerde;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

public class NumpyConversionUtil {
    public static INDArray convertToNd4J(String response) throws IOException {
        INDArray outputArray;
        File outputImagePath = new File(
                "src/main/resources/data/test-nd4j-output.npy");
        FileUtils.writeStringToFile(outputImagePath, response, Charset.defaultCharset());
        outputArray = BinarySerde.readFromDisk(outputImagePath);
        //outputArray = Nd4j.createFromNpyFile(outputImagePath);

        return outputArray;
    }
}
