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

package ai.konduit.serving.pipeline.impl.serde;

import ai.konduit.serving.pipeline.api.data.Point;
import ai.konduit.serving.pipeline.impl.data.point.NDPoint;
import org.nd4j.shade.jackson.core.JsonParser;
import org.nd4j.shade.jackson.core.JsonProcessingException;
import org.nd4j.shade.jackson.databind.DeserializationContext;
import org.nd4j.shade.jackson.databind.JsonDeserializer;
import org.nd4j.shade.jackson.databind.JsonNode;
import org.nd4j.shade.jackson.databind.node.ArrayNode;

import java.io.IOException;

public class PointDeserializer extends JsonDeserializer<Point> {
    @Override
    public Point deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
        JsonNode n = jp.getCodec().readTree(jp);
        String lbl = n.has("label") ? n.get("label").textValue() : null;
        Double prob = n.has("probability") ? n.get("probability").doubleValue() : null;
        ArrayNode cn = (ArrayNode)n.get("coords");
        double[] pts = new double[cn.size()];
        for( int i=0; i<pts.length; i++ ){
            pts[i] = cn.get(i).doubleValue();
        }
        return new NDPoint(pts, lbl, prob);
    }
}
