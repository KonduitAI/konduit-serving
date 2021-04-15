package ai.konduit.serving;

import ai.konduit.serving.client.java.InferenceApi;
import ai.konduit.serving.client.java.invoker.ApiClient;
import ai.konduit.serving.client.java.invoker.ApiException;
import ai.konduit.serving.client.java.models.InferenceConfiguration;
import lombok.*;
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
    private InferenceConfiguration.ProtocolEnum protocol;

    @Getter(value=AccessLevel.PRIVATE)
    @Setter(value=AccessLevel.PRIVATE)
    private ApiClient apiClient;

    @Getter(value=AccessLevel.PRIVATE)
    @Setter(value=AccessLevel.PRIVATE)
    private InferenceApi inferenceApi;

    public Map<String, Object> predict(Map<String, Object> input) throws ApiException {
        if(apiClient== null) {
            apiClient = new ApiClient()
                    .setBasePath(String.format("http%s://%s:%s",
                            useSsl ? "s": "",
                            host,
                            port));
        }

        if(inferenceApi == null) {
            inferenceApi = new InferenceApi(apiClient);
        }

        return inferenceApi.predict(input);
    }

    public static Map<String, String> getImage(String imageFilePath) {
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
            log.error("Error converting image file to image object", exception);
            return null;
        }
    }
}
