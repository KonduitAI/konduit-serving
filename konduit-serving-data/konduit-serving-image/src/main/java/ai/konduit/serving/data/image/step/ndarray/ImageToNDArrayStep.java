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

package ai.konduit.serving.data.image.step.ndarray;

import ai.konduit.serving.data.image.convert.ImageToNDArrayConfig;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import lombok.Builder;
import lombok.Data;
import org.nd4j.shade.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * A PipelineStep for converting {@link ai.konduit.serving.pipeline.api.data.Image}s to {@link ai.konduit.serving.pipeline.api.data.NDArray}s.
 * The exact way that images are converted is highly configurable (formats, channels, output sizes, normalization, etc) -
 * see {@link ImageToNDArrayConfig} for more details<br>
 * <br>
 * The following configuration is available:
 * <ul>
 *     <li><b>config</b>: the {@link ImageToNDArrayConfig} configuration for how conversion should be performed</li>
 *     <li><b>keys</b>: may be null. If non-null: These are the names of images in the Data instance to convert</li>
 *     <li><b>outputNames</b>: may be null. If non-null: the input images are renamed to this in the output Data instance after conversion to NDArray</li>
 *     <li><b>keepOtherValues</b>: True by default. If true: copy all the other (non-converted/non-image) entries in the input Data to the output Data</li>
 *     <li><b>metadata</b>: False by default. If true: include metadata about the images in the output Data - for example if/how it was cropped,
 *         and the original input size.</li>
 *     <li><b>metadataKey</b>: Sets the key that the metadata will be stored under. Default: {@link #DEFAULT_METADATA_KEY}. Not relevant if
 *         metadata == false</li>
 * </ul>
 *
 * Note that metadata will have the following format:<br>
 * If a single image is converted, the metadata Data instance will have a nested Data instance
 * i.e.:
 * <pre>
 * {@code
 * Data meta = myData.get
 * }}</pre>
 *
 * If multiple images are converted
 *
 * @author Alex Black
 */
@Builder
@Data
public class ImageToNDArrayStep implements PipelineStep {

    public static final String DEFAULT_METADATA_KEY = "@ImageToNDArrayStepMetadata";
    public static final String META_INNAME_KEY = "in_name";
    public static final String META_OUTNAME_KEY = "out_name";
    public static final String META_IMG_H = "image_height";
    public static final String META_IMG_W = "image_width";
    public static final String META_CROP_REGION = "crop_region";

    private ImageToNDArrayConfig config;
    private List<String> keys;
    private List<String> outputNames;
    @Builder.Default
    private boolean keepOtherValues = true;
    private boolean metadata;
    @Builder.Default
    private String metadataKey = DEFAULT_METADATA_KEY;

    public ImageToNDArrayStep(@JsonProperty("config") ImageToNDArrayConfig config, @JsonProperty("keys") List<String> keys,
                              @JsonProperty("outputNames") List<String> outputNames, @JsonProperty("keepOtherValues") boolean keepOtherValues,
                              @JsonProperty("metadata") boolean metadata, @JsonProperty("metadataKey") String metadataKey){
        this.config = config;
        this.keys = keys;
        this.outputNames = outputNames;
        this.keepOtherValues = keepOtherValues;
        this.metadata = metadata;
        this.metadataKey = metadataKey;
    }

}
