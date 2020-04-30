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
package ai.konduit.serving.pipeline.api.serde;

import ai.konduit.serving.pipeline.api.TextConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * JsonSubType represents a human-readable JSON/YAML subtype mapping.<br>
 * Essentially, {@code JsonSubType("X", MyXClass, SomeConfigInterface)} means that the JSON represented by a wrapper
 * class "X" should be deserialized to the implementation class "MyXClass" which implements the configuration
 * interface "SomeConfigInterface"
 *
 * i.e., the folowing JSON:
 * <pre>
 *     "X" : {
 *         (some config fields here)
 *     }
 * </pre>
 * could be deserialized as follows: {@code SomeConfigInterface i = MyXClass.fromJson("...")}
 */
@AllArgsConstructor
@Data
@Builder
public class JsonSubType {

    /**
     * The name of the type as it appears in the JSON or YAML configuration
     */
    private String name;

    /**
     * The class that the "name" data should be deserialized to
     */
    private Class<?> subtype;

    /**
     * The interface that the subtype class implements - i.e., "subtype" implements "configInterface"
     */
    private Class<? extends TextConfig> configInterface;

}
