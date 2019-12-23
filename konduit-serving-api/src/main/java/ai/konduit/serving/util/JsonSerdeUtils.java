/*
 *       Copyright (c) 2019 Konduit AI.
 *
 *       This program and the accompanying materials are made available under the
 *       terms of the Apache License, Version 2.0 which is available at
 *       https://www.apache.org/licenses/LICENSE-2.0.
 *
 *       Unless required by applicable law or agreed to in writing, software
 *       distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *       WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *       License for the specific language governing permissions and limitations
 *       under the License.
 *
 *       SPDX-License-Identifier: Apache-2.0
 *
 */

package ai.konduit.serving.util;

import ai.konduit.serving.config.SchemaType;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 *
 */
public class JsonSerdeUtils {


    /**
     *
     * @param schemaWithValues
     * @param fieldName
     * @return
     */
    public static INDArray deSerializeBase64Numpy(JsonObject schemaWithValues,String fieldName) {
        byte[] numpyValue  = schemaWithValues.getJsonObject("values").getBinary(fieldName);
        return Nd4j.createNpyFromByteArray(numpyValue);
    }

    public static Map<String,Object> deSerializeSchemaValues(JsonObject schemaValues, Map<String, SchemaType> schemaTypes) {
        return null;
    }

    /**
     * Converts an {@link JsonArray}
     * to a {@link INDArray} with a {@link org.nd4j.linalg.api.buffer.DataType#FLOAT}
     *
     * @param arr the {@link JsonArray} to convert
     * @return an equivalent {@link INDArray}
     */
    public static INDArray jsonToNDArray(JsonArray arr) {
        List<Integer> shapeList = new ArrayList<>();
        JsonArray currArr = arr;
        while(true) {
           shapeList.add(currArr.size());
           Object firstElement = currArr.getValue(0);
           if (firstElement instanceof JsonArray){
               currArr = (JsonArray)firstElement;
           }
           else {
               break;
           }
        }

        long[] shape = new long[shapeList.size()];
        for (int i = 0; i < shape.length; i++) {
            shape[i] = shapeList.get(i).longValue();
        }

        INDArray ndArray = Nd4j.zeros(shape);
        INDArray flatNdArray = ndArray.reshape(-1);
        int idx = 0;
        Stack<JsonArray> stack = new Stack<>();
        stack.push(arr);
        while(!stack.isEmpty()) {
            JsonArray popped = stack.pop();
            Object first = popped.getValue(0);
            if (first instanceof JsonArray) {
                for (int i = popped.size()-1; i >= 0; i--) {
                    stack.push(popped.getJsonArray(i));
                }
            }
            else{
                for (int i = 0; i < popped.size(); i++) {
                    flatNdArray.putScalar(idx++, ((Number)popped.getValue(i)).doubleValue());
                }
            }
        }

        return ndArray;
    }
}
