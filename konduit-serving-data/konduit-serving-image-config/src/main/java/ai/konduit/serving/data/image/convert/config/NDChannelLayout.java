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

package ai.konduit.serving.data.image.convert.config;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents the type (and order) of the channels for an image after it has been converted to an NDArray
 * <ul>
 *     <li>RGB: 3 channels, ordered according to: red, green, blue - most common for TensorFlow, Keras, and some other libraries</li>
 *     <li>BGR: 3 channels, ordered according to: blue, green, red - the default for OpenCV, JavaCV, DL4J</li>
 *     <li>RGBA: 4 channels, ordered according to: red, green, blue, alpha</li>
 *     <li>BGRA: 4 channels, ordered according to: blue, green, red, alpha</li>
 *     <li>GRAYSCALE: 1 channel - grayscale</li>
 * </ul>
 */
@Schema(description = "An enum that represents the type (and order) of the channels for an image after it has been converted to an NDArray. <br><br>" +
        "RGB -> 3 channels, ordered according to: red, green, blue - most common for TensorFlow, Keras, and some other libraries, <br>" +
        "BGR -> 3 channels, ordered according to: blue, green, red - the default for OpenCV, JavaCV, DL4J, <br>" +
        "RGBA -> 4 channels, ordered according to: red, green, blue, alpha, <br>" +
        "BGRA -> 4 channels, ordered according to: blue, green, red, alpha, GRAYSCALE -> 1 channel - grayscale.")
public enum NDChannelLayout {
    RGB, RGBA, BGR, BGRA, GRAYSCALE;

    public int numChannels() {
        switch (this) {
            case RGB:
            case BGR:
                return 3;
            case RGBA:
            case BGRA:
                return 4;
            case GRAYSCALE:
                return 1;
            default:
                throw new RuntimeException("Unknown enum value: " + this);
        }
    }
}
