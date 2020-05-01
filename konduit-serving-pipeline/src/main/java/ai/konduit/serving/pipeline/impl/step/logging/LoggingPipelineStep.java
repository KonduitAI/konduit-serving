package ai.konduit.serving.pipeline.impl.step.logging;

import ai.konduit.serving.pipeline.api.step.PipelineStep;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.slf4j.event.Level;

@Data
@Builder
@AllArgsConstructor
public class LoggingPipelineStep implements PipelineStep {

    public enum Log { KEYS, KEYS_AND_VALUES }

    @Builder.Default
    private Level logLevel = Level.INFO;

    @Builder.Default
    private Log log = Log.KEYS;

    @Builder.Default
    public String keyFilterRegex = null;

}
