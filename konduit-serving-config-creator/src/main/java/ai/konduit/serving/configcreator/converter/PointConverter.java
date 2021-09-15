/*
 *  ******************************************************************************
 *  *
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Apache License, Version 2.0 which is available at
 *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  *  See the NOTICE file distributed with this work for additional
 *  *  information regarding copyright ownership.
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  * License for the specific language governing permissions and limitations
 *  * under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *  *****************************************************************************
 */
package ai.konduit.serving.configcreator.converter;

import ai.konduit.serving.pipeline.api.data.Point;
import ai.konduit.serving.pipeline.impl.data.point.NDPoint;
import picocli.CommandLine;

import java.util.HashMap;
import java.util.Map;

/**
 * Convert a {@link Point} usually {@link NDPoint} implementation
 * from a string with the following format:
 * Split fields by comma, Separate field names with
 * fieldName=value.
 * For the coordinate array {@link NDPoint#coords} each value
 * is separated by a space.
 */
public class PointConverter implements CommandLine.ITypeConverter<Point> {
    @Override
    public Point convert(String value) throws Exception {
        String[] split = value.split(",");
        Map<String,String> input = new HashMap<>();
        for(String keyVal : split) {
            String[] keyValSplit = keyVal.split("=");
            input.put(keyValSplit[0],keyValSplit[1]);
        }

        NDPoint point = new NDPoint();

        for(Map.Entry<String,String> entry : input.entrySet()) {
            switch(entry.getKey()) {
                case "coords":
                    String[] coordSplit = entry.getValue().split(" ");
                    double[] parsed = new double[coordSplit.length];
                    for(int i = 0; i < coordSplit.length; i++) {
                        parsed[i] = Double.parseDouble(coordSplit[i]);
                    }

                    point.coords(parsed);
                    break;
                case "label":
                    point.label(entry.getValue());
                    break;
                case "probability":
                    point.probability(Double.parseDouble(entry.getValue()));
                    break;

            }
        }
        return point;
    }
}
