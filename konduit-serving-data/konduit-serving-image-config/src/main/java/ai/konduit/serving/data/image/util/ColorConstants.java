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

package ai.konduit.serving.data.image.util;

public class ColorConstants {

    public static final String COLOR_DESCRIPTION = "The color can be a hex/HTML string like" +
            "\"#788E87\", an RGB value like RGB - \"rgb(128,0,255)\" or  it can be from a set of predefined HTML color names: " +
            "[white, silver, gray, black, red, maroon, yellow, olive, lime, green, aqua, teal, blue, navy, fuchsia, purple]";

    public static final String INVALID_COLOR = "Invalid color: Must be in one of the following formats: hex/HTML - #788E87, " +
            "RGB - rgb(128,0,255), or a HTML color name such as \"green\" (https://en.wikipedia.org/wiki/Web_colors#HTML_color_names) - got \"%s\"";




}
