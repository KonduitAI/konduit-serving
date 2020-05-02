package ai.konduit.serving.pipeline.impl.step.logging;

import ai.konduit.serving.pipeline.api.step.PipelineStep;
import lombok.Builder;
import lombok.Data;
import org.nd4j.shade.jackson.annotation.JsonProperty;
import org.slf4j.event.Level;

@Data
@Builder
public class LoggingPipelineStep implements PipelineStep {

    public enum Log { KEYS, KEYS_AND_VALUES }

    @Builder.Default
    private Level logLevel = Level.INFO;

    @Builder.Default
    private Log log = Log.KEYS;

    @Builder.Default
    public String keyFilterRegex = null;

    public LoggingPipelineStep(@JsonProperty("logLevel") Level logLevel, @JsonProperty("log") Log log, @JsonProperty("keyfilterRegex") String keyFilterRegex) {
        this.logLevel = logLevel;
        this.log = log;
        this.keyFilterRegex = keyFilterRegex;
    }

}
