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

package ai.konduit.serving.models.tensorflow.format;

import ai.konduit.serving.models.tensorflow.util.TensorFlowUtil;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.data.NDArrayType;
import ai.konduit.serving.pipeline.api.format.NDArrayConverter;
import ai.konduit.serving.pipeline.api.format.NDArrayFormat;
import ai.konduit.serving.pipeline.impl.data.ndarray.SerializedNDArray;
import lombok.AllArgsConstructor;
import org.nd4j.common.base.Preconditions;
import org.nd4j.common.util.ArrayUtil;
import org.tensorflow.Tensor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class TensorFlowConverters {

    private TensorFlowConverters(){ }

    @AllArgsConstructor
    public static class SerializedToTensorFlowConverter implements NDArrayConverter {
        @Override
        public boolean canConvert(NDArray from, NDArrayFormat to) {
            return canConvert(from, to.formatType());
        }

        @Override
        public boolean canConvert(NDArray from, Class<?> to) {
            return SerializedNDArray.class.isAssignableFrom(from.get().getClass()) && Tensor.class.isAssignableFrom(to);
        }

        @Override
        public <U> U convert(NDArray from, Class<U> to) {
            Preconditions.checkState(canConvert(from, to), "Unable to convert NDArray to %s", to);
            SerializedNDArray t = (SerializedNDArray) from.get();
            Tensor<?> arr = convert(t);
            return (U)arr;
        }

        @Override
        public <U> U convert(NDArray from, NDArrayFormat<U> to) {
            Preconditions.checkState(canConvert(from, to), "Unable to convert to format: %s", to);
            SerializedNDArray f = (SerializedNDArray) from.get();
            Tensor<?> arr = convert(f);
            return (U)arr;
        }

        public Tensor<?> convert(SerializedNDArray from){
            long[] shape = from.getShape();
            Class<?> tfType = TensorFlowUtil.toTFType(from.getType());
            from.getBuffer().rewind();
            Tensor<?> t = Tensor.create(tfType, shape, from.getBuffer());
            return t;
        }
    }

    @AllArgsConstructor
    public static class TensorFlowToSerializedConverter implements NDArrayConverter {
        @Override
        public boolean canConvert(NDArray from, NDArrayFormat to) {
            return canConvert(from, to.formatType());
        }

        @Override
        public boolean canConvert(NDArray from, Class<?> to) {
            return Tensor.class.isAssignableFrom(from.get().getClass()) && SerializedNDArray.class.isAssignableFrom(to);
        }

        @Override
        public <U> U convert(NDArray from, Class<U> to) {
            Preconditions.checkState(canConvert(from, to), "Unable to convert NDArray to %s", to);
            Tensor<?> t = (Tensor<?>) from.get();
            SerializedNDArray arr = convert(t);
            return (U)arr;
        }

        @Override
        public <U> U convert(NDArray from, NDArrayFormat<U> to) {
            Preconditions.checkState(canConvert(from, to), "Unable to convert to format: %s", to);
            Tensor<?> t = (Tensor<?>) from.get();
            SerializedNDArray arr = convert(t);
            return (U)arr;
        }

        public SerializedNDArray convert(Tensor<?> from){
            long[] shape = from.shape();
            NDArrayType t = TensorFlowUtil.fromTFType(from.dataType());

            int w = t.width();
            long length = ArrayUtil.prodLong(shape);
            long lengthBytes = w * length;
            ByteBuffer bb = ByteBuffer.allocateDirect((int)lengthBytes).order(ByteOrder.nativeOrder());     //TODO SerializedNDArray should be in little endian...
            from.writeTo(bb);

            return new SerializedNDArray(t, shape, bb);
        }
    }

}
