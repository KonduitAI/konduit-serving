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

import org.bytedeco.opencv.opencv_core.Scalar;
import org.nd4j.common.base.Preconditions;

import java.awt.*;
import java.util.Random;

public class ColorUtil {

    public static final String COLOR_DESCRIPTION = "The color can be a hex/HTML string like" +
            "\"#788E87\", an RGB value like RGB - \"rgb(128,0,255)\" or  it can be from a set of predefined HTML color names: " +
            "[white, silver, gray, black, red, maroon, yellow, olive, lime, green, aqua, teal, blue, navy, fuchsia, purple]";

    public static final String INVALID_COLOR = "Invalid color: Must be in one of the following formats: hex/HTML - #788E87, " +
            "RGB - rgb(128,0,255), or a HTML color name such as \"green\" (https://en.wikipedia.org/wiki/Web_colors#HTML_color_names) - got \"%s\"";

    private ColorUtil() {
    }

    /**
     * Convert a color to a Scalar in one of 3 formats:<br>
     * hex/HTML - {@code #788E87}<br>
     * RGB - "rgb(128,0,255)"<br>
     * A HTML color name such as "green" (https://en.wikipedia.org/wiki/Web_colors#HTML_color_names)<br>
     * @param s Color name
     * @return Color
     */
    public static Scalar stringToColor(String s) {
        if (s.startsWith("#")) {
            String hex = s.substring(1);
            Color c = Color.decode(hex);
            return org.bytedeco.opencv.helper.opencv_core.RGB(c.getRed(), c.getGreen(), c.getBlue());
        } else if (s.toLowerCase().startsWith("rgb(") && s.endsWith(")")) {
            String sub = s.substring(4, s.length() - 1);
            Preconditions.checkState(sub.matches("\\d+,\\d+,\\d+"), INVALID_COLOR, s);
            String[] split = sub.split(",");
            int r = Integer.parseInt(split[0]);
            int g = Integer.parseInt(split[1]);
            int b = Integer.parseInt(split[2]);
            return org.bytedeco.opencv.helper.opencv_core.RGB(r, g, b);
        } else {
            Scalar sc = getColorHTML(s);
            if (sc == null) {
                throw new UnsupportedOperationException(String.format(INVALID_COLOR, s));
            }
            return sc;
        }
    }

    /**
     * Get a color from the 16 HTML color names (not case sensitive):
     * while, silver, gray, black, red, maroon, yellow, olive, lime, green, aqua, teal, blue, navy, fuscia, purple.
     * Returns null for any not found colors
     * @return The color name as a Scalar, or null if not found
     */
    public static Scalar getColorHTML(String name) {
        switch (name.toLowerCase()) {
            case "white":
                return org.bytedeco.opencv.helper.opencv_core.RGB(255, 255, 255);
            case "silver":
                return org.bytedeco.opencv.helper.opencv_core.RGB(0xC0, 0xC0, 0xC0);
            case "gray":
                return org.bytedeco.opencv.helper.opencv_core.RGB(0x80, 0x80, 0x80);
            case "black":
                return org.bytedeco.opencv.helper.opencv_core.RGB(0x00, 0x00, 0x00);
            case "red":
                return org.bytedeco.opencv.helper.opencv_core.RGB(0xFF, 0x00, 0x00);
            case "maroon":
                return org.bytedeco.opencv.helper.opencv_core.RGB(0x80, 0x00, 0x00);
            case "yellow":
                return org.bytedeco.opencv.helper.opencv_core.RGB(0xFF, 0xFF, 0x00);
            case "olive":
                return org.bytedeco.opencv.helper.opencv_core.RGB(0x80, 0x80, 0x00);
            case "lime":
                return org.bytedeco.opencv.helper.opencv_core.RGB(0x00, 0xFF, 0x00);
            case "green":
                return org.bytedeco.opencv.helper.opencv_core.RGB(0x00, 0x80, 0x00);
            case "aqua":
                return org.bytedeco.opencv.helper.opencv_core.RGB(0x00, 0xFF, 0xFF);
            case "teal":
                return org.bytedeco.opencv.helper.opencv_core.RGB(0x00, 0x80, 0x80);
            case "blue":
                return org.bytedeco.opencv.helper.opencv_core.RGB(0x00, 0x00, 0xFF);
            case "navy":
                return org.bytedeco.opencv.helper.opencv_core.RGB(0x00, 0x00, 0x80);
            case "fuchsia":
                return org.bytedeco.opencv.helper.opencv_core.RGB(0xFF, 0x00, 0xFF);
            case "purple":
                return org.bytedeco.opencv.helper.opencv_core.RGB(0x80, 0x00, 0x80);
        }
        return null;
    }

    /**
     * Regenate a random color using the specified RGN
     *
     * @param rng RNG to use
     * @return Random color
     */
    public static Scalar randomColor(Random rng) {
        return org.bytedeco.opencv.helper.opencv_core.RGB(rng.nextInt(255), rng.nextInt(255), rng.nextInt(255));
    }


}
