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

import lombok.*;
import org.nd4j.shade.jackson.annotation.JsonIgnoreProperties;

@AllArgsConstructor
@Data
@NoArgsConstructor
@JsonIgnoreProperties("builder")
@EqualsAndHashCode(exclude = {"builder"})
public abstract class BaseGraphStep implements GraphStep {
    protected GraphBuilder builder;
    protected String name;


    @Override
    public String name() {
        return name;
    }

    @Override
    public void name(@NonNull String name){
        this.name = name;
    }

    @Override
    public GraphBuilder builder() {
        return builder;
    }
}
