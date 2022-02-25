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

package ai.konduit.serving.data.image.convert;

import ai.konduit.serving.data.image.convert.config.AspectRatioHandling;
import ai.konduit.serving.data.image.convert.config.ImageNormalization;
import ai.konduit.serving.data.image.convert.config.NDChannelLayout;
import ai.konduit.serving.data.image.convert.config.NDFormat;
import ai.konduit.serving.pipeline.api.data.NDArrayType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.nd4j.shade.jackson.annotation.JsonProperty;

/**
 * Configuration for converting {@link ai.konduit.serving.pipeline.api.data.Image}s to {@link ai.konduit.serving.pipeline.api.data.NDArray}s.
 * <p>
 * The following can be configured:<br>
 * <ul>
 *     <li><b>height</b>: Output NDArray image height: leave null to convert to the same size as the input. Default: null</li>
 *     <li><b>width</b>: Output NDArray image width: leave null to convert to the same size as the input. Default: null</li>
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
 *         instead of a single {@code NDArray} - one entry for each entry in the input {@code List<Image>}. FIRST:
 *         the first bounding box only is returned as as single {@code NDArray} - the remainder are discarded/ignored.
 *     </li>
 * </ul>
 *
 * @author Alex Black
 */
@Data
@Accessors(fluent = true)
@NoArgsConstructor
@Schema(description = "Configuration for converting an image into an n-dimensional array.")
public class ImageToNDArrayConfig {
    /**
     * See {@link ImageToNDArrayConfig} - listHandling field
     */
    @Schema(description = "An enum to specify how to handle a list of input images. <br><br>" +
            "NONE -> No list handling i.e. Simply convert an image to n-dimensional array (assuming the input is not a list of images), <br>" +
            "BATCH -> Convert a list of images to a batch of n-dimensional array (whose first axis will be first image index), <br>" +
            "LIST_OUT -> Convert a list of images to a list of n-dimensional array, <br>" +
            "FIRST -> Convert the first image in the list of images to an n-dimensional array.")
    public enum ListHandling {NONE, BATCH, LIST_OUT, FIRST}


    @Schema(description = "Output array image height. Leave null to convert to the same size as the image height.")
    private Integer height;

    @Schema(description = "Output array image width. Leave null to convert to the same size as the image width.")
    private Integer width;

    
    @Schema(description = "Data type of the n-dimensional array.", defaultValue = "FLOAT")
    private NDArrayType dataType = NDArrayType.FLOAT;

    
    @Schema(description = "If true, the output array will contain an extra dimension for the minibatch number. This " +
            "will look like (1, Channels, Height, Width) instead of (Channels, Height, Width) for format == CHANNELS_FIRST " +
            "or (1, Height, Width, Channels) instead of (Height, Width, Channels) for format == CHANNELS_LAST.",
            defaultValue = "true")
    private boolean includeMinibatchDim = true;
    

    @Schema(description = "An enum to Handle the situation where the input image and output NDArray have different aspect ratios. <br><br>" +
            "CENTER_CROP (crop larger dimension then resize if necessary), <br>" +
            "PAD (pad smaller dimension then resize if necessary), <br>" +
            "STRETCH (simply resize, distorting if necessary).",
            defaultValue = "CENTER_CROP")
    private AspectRatioHandling aspectRatioHandling = AspectRatioHandling.CENTER_CROP;

    
    @Schema(description = "The format to be used when converting an Image to an NDArray.",
            defaultValue = "CHANNELS_FIRST")
    private NDFormat format = NDFormat.CHANNELS_FIRST;

    
    @Schema(description = "An enum that represents the type (and order) of the color channels for an image after it has " +
            "been converted to an NDArray. For example, RGB vs. BGR etc",
            defaultValue = "RGB")
    private NDChannelLayout channelLayout = NDChannelLayout.RGB;

    
    @Schema(description = "Configuration that specifies the normalization type of an image array values.")
    private ImageNormalization normalization = new ImageNormalization(ImageNormalization.Type.SCALE);

    
    @Schema(description = "An enum to specify how to handle a list of input images.",
            defaultValue = "NONE")
    private ListHandling listHandling = ListHandling.NONE;


    public ImageToNDArrayConfig(@JsonProperty("height") Integer height, @JsonProperty("width") Integer width, @JsonProperty("dataType") NDArrayType dataType,
                                @JsonProperty("includeMinibatchDim") boolean includeMinibatchDim, @JsonProperty("aspectRatioHandling") AspectRatioHandling aspectRatioHandling,
                                @JsonProperty("format") NDFormat format, @JsonProperty("channelLayout") NDChannelLayout channelLayout,
                                @JsonProperty("normalization") ImageNormalization normalization, @JsonProperty("listHandling") ListHandling listHandling){
        this.height = height;
        this.width = width;
        this.dataType = dataType;
        this.includeMinibatchDim = includeMinibatchDim;
        this.aspectRatioHandling = aspectRatioHandling;
        this.format = format;
        this.channelLayout = channelLayout;
        this.normalization = normalization;
        this.listHandling = listHandling;
    }


}
