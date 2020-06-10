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
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import org.nd4j.shade.jackson.annotation.JsonProperty;

@Data
@Builder
@Accessors(fluent = true)
@JsonName("SHOW_IMAGE")
public class ShowImagePipelineStep implements PipelineStep {

    @Builder.Default
    private String imageName = "image";
    @Builder.Default
    private String displayName = "Image";
    @Builder.Default
    private Integer width = 1280;
    @Builder.Default
    private Integer height = 720;
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
