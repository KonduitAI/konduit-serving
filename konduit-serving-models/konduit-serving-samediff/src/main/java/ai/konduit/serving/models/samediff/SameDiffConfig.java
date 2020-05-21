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
package ai.konduit.serving.models.samediff;

import ai.konduit.serving.pipeline.api.Configuration;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class SameDiffConfig implements Configuration {


    @Override
    public Set<String> keys() {
        return Collections.emptySet();
    }

    @Override
    public Map<String, Object> asMap() {
        return Collections.emptyMap();
    }

    @Override
    public Object get(String key) {
        throw new IllegalStateException("No key \"" + key + "\" exists");
    }

    @Override
    public Object getOrDefault(String key, Object defaultValue) {
        return defaultValue;
    }
}
