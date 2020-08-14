/* ******************************************************************************
 * Copyright (c) 2020 Konduit K.K.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/
package ai.konduit.serving.pipeline.api.data;

import ai.konduit.serving.pipeline.impl.data.box.BBoxCHW;
import ai.konduit.serving.pipeline.impl.data.box.BBoxXY;
import ai.konduit.serving.pipeline.impl.pipeline.serde.BoundingBoxDeserializer;
import ai.konduit.serving.pipeline.impl.pipeline.serde.BoundingBoxSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import org.nd4j.shade.jackson.databind.annotation.JsonDeserialize;
import org.nd4j.shade.jackson.databind.annotation.JsonSerialize;

import java.util.Objects;

/**
 * BoundingBox is usually used to represent a rectangular region in an image, along with an optional String label,
 * and an aptional double probability.
 * <p>
 * Bounding boxes can be defined in two (essentially equivalent) formats:<br>
 * (a) Based on center X, center Y, height and width<br>
 * (b) Based on x1, x2, y1 and y2 locations (i.e., lower/upper X, lower/upper Y coordinate)<br>
 * <p>
 * As a general rule, bounding box coordinates are specified as a fraction of the image - with coordinates (x,y)=(0.0, 0.0)
 * being top left of the image, and (x,y)=(1.0, 1.0) being the bottom right of the image.<br>
 * However, in some use cases, specifying x/y locations in pixels might be used.
 *
 * @author Alex Black
 */
@Schema(description = "This object is usually used to represent a rectangular region in an image, along with an optional String label, " +
        "and an optional double probability. Bounding boxes can be defined in two (essentially equivalent) formats: " +
        "(a) Based on center X, center Y, height and width " +
        "(b) Based on x1, x2, y1 and y2 locations (i.e., lower/upper X, lower/upper Y coordinate) " +
        "As a general rule, bounding box coordinates are specified as a fraction of the image - with coordinates (x,y)=(0.0, 0.0) " +
        "being top left of the image, and (x,y)=(1.0, 1.0) being the bottom right of the image. However, in some use cases, " +
        "specifying x/y locations in pixels might be used.")
@JsonSerialize(using = BoundingBoxSerializer.class)
@JsonDeserialize(using = BoundingBoxDeserializer.class)
public interface BoundingBox {

    /**
     * As per {@link #createXY(double, double, double, double, String, Double)} without a label or probability
     */
    static BoundingBox create(double cx, double cy, double h, double w) {
        return create(cx, cy, h, w, "", 0.0);
    }

    /**
     * Create a BoundingBox instance based on the center X/Y location and box height/width.
     * As per {@link BoundingBox}, specifying these as as a fraction of image size (i.e., 0.0 to 1.0) is generally preferred
     *
     * @param cx          Center X location
     * @param cy          Center Y location
     * @param h           Height
     * @param w           Width
     * @param label       Label for the bounding box. May be null.
     * @param probability PRobability for the bounding box, in range [0.0, 1.0]. May be null
     * @return BoundingBox instance
     */
    static BoundingBox create(double cx, double cy, double h, double w, String label, Double probability) {
        return new BBoxCHW(cx, cy, h, w, label, probability);
    }


    static BoundingBox createXY(double x1, double x2, double y1, double y2) {
        return createXY(x1, x2, y1, y2, "", 0.0);
    }

    /**
     * Create a BoundingBox instance based on lower/upper X/Y coordinates of the box.<br>
     * i.e., the top-left of the box is defined by (x1, y1) and the bottom-right is defined by (x2, y2)<br>
     * As per {@link BoundingBox}, specifying these as as a fraction of image size (i.e., 0.0 to 1.0) is generally preferred
     *
     * @param x1          The lower X coordinate for the box (i.e., top-left X coordinate)
     * @param x2          The upper X coordinate for the box (i.e., bottom-right X coordinate)
     * @param y1          The smaller Y coordinate for the box (i.e., top-left Y coordinate)
     * @param y2          The larger Y coordinate for the box (i.e., bottom-right Y coordinate)
     * @param label       Label for the bounding box. May be null.
     * @param probability PRobability for the bounding box, in range [0.0, 1.0]. May be null
     * @return BoundingBox instance
     */
    static BoundingBox createXY(double x1, double x2, double y1, double y2, String label, Double probability) {
        return new BBoxXY(x1, x2, y1, y2, label, probability);
    }

    /**
     * @return The lower X coordinate (i.e., top-left X coordinate)
     */
    double x1();

    /**
     * @return The upper X coordinate (i.e., bottom-right X coordinate)
     */
    double x2();

    /**
     * @return The smaller Y coordinate (i.e., top-left Y coordinate)
     */
    double y1();

    /**
     * @return The larger X coordinate (i.e., bottom-right Y coordinate)
     */
    double y2();

    /**
     * @return The bounding box center X coordinate
     */
    double cx();

    /**
     * @return The bounding box center Y coordinate
     */
    double cy();

    default double width(){
        return Math.abs(x2() - x1());
    }

    default double height(){
        return Math.abs(y2() - y1());
    }

    /**
     * @return The label for the bounding box. May be null.
     */
    String label();

    /**
     * @return The probability for the bounding box. May be null.
     */
    Double probability();

    /**
     * As per {@link #equals(BoundingBox, BoundingBox, double, double)} using eps = 1e-5 and probEps = 1e-5
     */
    static boolean equals(BoundingBox bb1, BoundingBox bb2) {
        return equals(bb1, bb2, 1e-5, 1e-5);
    }

    /**
     * @param bb1     First bounding box to compare
     * @param bb2     Second bounding box to compare
     * @param eps     Epsilon value for X/Y coordinates. Use 0.0 for exact, or non-zero for "approximately equals" behaviour
     * @param probEps Epsilon value for probability comparison. Use 0.0 for exact probability match, or non-zero for "approximately
     *                equal" behavior. Unused if one/both bounding boxes don't have a probability value
     * @return True if the two bounding boxes are equals, false otherwise
     */
    static boolean equals(BoundingBox bb1, BoundingBox bb2, double eps, double probEps) {
        return Math.abs(bb1.x1() - bb2.x1()) < eps &&
                Math.abs(bb1.x2() - bb2.x2()) < eps &&
                Math.abs(bb1.y1() - bb2.y1()) < eps &&
                Math.abs(bb1.y2() - bb2.y2()) < eps &&
                Objects.equals(bb1.label(), bb2.label()) &&
                ((bb1.probability() == null && bb2.probability() == null) ||
                        (bb1.probability() != null && Math.abs(bb1.probability() - bb2.probability()) < probEps));
    }

}
