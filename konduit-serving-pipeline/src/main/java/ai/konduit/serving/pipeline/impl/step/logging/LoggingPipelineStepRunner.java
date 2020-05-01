package ai.konduit.serving.pipeline.impl.step.logging;

import ai.konduit.serving.pipeline.api.Data;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.event.Level;

import java.util.List;
import java.util.regex.Pattern;


@Slf4j
public class LoggingPipelineStepRunner implements PipelineStepRunner {

    private final LoggingPipelineStep step;
    private final Pattern pattern;

    public LoggingPipelineStepRunner(@NonNull LoggingPipelineStep step) {
        this.step = step;
        if(step.getKeyFilterRegex() != null){
            pattern = Pattern.compile(step.getKeyFilterRegex());
        } else {
            pattern = null;
        }
    }

    @Override
    public void close() {
        //No-op
    }

    @Override
    public PipelineStep getPipelineStep() {
        return step;
    }

    @Override
    public Data exec(Data data) {
        Level logLevel = step.getLogLevel();
        LoggingPipelineStep.Log toLog = step.getLog();
        boolean keysOnly = toLog == LoggingPipelineStep.Log.KEYS;

        //TODO does SLF4J have utility methods for this?
        boolean skip = logLevel == Level.ERROR && !log.isErrorEnabled() ||
                logLevel == Level.WARN && !log.isWarnEnabled() ||
                logLevel == Level.INFO && !log.isInfoEnabled() ||
                logLevel == Level.DEBUG && !log.isDebugEnabled() ||
                logLevel == Level.TRACE && !log.isTraceEnabled();

        if(skip)
            return data;

        List<String> keys = data.keys();

        StringBuilder sb = new StringBuilder();
        for(String s : keys){
            if(pattern != null && pattern.matcher(s).matches())
                continue;

            if(keysOnly){
                if(sb.length() > 0){
                    sb.append(", ");
                }
                sb.append("\"").append(s).append("\"");
            } else {
                if(sb.length() > 0)
                    sb.append("\n");
                sb.append("\"").append(s).append("\": ").append(data.get(s));
            }
        }

        switch (logLevel){
            case ERROR:
                log.error(sb.toString());
                break;
            case WARN:
                log.warn(sb.toString());
                break;
            case INFO:
                log.info(sb.toString());
                break;
            case DEBUG:
                log.debug(sb.toString());
                break;
            case TRACE:
                log.trace(sb.toString());
                break;
        }


        return data;
    }
}
