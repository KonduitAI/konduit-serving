package ai.konduit.serving.miscutils;

import ai.konduit.serving.output.types.NDArrayOutput;
import ai.konduit.serving.util.ObjectMappers;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.io.FileUtils;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ExpectedAssertUtil {
    public static JsonArray probabilitiesToJsonArray(String fileName) throws IOException {
        String jsonString = FileUtils.readFileToString(new File(fileName), StandardCharsets.UTF_8);
        JsonObject jsonObject = new JsonObject(jsonString);
        return jsonObject.getJsonObject("classification").getJsonArray("probabilities");
    }
    public static INDArray fileAndKeyToNDArrayOutput(String fileName, String key) throws IOException {
        String jsonString = FileUtils.readFileToString(new File(fileName), StandardCharsets.UTF_8);
        JsonObject jsonObject = new JsonObject(jsonString);
        return ObjectMappers.fromJson(jsonObject.getJsonObject(key).toString(), NDArrayOutput.class)
                .getNdArray();
    }
}
