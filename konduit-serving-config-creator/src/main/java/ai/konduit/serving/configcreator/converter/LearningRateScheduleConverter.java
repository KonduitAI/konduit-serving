/*
 *  ******************************************************************************
 *  *
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Apache License, Version 2.0 which is available at
 *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  *  See the NOTICE file distributed with this work for additional
 *  *  information regarding copyright ownership.
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  * License for the specific language governing permissions and limitations
 *  * under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *  *****************************************************************************
 */
package ai.konduit.serving.configcreator.converter;

import ai.konduit.serving.configcreator.StringSplitter;
import org.nd4j.linalg.schedule.*;
import picocli.CommandLine;

import java.util.Collections;
import java.util.Map;

public class LearningRateScheduleConverter implements CommandLine.ITypeConverter<ISchedule> {
    public final static String DELIMITER = ",";
    public final static String SCHEDULE_TYPE_KEY = "type";

    public enum Scheduletype {
        CYCLE,
        EXPONENTIAL,
        FIXED,
        INVERSE,
        MAP,
        POLY,
        RAMP,
        SIGMOID,
        STEP
    }


    @Override
    public ISchedule convert(String value) throws Exception {
        StringSplitter stringSplitter = new StringSplitter(DELIMITER);
        Map<String,String> result = stringSplitter.splitResult(value);
        String type = result.get(SCHEDULE_TYPE_KEY);
        result.remove(SCHEDULE_TYPE_KEY);
        return instanceForType(type,result);
    }

    private ISchedule instanceForType(String type,Map<String,String> configurationValues) {
        switch(Scheduletype.valueOf(type.toUpperCase())) {
            case MAP:
                return new MapSchedule(ScheduleType.EPOCH, Collections.emptyMap());
            case POLY:
            return new PolySchedule(ScheduleType.EPOCH,getValue(configurationValues,"initialValue"),getValue(configurationValues,"power"),1);
            case STEP:
                return new StepSchedule(ScheduleType.EPOCH,getValue(configurationValues,"initialValue"),getValue(configurationValues,"decayRate"),getValue(configurationValues,"step"));
            case CYCLE:
                return new CycleSchedule(ScheduleType.EPOCH,getValue(configurationValues,"initialLearningRate"),getValue(configurationValues,"maxLearningRate"),getIntValue(configurationValues,"cycleLength"),getIntValue(configurationValues,"annealingLength"),getValue(configurationValues,"annealingDecay"));
            case FIXED:
                return new FixedSchedule(getValue(configurationValues,"value"));
            case INVERSE:
                return new InverseSchedule(ScheduleType.EPOCH,getValue(configurationValues,"initialValue"),getValue(configurationValues,"gamma"),getValue(configurationValues,"power"));
            case SIGMOID:
                return new SigmoidSchedule(ScheduleType.EPOCH,getValue(configurationValues,"initialValue"),getValue(configurationValues,"gamma"),getIntValue(configurationValues,"stepSize"));
            case EXPONENTIAL:
                return new ExponentialSchedule(ScheduleType.EPOCH,getValue(configurationValues,"initialValue"),getValue(configurationValues,"gamma"));
            default:
                throw new IllegalArgumentException("Unable to create learning rate schedule of type " + type);
        }

    }


    private int getIntValue(Map<String,String> getFrom,String key) {
        if(!getFrom.containsKey(key)) {
            throw new IllegalArgumentException("Unable to find configuration value " + key);
        }
        return Integer.parseInt(getFrom.get(key));
    }

    private double getValue(Map<String,String> getFrom,String key) {
        if(!getFrom.containsKey(key)) {
            throw new IllegalArgumentException("Unable to find configuration value " + key);
        }
        return Double.parseDouble(getFrom.get(key));
    }

}
