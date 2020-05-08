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
import ai.konduit.serving.data.image.convert.config.NDChannelLayout;
import ai.konduit.serving.data.image.convert.config.NDFormat;
import ai.konduit.serving.pipeline.api.data.NDArrayType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Configuration for converting {@link ai.konduit.serving.pipeline.api.data.Image}s to {@link ai.konduit.serving.pipeline.api.data.NDArray}s.
 * <p>
 * The following can be configured:<br>
 * <ul>
 *     <li>height: Output NDArray image height: leave null to convert to the same size as the input. Default: null</li>
 *     <li>width: Output NDArray image height: leave null to convert to the same size as the input. Default: null</li>
 *     <li>datatype: {@link NDArrayType} (data type) of the output array</li>
 *     <li>includeMinibatchDim: If true: the output array will be rank 4 with shape [1, c, h, w] or [1, h, w, c].
 *         If false: return rank 3 array with shape [c, h, w] or [h, w, c]. Default: true</li>
 *     <li>aspectRatioHandling: How should input images with different aspect ratio to the output height/width be handled? Default: CENTER_CROP</li>
 *     <ul>
 *         <li>CENTER_CROP: Crop the larger dimension down to the correct aspect ratio (and then resize if necessary).</li>
 *         <li>PAD: Zero pad the smaller dimension to make the aspect ratio match the output (and then resize if necessary)</li>
 *         <li>STRETCH: Simply resize the image to the required aspect ratio, distorting the image if necessary</li>
 *     </ul>
 *     <li>format: CHANNELS_FIRST (output shape: [1, c, h, w] or [c, h, w]) or CHANNELS_LAST (output shape: [1, h, w, c] or [h, w, c])</li>
 *     <li>channels: The layout for the returned array. Note input images will be converted if necessary. Default: RGB</li>
 *     <ul>
 *         <li>RGB: 3 channels, ordered according to: red, green, blue - most common for TensorFlow, Keras, and some other libraries</li>
 *         <li>BGR: 3 channels, ordered according to: blue, green, red - the default for OpenCV, JavaCV, DL4J</li>
 *         <li>RGBA: 4 channels, ordered according to: red, green, blue, alpha</li>
 *         <li>BGRA: 4 channels, ordered according to: blue, green, red, alpha</li>
 *         <li>GRAYSCALE: 1 channel - grayscale</li>
 *     </ul>
 * </ul>
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
    private NDChannelLayout channelLayout = NDChannelLayout.RGB;

    public ImageToNDArrayConfig() {
        //Normally this would be unnecessary to set default values here - but @Builder.Default values are NOT treated as normal default values.
        //Without setting defaults here again like this, the fields would actually be null
        this.dataType = NDArrayType.FLOAT;
        this.includeMinibatchDim = true;
        this.aspectRatioHandling = AspectRatioHandling.CENTER_CROP;
        this.format = NDFormat.CHANNELS_FIRST;
        this.channelLayout = NDChannelLayout.RGB;
    }


}
