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

package ai.konduit.serving.data.image.convert.config;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * How We should handle the situation where the input image and output image/NDArray have different aspect ratios?<br>
 * Use in {@link ai.konduit.serving.data.image.convert.ImageToNDArrayConfig} and {@link ai.konduit.serving.data.image.step.resize.ImageResizeStep}
 * See {@link ai.konduit.serving.data.image.convert.ImageToNDArrayConfig} for more details<br>
 * <ul>
 *     <li>CENTER_CROP: Crop the larger dimension down to the correct aspect ratio (and then resize if necessary).</li>
 *     <li>PAD: Zero pad the smaller dimension to make the aspect ratio match the output (and then resize if necessary)</li>
 *     <li>STRETCH: Simply resize the image to the required aspect ratio, distorting the image if necessary</li>
 * </ul>
 */
@Schema(description = "An enum specifying how to handle the situation where the input image and output. NDArray have different aspect ratios. <br><br>" +
        "CENTER_CROP -> Crop the larger dimension down to the correct aspect ratio (and then resize if necessary), <br>" +
        "PAD -> Zero pad the smaller dimension to make the aspect ratio match the output (and then resize if necessary), <br>" +
        "STRETCH -> Simply resize the image to the required aspect ratio, distorting the image if necessary")
public enum AspectRatioHandling {
    CENTER_CROP, PAD, STRETCH
}
