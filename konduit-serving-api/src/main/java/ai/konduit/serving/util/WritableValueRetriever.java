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

import org.datavec.api.writable.*;
import org.nd4j.common.base.Preconditions;
import org.nd4j.linalg.api.ndarray.INDArray;

/**
 * Utilities for datavec's {@link Writable}
 * this just perform basic conversion
 * between unknown objects and {@link Writable}
 * types.
 *
 * @author Adam Gibson
 */
public class WritableValueRetriever {


    /**
     * Create a {@link Writable}
     * from the given value
     * If an input type is invalid, an
     * {@link IllegalArgumentException}
     * will be thrown
     *
     * @param input the input object
     * @return writable
     */
    public static Writable writableFromValue(Object input) {
        Preconditions.checkNotNull(input, "Unable to convert null value!");
        if (input instanceof Double) {
            return new DoubleWritable((Double) input);
        } else if (input instanceof Float) {
            return new FloatWritable((Float) input);
        } else if (input instanceof String) {
            return new Text(input.toString());
        } else if (input instanceof Long) {
            return new LongWritable((Long) input);
        } else if (input instanceof INDArray) {
            return new NDArrayWritable((INDArray) input);
        } else if (input instanceof Integer) {
            return new IntWritable((Integer) input);
        } else if (input instanceof byte[]) {
            return new BytesWritable((byte[]) input);
        } else if (input instanceof Boolean) {
            return new BooleanWritable((Boolean) input);
        } else
            throw new IllegalArgumentException("Unsupported type " + input.getClass().getName());
    }

    /**
     * Get the underlying value fro the given {@link Writable}
     *
     * @param writable the writable to get the value for
     * @return the underlying value represnted by the {@link Writable}
     */
    public static Object getUnderlyingValue(Writable writable) {
        switch (writable.getType()) {
            case Float:
                return writable.toFloat();
            case Double:
                return writable.toDouble();
            case Int:
                return writable.toInt();
            case Long:
                return writable.toLong();
            case NDArray:
                NDArrayWritable ndArrayWritable = (NDArrayWritable) writable;
                return ndArrayWritable.get();
            case Boolean:
                BooleanWritable booleanWritable = (BooleanWritable) writable;
                return booleanWritable.get();
            case Byte:
                ByteWritable byteWritable = (ByteWritable) writable;
                return byteWritable.get();
            case Bytes:
                BytesWritable bytesWritable = (BytesWritable) writable;
                return bytesWritable.getContent();
            case Text:
                return writable.toString();
            default:
                throw new UnsupportedOperationException();
        }
    }

}
