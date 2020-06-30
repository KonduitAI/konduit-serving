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
package ai.konduit.serving.pipeline.impl.step.bbox.filter;

import ai.konduit.serving.pipeline.api.step.PipelineStep;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.Tolerate;

import java.util.Arrays;
import java.util.List;

@Data
@Accessors(fluent = true)
@AllArgsConstructor
@NoArgsConstructor
public class BoundingBoxFilterStep implements PipelineStep {

    public static final String DEFAULT_OUTPUT_NAME = "bounding_boxes";

    private boolean keepOtherValues = true;

    @Schema(description = "A list of class labels for which bounding boxes will be drawn")
    private List<String> classesToKeep;

    private String inputName = "input";

    private String outputName = DEFAULT_OUTPUT_NAME;

    @Tolerate
    public BoundingBoxFilterStep classesToKeep(String... classesToKeep) {
        return this.classesToKeep(Arrays.asList(classesToKeep));
    }

}
