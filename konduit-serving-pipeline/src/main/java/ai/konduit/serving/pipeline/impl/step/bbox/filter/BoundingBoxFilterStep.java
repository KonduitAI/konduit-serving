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
package ai.konduit.serving.pipeline.impl.step.bbox.filter;

import ai.konduit.serving.annotation.json.JsonName;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
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
@JsonName("BOUNDING_BOX_FILTER")
public class BoundingBoxFilterStep implements PipelineStep {

    public static final String DEFAULT_OUTPUT_NAME = "bounding_boxes";

    @Schema(description = "If true, other data key and values from the previous step are kept and passed on to the next step as well.",
            defaultValue = "true")
    private boolean keepOtherValues = true;

    @Schema(description = "A list of class labels for which bounding boxes will be drawn")
    protected List<String> classesToKeep;

    @Schema(description = "Input name where the all bounding box are be contained in", defaultValue = "input")
    protected String inputName = "input";

    @Schema(description = "Output key name where the bounding box will be contained in.",
            defaultValue = DEFAULT_OUTPUT_NAME)
    protected String outputName = DEFAULT_OUTPUT_NAME;

    @Tolerate
    public BoundingBoxFilterStep classesToKeep(String... classesToKeep) {
        return this.classesToKeep(Arrays.asList(classesToKeep));
    }

}
