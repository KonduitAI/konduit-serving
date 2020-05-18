package ai.konduit.serving.pipeline.api.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TraceEvent {
    public static enum EventType {
        START, END;
    }
    private String name;
    private long timeStampStart;
    private long timeStampEnd;
    EventType type;
}
