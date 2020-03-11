package ai.konduit.serving.miscutils;

import ai.konduit.serving.output.types.NDArrayOutput;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.io.IOUtils;
import org.nd4j.jackson.objectmapper.holder.ObjectMapperHolder;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ExpectedAssertUtil {
    public static JsonArray ProbabilitiesAssert(String fileName) throws IOException {
        InputStream expectedIS = new FileInputStream(fileName);
        String encodedText = IOUtils.toString(expectedIS, StandardCharsets.UTF_8);
        JsonObject expectedObj = new JsonObject(encodedText);
        JsonArray expArr = expectedObj.getJsonObject("classification").getJsonArray("probabilities");
        return expArr;
    }
    public static INDArray NdArrayAssert(String fileName, String key) throws IOException {
        InputStream expectedIS = new FileInputStream(fileName);
        String encodedText = IOUtils.toString(expectedIS, StandardCharsets.UTF_8);
        JsonObject expectedObj = new JsonObject(encodedText);
        NDArrayOutput expND = ObjectMapperHolder.getJsonMapper().readValue(expectedObj.getJsonObject(key).toString(), NDArrayOutput.class);
        INDArray expectedArr = expND.getNdArray();
        return expectedArr;
    }
}
