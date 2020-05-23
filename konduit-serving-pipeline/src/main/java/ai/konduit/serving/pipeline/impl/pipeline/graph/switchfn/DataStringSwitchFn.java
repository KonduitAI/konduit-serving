/*
 *  ******************************************************************************
 *  * Copyright (c) 2020 Konduit K.K.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Apache License, Version 2.0 which is available at
 *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  * License for the specific language governing permissions and limitations
 *  * under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *  *****************************************************************************
 */

package ai.konduit.serving.pipeline.impl.pipeline.graph.switchfn;

import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.ValueType;
import ai.konduit.serving.pipeline.impl.pipeline.graph.SwitchFn;
import lombok.experimental.Accessors;
import org.nd4j.common.base.Preconditions;

import java.util.Map;

@lombok.Data
@Accessors(fluent = true)
public class DataStringSwitchFn implements SwitchFn {

    private final int numOutputs;
    private final String fieldName;
    private final Map<String,Integer> map;

    public DataStringSwitchFn(int numOutputs, String fieldName, Map<String,Integer> map){
        this.numOutputs = numOutputs;
        this.fieldName = fieldName;
        this.map = map;
    }

    @Override
    public int numOutputs() {
        return numOutputs;
    }

    @Override
    public int selectOutput(Data data) {
        Preconditions.checkState(data.has(fieldName), "Input data does not have a String field of name \"%s\"", fieldName);
        Preconditions.checkState(data.type(fieldName) == ValueType.STRING, "Input data field \"%s\" has type \"%s\", " +
                "must be String", fieldName, data.type(fieldName));
        String s = data.getString(fieldName);
        Preconditions.checkState(map.containsKey(s), "String->Integer map does not contain key for value \"%s\": " +
                "Map has only keys %s", map.keySet());

        return map.get(s);
    }
}
