package ai.konduit.serving.pipeline.api.context;

import lombok.*;

import java.nio.file.Path;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProfilerConfig {
    @Getter
    private Path outputFile;
    @Getter
    private long splitSize;
}
