/*
 *
 *  * ******************************************************************************
 *  *  * Copyright (c) 2015-2019 Skymind Inc.
 *  *  * Copyright (c) 2019 Konduit AI.
 *  *  *
 *  *  * This program and the accompanying materials are made available under the
 *  *  * terms of the Apache License, Version 2.0 which is available at
 *  *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  *  * License for the specific language governing permissions and limitations
 *  *  * under the License.
 *  *  *
 *  *  * SPDX-License-Identifier: Apache-2.0
 *  *  *****************************************************************************
 *
 *
 */
package ai.konduit.serving.model;

import ai.konduit.serving.annotation.json.JsonName;
import ai.konduit.serving.pipeline.api.TextConfig;
import ai.konduit.serving.pipeline.api.data.ValueType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(fluent = true)
@NoArgsConstructor
@Schema(description = "Python base")
public class PythonIO implements Serializable, TextConfig {

    private String name,pythonType;
    //relevant for lists, byte de serialization
    private ValueType secondaryType = ValueType.NONE;
    private ValueType type = ValueType.NONE;

    @Builder
    public PythonIO(String name, String pythonType, ValueType secondaryType, ValueType type) {
        this.name = name;
        this.pythonType = pythonType;
        if(secondaryType == null)
            secondaryType = ValueType.NONE;
        if(type == null)
            type = ValueType.NONE;
        this.secondaryType = secondaryType;
        this.type = type;
        validate();
    }


    /**
     * Returns true if this io is a list
     * with a secondary type defined
     * @return
     */
    public boolean isDictWithType() {
        if(type == null)
            return false;
        return type == ValueType.BOUNDING_BOX
                || type == ValueType.POINT
                && secondaryType != ValueType.NONE;
    }

    /**
     * Returns true if this is a list type
     * with no secondary type defined
     * @return
     */
    public boolean isDictWithUndefinedType() {
        return !isDictWithType();
    }



    /**
     * Returns true if this io is a list
     * with a secondary type defined
     * @return
     */
    public boolean isListWithType() {
        if(type == null)
            return false;
        return type == ValueType.LIST && secondaryType != ValueType.NONE;
    }

    /**
     * Returns true if this is a list type
     * with no secondary type defined
     * @return
     */
    public boolean isListWithUndefinedType() {
        if(type == null)
            return false;
        return !isListWithType();
    }


    private void validate() {

    }

}
