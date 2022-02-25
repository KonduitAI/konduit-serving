/*
 *  ******************************************************************************
 *  * Copyright (c) 2022 Konduit K.K.
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

package ai.konduit.serving.pipeline.impl.pipeline.graph;

import ai.konduit.serving.pipeline.api.TextConfig;
import ai.konduit.serving.pipeline.api.data.Data;
import org.nd4j.shade.jackson.annotation.JsonTypeInfo;

import static org.nd4j.shade.jackson.annotation.JsonTypeInfo.Id.NAME;

/**
 * SwitchFn is used with {@link SwitchStep} in order to determine which of {@code numOutputs()} outputs the input
 * Data instance will be forwarded on to.<br>
 * Note that the number of possible outputs is fixed at graph construction time
 *
 * @author Alex Black
 */
@JsonTypeInfo(use = NAME, property = "@type")
public interface SwitchFn extends TextConfig {

    /**
     * @return Number of outputs
     */
    int numOutputs();

    /**
     * Select the number of the output that the specified Data instance should be forwarded on to
     *
     * @param data Input data
     * @return Index of the output to forward this Data on to. Must be in range 0 to numOutputs()-1 inclusive
     */
    int selectOutput(Data data);

}
