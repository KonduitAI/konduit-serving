/* ******************************************************************************
 * Copyright (c) 2020 Konduit K.K.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/
package ai.konduit.serving.pipeline.impl.step.logging;

import ai.konduit.serving.pipeline.api.data.Data;
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

        String s = sb.toString();

        //TODO Is there a cleaner way to do this?
        switch (logLevel){
            case ERROR:
                log.error(s);
                break;
            case WARN:
                log.warn(s);
                break;
            case INFO:
                log.info(s);
                break;
            case DEBUG:
                log.debug(s);
                break;
            case TRACE:
                log.trace(s);
                break;
        }


        return data;
    }
}
