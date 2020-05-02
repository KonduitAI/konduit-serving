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
package ai.konduit.serving.pipeline.impl.data.wrappers;

import ai.konduit.serving.pipeline.impl.data.Value;
import ai.konduit.serving.pipeline.impl.data.ValueType;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class BooleanValue implements Value<Boolean> {

    private boolean value;

    @Override
    public ValueType type() {
        return ValueType.BOOLEAN;
    }

    @Override
    public Boolean get() {
        return value;
    }

    @Override
    public void set(Boolean value) {
        this.value = value;
    }
}
