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

import io.vertx.core.json.JsonObject;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import static org.junit.Assert.assertEquals;

public class JsonSerdeUtilsTest {

    @Test
    public void testNdArraySerialization() {
        JsonObject schemaWithValues = new JsonObject();
        String ndArrayFieldName = "ndarray";
        JsonObject values = new JsonObject();
        INDArray array = Nd4j.scalar(1.0);
        values.put(ndArrayFieldName, Nd4j.toNpyByteArray(array));
        schemaWithValues.put("values",values);

        INDArray deserialized = JsonSerdeUtils.deSerializeBase64Numpy(schemaWithValues,ndArrayFieldName);
        assertEquals(array,deserialized);
    }

}
