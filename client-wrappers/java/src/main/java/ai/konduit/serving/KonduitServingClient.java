package ai.konduit.serving;

import ai.konduit.serving.client.java.InferenceApi;
import ai.konduit.serving.client.java.invoker.ApiClient;
import ai.konduit.serving.client.java.invoker.ApiException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * A convenient client that connects with a Konduit-Serving server for predictions
 *
 */
@Slf4j
@Data
@Accessors(fluent = true)
@AllArgsConstructor
@Builder
public class KonduitServingClient
{
    private String serverId;
    private boolean useSsl;
    private String host;
    private int port;

    public Map<String, Object> predict(Map<String, Object> input) throws ApiException {
        return new InferenceApi(
                new ApiClient()
                        .setBasePath(String.format("http%s://%s:%s",
                                useSsl ? "s": "",
                                host,
                                port))
        ).predict(input);
    }

    public Map<String, String> getImage(String imageFilePath) {
        File imageFile = new File(imageFilePath);

        try {
            Map<String, String> imageMap = new HashMap<>();
            imageMap.put("@ImageData",
                    new String(Base64.getEncoder().encode(
                            FileUtils.readFileToByteArray(imageFile)
                    ),
                    StandardCharsets.UTF_8));
            imageMap.put("@ImageFormat", "PNG");
            return imageMap;
        } catch (IOException exception) {
            exception.printStackTrace();
            return null;
        }
    }
}
