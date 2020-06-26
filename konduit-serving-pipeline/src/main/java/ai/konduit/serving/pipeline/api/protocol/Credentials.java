package ai.konduit.serving.pipeline.api.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class Credentials {
    private String accessKey;
    private String secretKey;
}
