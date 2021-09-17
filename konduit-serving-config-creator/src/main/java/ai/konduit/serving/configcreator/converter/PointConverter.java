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

import ai.konduit.serving.configcreator.StringSplitter;
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
        StringSplitter stringSplitter = new StringSplitter(",");
        Map<String,String> input = stringSplitter.splitResult(value);

        double[] coords = null;
        String label = null;
        String probability = null;
        for(Map.Entry<String,String> entry : input.entrySet()) {
            switch(entry.getKey()) {
                //x,y
                case "x":
                    if(coords == null) {
                        coords = new double[2];
                    }
                    coords[0] = Double.parseDouble(entry.getValue());
                    break;
                case "y":
                    if(coords == null) {
                        coords = new double[2];
                    }
                    coords[1] = Double.parseDouble(entry.getValue());
                    break;

                case "coords":
                    String[] coordSplit = entry.getValue().split(" ");
                    double[] parsed = new double[coordSplit.length];
                    for(int i = 0; i < coordSplit.length; i++) {
                        parsed[i] = Double.parseDouble(coordSplit[i]);
                    }

                    coords = parsed;
                    break;
                case "label":
                    label = entry.getValue();
                    break;
                case "probability":
                    probability = entry.getValue();
                    break;

            }
        }

        return Point.create(coords,label,probability == null ? 0.0 : Double.parseDouble(probability));
    }




}
