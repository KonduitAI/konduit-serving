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

package ai.konduit.serving.pipeline.impl.format;

import ai.konduit.serving.pipeline.api.data.NDArrayType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.nio.ByteBuffer;
import java.util.Arrays;

@AllArgsConstructor
@Data
public class SerializedNDArray {
    private final NDArrayType type;
    private final long[] shape;
    private final ByteBuffer buffer;

    @Override
    public boolean equals(Object o){
        if(!(o instanceof SerializedNDArray))
            return false;

        SerializedNDArray s = (SerializedNDArray)o;
        if(type != s.type || !Arrays.equals(shape, s.shape))
            return false;

        if(buffer.capacity() != s.buffer.capacity())
            return false;

        if(buffer.hasArray() && s.buffer.hasArray()){
            return Arrays.equals(buffer.array(), s.buffer.array());
        }

        int n = buffer.capacity();
        for( int i=0; i<n; i++ ){
            byte b1 = buffer.get(i);
            byte b2 = s.buffer.get(i);
            if(b1 != b2)
                return false;
        }

        return true;
    }
}
