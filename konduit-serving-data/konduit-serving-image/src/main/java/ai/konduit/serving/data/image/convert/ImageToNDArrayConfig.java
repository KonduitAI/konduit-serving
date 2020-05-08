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

package ai.konduit.serving.data.image.convert;

import ai.konduit.serving.data.image.convert.config.AspectRatioHandling;
import ai.konduit.serving.data.image.convert.config.NDChannels;
import ai.konduit.serving.data.image.convert.config.NDFormat;
import ai.konduit.serving.pipeline.api.data.NDArrayType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.nd4j.shade.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Configuration for converting {@link ai.konduit.serving.pipeline.api.data.Image}s to {@link ai.konduit.serving.pipeline.api.data.NDArray}s.
 * 
 *
 * @author Alex Black
 */
@Data
@Accessors(fluent = true)
@Builder
@AllArgsConstructor
public class ImageToNDArrayConfig {

    private Integer height;
    private Integer width;
    @Builder.Default
    private NDArrayType dataType = NDArrayType.FLOAT;
    @Builder.Default
    private boolean includeMinibatchDim = true;
    @Builder.Default
    private AspectRatioHandling aspectRatioHandling = AspectRatioHandling.CENTER_CROP;
    @Builder.Default
    private NDFormat format = NDFormat.CHANNELS_FIRST;
    @Builder.Default
    private NDChannels channels = NDChannels.RGB;

    public ImageToNDArrayConfig(){
        //Normally this would be unnecessary to set default values here - but @Builder.Default values are NOT treated as normal default values.
        //Without setting defaults here again like this, the fields would actually be null
        this.dataType = NDArrayType.FLOAT;
        this.includeMinibatchDim = true;
        this.aspectRatioHandling = AspectRatioHandling.CENTER_CROP;
        this.format = NDFormat.CHANNELS_FIRST;
        this.channels = NDChannels.RGB;
    }


}
