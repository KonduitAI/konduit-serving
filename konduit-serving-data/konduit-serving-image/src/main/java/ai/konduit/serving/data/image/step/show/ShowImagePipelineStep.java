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

package ai.konduit.serving.data.image.step.show;

import ai.konduit.serving.annotation.json.JsonName;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import org.nd4j.shade.jackson.annotation.JsonProperty;

@Data
@Builder
@Accessors(fluent = true)
@JsonName("SHOW_IMAGE")
@Schema(description = "A pipeline step that configures how to show/render an image from a previous step.")
public class ShowImagePipelineStep implements PipelineStep {

    @Builder.Default
    @Schema(description = "Name of the incoming input image key.",
            defaultValue = "image")
    private String imageName = "image";

    @Builder.Default
    @Schema(description = "Image display name.",
            defaultValue = "Image")
    private String displayName = "Image";

    @Builder.Default
    @Schema(description = "Height of the image.",
            defaultValue = "1280")
    private Integer width = 1280;

    @Builder.Default
    @Schema(description = "Width of the image.",
            defaultValue = "720")
    private Integer height = 720;

    @Schema(description = "Allow multiple images to be shown.")
    private boolean allowMultiple = false;

    public ShowImagePipelineStep(@JsonProperty("imageName") String imageName, @JsonProperty("displayName") String displayName,
                                 @JsonProperty("width") Integer width, @JsonProperty("height") Integer height,
                                 @JsonProperty("allowMultiple") boolean allowMultiple){
        this.imageName = imageName;
        this.displayName = displayName;
        this.width = width;
        this.height = height;
        this.allowMultiple = allowMultiple;
    }

    public ShowImagePipelineStep(){
        //Normally this would be unnecessary to set default values here - but @Builder.Default values are NOT treated as normal default values.
        //Without setting defaults here again like this, the fields would actually be null
        this.imageName = "image";
        this.displayName = displayName();
        this.width = 1280;
        this.height = 720;
    }


}
