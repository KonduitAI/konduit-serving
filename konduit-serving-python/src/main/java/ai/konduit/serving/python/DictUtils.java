/* ******************************************************************************
 * Copyright (c) 2022 Konduit K.K.
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
package ai.konduit.serving.python;

import ai.konduit.serving.pipeline.api.data.BoundingBox;
import ai.konduit.serving.pipeline.api.data.Point;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dictionary Utils for handling
 * type conversion for dictionaries
 * and serialized specialized objects
 * for python pipeline step.
 *
 * @author Adam Gibson
 */
public class DictUtils {


    private DictUtils(){}


    /**
     * Create a {@link Point}
     *  object from an input dictionary
     * @param dictPoint the input dictionary
     * @return the created {@link Point}
     */
    public static Point fromPointDict(Map<String,Object> dictPoint) {
        Point point = Point.create(
                castNumber(dictPoint.get("x")).doubleValue(),
                castNumber(dictPoint.get("y")).doubleValue(),
                castNumber(dictPoint.get("z")).doubleValue(),
                dictPoint.getOrDefault("label","").toString(),
                castNumber(dictPoint.getOrDefault("probability",0.0))
                        .doubleValue()
        );

        return point;
    }

    private static Number castNumber(Object input) {
        return (Number) input;
    }

    /**
     * Create a dictionary from {@link Point}
     *  based on the defined attributes
     * @param point the input point
     * @return the returned dictionary
     */
    public static Map<String,Object> toPointDict(Point point) {
        Map<String,Object> ret = new LinkedHashMap<>(5);
        ret.put("x",point.x());
        ret.put("y",point.y());
        ret.put("label",point.label());
        if(point.dimensions() > 2)
            ret.put("z",point.z());
        ret.put("dimensions",point.dimensions());
        return ret;
    }

    /**
     * Convert a {@link BoundingBox}
     * to a dictionary
     * @param boundingBox the bounding box to convert
     * @return the bounding box as a dictionary
     */
    public static Map<String,Object> toBoundingBoxDict(BoundingBox boundingBox) {
        Map<String,Object> boundingBoxValues = new LinkedHashMap<>();
        boundingBoxValues.put("cx",boundingBox.cx());
        boundingBoxValues.put("cy",boundingBox.cy());
        boundingBoxValues.put("width",boundingBox.width());
        boundingBoxValues.put("height",boundingBox.height());
        boundingBoxValues.put("label",boundingBox.label());
        boundingBoxValues.put("probability",boundingBox.probability());
        boundingBoxValues.put("cy",boundingBox.cy());
        boundingBoxValues.put("x1",boundingBox.x1());
        boundingBoxValues.put("x2",boundingBox.x2());
        boundingBoxValues.put("y1",boundingBox.y1());
        boundingBoxValues.put("y2",boundingBox.y2());
        return boundingBoxValues;
    }


    /**
     * Create a {@link BoundingBox}  from a dictionary.
     * The attributes are:
     * cx,
     * cy
     * height
     * width
     * label
     * probability
     * @param dict a dict with the above attributes
     * @return the equivalent bounding box with the given attributes
     */
    public static BoundingBox boundingBoxFromDict(Map<String,Object> dict) {
        BoundingBox boundingBox = BoundingBox.create(
                (double) dict.get("cx"),
                (double) dict.get("cy"),
                (double) dict.get("height"),
                (double) dict.get("width"),
                (String) dict.get("label"),
                (double) dict.get("probability"));
        return boundingBox;
    }

}
