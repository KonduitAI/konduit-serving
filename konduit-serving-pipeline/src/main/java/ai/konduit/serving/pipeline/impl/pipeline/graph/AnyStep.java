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

package ai.konduit.serving.pipeline.impl.pipeline.graph;

import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * AnyStep forwards the first available input to the output.
 * Usually used in conjunction with a Switch step - i.e., input -> Switch -> (left branch, right branch) -> Any<br>
 * If more than one of the inputs is available, the output is undefined (could be any of the inputs)
 *
 * @author Alex Black
 */
@EqualsAndHashCode(callSuper = true)
public class AnyStep extends BaseMergeStep {

    public AnyStep(GraphBuilder b, List<String> steps, String name) {
        super(b, steps, name);
    }

    @Override
    public String toString() {
        return "Any(\"" + String.join("\",\"", inputs()) + "\")";
    }

}
