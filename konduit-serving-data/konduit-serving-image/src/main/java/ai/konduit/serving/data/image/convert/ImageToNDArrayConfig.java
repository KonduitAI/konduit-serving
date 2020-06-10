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
import ai.konduit.serving.data.image.convert.config.ImageNormalization;
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
 *     <li><b>height</b>: Output NDArray image height: leave null to convert to the same size as the input. Default: null</li>
 *     <li><b>width</b>: Output NDArray image height: leave null to convert to the same size as the input. Default: null</li>
 *     <li><b>datatype</b>: {@link NDArrayType} (data type) of the output array</li>
 *     <li><b>includeMinibatchDim</b>: If true: the output array will be rank 4 with shape [1, c, h, w] or [1, h, w, c].
 *         If false: return rank 3 array with shape [c, h, w] or [h, w, c]. Default: true</li>
 *     <li><b>aspectRatioHandling</b>: How should input images with different aspect ratio to the output height/width be handled? Default: CENTER_CROP</li>
 *     <ul>
 *         <li><b>CENTER_CROP</b>: Crop the larger dimension down to the correct aspect ratio (and then resize if necessary).</li>
 *         <li><b>PAD</b>: Zero pad the smaller dimension to make the aspect ratio match the output (and then resize if necessary)</li>
 *         <li><b>STRETCH</b>: Simply resize the image to the required aspect ratio, distorting the image if necessary</li>
 *     </ul>
 *     <li><b>format</b>: CHANNELS_FIRST (output shape: [1, c, h, w] or [c, h, w]) or CHANNELS_LAST (output shape: [1, h, w, c] or [h, w, c])</li>
 *     <li><b>channels</b>: The layout for the returned array. Note input images will be converted if necessary. Default: RGB</li>
 *     <ul>
 *         <li><b>RGB</b>: 3 channels, ordered according to: red, green, blue - most common for TensorFlow, Keras, and some other libraries</li>
 *         <li><b>BGR</b>: 3 channels, ordered according to: blue, green, red - the default for OpenCV, JavaCV, DL4J</li>
 *         <li><b>RGBA</b>: 4 channels, ordered according to: red, green, blue, alpha</li>
 *         <li><b>BGRA</b>: 4 channels, ordered according to: blue, green, red, alpha</li>
 *         <li><b>GRAYSCALE</b>: 1 channel - grayscale</li>
 *     </ul>
 *     <li><b>normalization</b>: How the image should be normalized. See {@link ImageNormalization} - support scaling ([0,1] range),
 *         subtracting mean (out = (in-mean)), standardization (out = (in-mean)/stdev), inception ([-1, 1] range) and
 *         VGG mean subtraction (fixed out = in - meanRgb, where meanRgb is hardcoded to [123.68, 116.779, 103.939].
 *         Default: simple scale normalization ([0, 1] range).
 *         Note: If image normalization in null, or ImageNormalization.type == Type.NONE, no normalization is applied.
 *     </li>
 *     <li><b>listHandling</b>: Only applies in situations such as {@link ai.konduit.serving.data.image.step.ndarray.ImageToNDArrayStep},
 *         and only when {@code List<Image>} is passed in instead of {@code Image}. This setting determines what the output
 *         should be. NONE: Error for {@code List<Image>} input (only single Images are allowed). BATCH: a single output
 *         NDArray is returned, with the images batched along dimension 0. LIST_OUT: A {@code List<NDArray>} is returned
 *         instead of a single {@code NDArray} - one entry for each entry in the input {@code List<Image>}.
 *     </li>
 * </ul>
 *
 * @author Alex Black
 */
@Data
@Accessors(fluent = true)
@Builder
@AllArgsConstructor
public class ImageToNDArrayConfig {
    /**
     * See {@link ImageToNDArrayConfig} - listHandling field
     */
    public static enum ListHandling {NONE, BATCH, LIST_OUT}


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
    @Builder.Default
    private ImageNormalization normalization = new ImageNormalization(ImageNormalization.Type.SCALE);
    @Builder.Default
    private ListHandling listHandling = ListHandling.NONE;

    public ImageToNDArrayConfig() {
        //Normally this would be unnecessary to set default values here - but @Builder.Default values are NOT treated as normal default values.
        //Without setting defaults here again like this, the fields would actually be null
        this.dataType = NDArrayType.FLOAT;
        this.includeMinibatchDim = true;
        this.aspectRatioHandling = AspectRatioHandling.CENTER_CROP;
        this.format = NDFormat.CHANNELS_FIRST;
        this.channelLayout = NDChannelLayout.RGB;
        this.normalization = new ImageNormalization(ImageNormalization.Type.SCALE);
    }


}
