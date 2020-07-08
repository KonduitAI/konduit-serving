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

package ai.konduit.serving.pipeline.api.data;

import ai.konduit.serving.pipeline.impl.data.point.NDPoint;
import ai.konduit.serving.pipeline.impl.serde.PointDeserializer;
import org.nd4j.shade.jackson.databind.annotation.JsonDeserialize;

import java.util.Objects;

/**
 * Point is usually used to represent a point in space. Often, this may be a point on a 2 dimensional plane, or in 3
 * dimensional space. However, a Point can in principle have any number of dimensions.
 * <p>
 * Points are generally specified with relative coordinates when used together with other inputs like pictures.
 * But using them with absolute values is also possible.
 *
 * @author Paul Dubs
 */
@JsonDeserialize(using = PointDeserializer.class)
public interface Point {

    /**
     * As per {@link #create(double, double, String, Double)} without a label or probability
     */
    static Point create(double x, double y) {
        return create(x, y, null, null);
    }

    /**
     * Create a 2d Point instance.
     * As per {@link Point}, specifying these as as a fraction of image size (i.e., 0.0 to 1.0) is generally preferred
     *
     * @param x          X location
     * @param y          Y location
     * @param label       Label for the point. May be null.
     * @param probability Probability for the point, in range [0.0, 1.0]. May be null
     * @return Point instance
     */
    static Point create(double x, double y, String label, Double probability) {
        return new NDPoint(new double[]{x, y}, label, probability);
    }

    /**
     * As per {@link #create(double, double, double, String, Double)} without a label or probability
     */
    static Point create(double x, double y, double z) {
        return create(x, y, z, null, null);
    }

    /**
     * Create a 3d Point instance.
     * As per {@link Point}, specifying these as as a fraction of image size (i.e., 0.0 to 1.0) is generally preferred
     *
     * @param x          X location
     * @param y          Y location
     * @param z          Z location
     * @param label       Label for the point. May be null.
     * @param probability Probability for the point, in range [0.0, 1.0]. May be null
     * @return Point instance
     */
    static Point create(double x, double y, double z, String label, Double probability) {
        return new NDPoint(new double[]{x, y, z}, label, probability);
    }

    /**
     * As per {@link #create(double[], String, Double)} without a label or probability
     */
    static Point create(double... x) {
        return create(x, null, null);
    }

    /**
     * Create a n-d Point instance.
     * As per {@link Point}, specifying these as as a fraction of image size (i.e., 0.0 to 1.0) is generally preferred
     *
     * @param x          coordinates of the point in n-dimensional space
     * @param label       Label for the point. May be null.
     * @param probability Probability for the point, in range [0.0, 1.0]. May be null
     * @return Point instance
     */
    static Point create(double[] x, String label, Double probability) {
        return new NDPoint(x, label, probability);
    }


    /**
     * @return The n-th coordinate of the point
     */
    double get(int n);

    /**
     * @return Dimensionality of the point
     */
    int dimensions();


    /**
     * @return The X coordinate (i.e., the first dimension of the point)
     */
    default double x() { return get(0); }

    /**
     * @return The y coordinate (i.e., the second dimension of the point)
     */
    default double y() { return get(1); }


    /**
     * @return The z coordinate (i.e., the third dimension of the point)
     */
    default double z() { return get(2); }

    /**
     * @return The label for the bounding box. May be null.
     */
    String label();

    /**
     * @return The probability for the bounding box. May be null.
     */
    Double probability();

    /**
     * As per {@link #equals(Point, Point, double, double)} using eps = 1e-5 and probEps = 1e-5
     */
    static boolean equals(Point p1, Point p2) {
        return equals(p1, p2, 1e-5, 1e-5);
    }

    /**
     * @param p1     First point to compare
     * @param p2     Second point to compare
     * @param eps     Epsilon value for coordinates. Use 0.0 for exact, or non-zero for "approximately equals" behaviour
     * @param probEps Epsilon value for probability comparison. Use 0.0 for exact probability match, or non-zero for "approximately
     *                equal" behavior. Unused if one/both points don't have a probability value
     * @return True if the two points are equals, false otherwise
     */
    static boolean equals(Point p1, Point p2, double eps, double probEps) {
        if(p1 == p2){ return true; }
        if(p1.dimensions() != p2.dimensions()){ return false; }
        for (int i = 0; i < p1.dimensions(); i++) {
            if(Math.abs(p1.get(i) - p2.get(i)) >= eps){ return false; }
        }
        return Objects.equals(p1.label(), p2.label()) &&
                ((p1.probability() == null && p2.probability() == null) ||
                        (p1.probability() != null && Math.abs(p1.probability() - p2.probability()) < probEps));
    }


    /**
     * Turn relative defined coordinates into absolute coordinates
     */
    default Point toAbsolute(double... absoluteSizes){
        // if the first point is absolute (not between 0 and 1), all others should be too
        if(!(0.0 < x() && x() < 1.0)) { return this; }

        double[] coords = new double[dimensions()];
        if(coords.length != absoluteSizes.length){
            throw new IllegalArgumentException("An absolute size has to be defined for each dimension of the point!");
        }
        for (int i = 0; i < coords.length; i++) {
            coords[i] = absoluteSizes[i] * get(i);
        }
        return Point.create(coords, label(), probability());
    }
}
