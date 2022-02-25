/* ******************************************************************************
 * Copyright (c) 2022 Konduit K.K.
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

import java.util.List;
import java.util.ServiceLoader;

/**
 * JsonSubTypesMapping is an interface implemented by a class in each module.
 *
 * Used via {@link ServiceLoader} or OSGi to determine at what objects JSON can be deserialized to, based on the currently
 * available Konduit Serving module.
 *
 * See {@link JsonSubType} for more details
 *
 */
public interface JsonSubTypesMapping {

    /**
     * @return A list of JsonSubType objects that represent the classes that the JSON data can be deserialized to using this module
     */
    List<JsonSubType> getSubTypesMapping();

}
