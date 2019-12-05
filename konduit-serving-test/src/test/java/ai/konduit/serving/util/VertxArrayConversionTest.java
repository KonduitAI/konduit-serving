/*
 *
 *  * ******************************************************************************
 *  *  * Copyright (c) 2015-2019 Skymind Inc.
 *  *  * Copyright (c) 2019 Konduit AI.
 *  *  *
 *  *  * This program and the accompanying materials are made available under the
 *  *  * terms of the Apache License, Version 2.0 which is available at
 *  *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  *  * License for the specific language governing permissions and limitations
 *  *  * under the License.
 *  *  *
 *  *  * SPDX-License-Identifier: Apache-2.0
 *  *  *****************************************************************************
 *
 *
 */

package ai.konduit.serving.util;

import io.netty.buffer.Unpooled;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.serde.binary.BinarySerde;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static junit.framework.TestCase.assertEquals;

public class VertxArrayConversionTest {

    @Test(timeout = 60000)

    public void testConversion() {
        INDArray initial = Nd4j.linspace(1, 4, 4);
        byte[] npyBytes = Nd4j.toNpyByteArray(initial);
        Buffer npyBuffer = Buffer.buffer(npyBytes);
        INDArray npyArray = VertxArrayConversion.toArray(npyBuffer, "numpy");
        assertEquals(initial, npyArray);

        JsonArray jsonArray = new JsonArray().add(new JsonArray(Arrays.asList(1, 2, 3, 4)));
        INDArray jsonConversion = VertxArrayConversion.toArray(Buffer.buffer(jsonArray.toString()), "json").reshape(4);
        assertEquals(initial, jsonConversion);

        ByteBuffer nd4j = BinarySerde.toByteBuffer(initial);
        Buffer buffer = Buffer.buffer(Unpooled.wrappedBuffer(nd4j));
        INDArray nd4jConverted = VertxArrayConversion.toArray(buffer, "nd4j");
        assertEquals(initial, nd4jConverted);
    }


}
