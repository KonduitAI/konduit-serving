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

/**
 * The format to be used when converting an Image to an NDArray<br>
 * CHANNELS_FIRST (output shape: [1, c, h, w] or [c, h, w]) or CHANNELS_LAST (output shape: [1, h, w, c] or [h, w, c])<br>
 * See {@link ai.konduit.serving.data.image.convert.ImageToNDArrayConfig}
 */

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The format to be used when converting an Image to an NDArray. " +
        "CHANNELS_FIRST -> (output shape: [1, c, h, w] or [c, h, w]), " +
        "CHANNELS_LAST -> (output shape: [1, h, w, c] or [h, w, c]).")
public enum NDFormat {
    CHANNELS_FIRST,
    CHANNELS_LAST
}
