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

package ai.konduit.serving.tensorrt;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.nd4j.shade.jackson.annotation.JsonCreator;
import org.nd4j.shade.jackson.annotation.JsonProperty;

import java.io.Serializable;

@Data
@Builder
@Accessors(fluent = true)
@NoArgsConstructor
public class NamedDimension implements Serializable {

    private String name;
    private long[] dimensions;

    @JsonCreator
    public NamedDimension(@JsonProperty("name") String name,
                          @JsonProperty("dimensions") long[] dimensions) {
        this.name = name;
        this.dimensions = dimensions;
    }
}
