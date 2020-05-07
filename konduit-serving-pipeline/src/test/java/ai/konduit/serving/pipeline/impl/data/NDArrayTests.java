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

package ai.konduit.serving.pipeline.impl.data;

import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.data.NDArrayType;
import ai.konduit.serving.pipeline.api.format.NDArrayConverter;
import ai.konduit.serving.pipeline.api.format.NDArrayFactory;
import ai.konduit.serving.pipeline.api.format.NDArrayFormat;
import ai.konduit.serving.pipeline.impl.data.ndarray.BaseNDArray;
import ai.konduit.serving.pipeline.impl.data.ndarray.SerializedNDArray;
import ai.konduit.serving.pipeline.registry.NDArrayConverterRegistry;
import ai.konduit.serving.pipeline.registry.NDArrayFactoryRegistry;
import lombok.AllArgsConstructor;
import org.junit.Test;
import org.nd4j.common.base.Preconditions;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.assertArrayEquals;

public class NDArrayTests {

    @Test
    public void test2StepConversion(){
        //The idea: We add some new module with a new NDArray format, X
        //In that module, we implement only conversion of X->SerializedNDArray and SerializedNDArray->X
        //What if we want to do X->Y?
        //NDArrayConverterRegistry will try to do X->SerializedNDArray->Y - as SerializedNDArray is something all image types should implement
        // conversions to/from for

        float[] f = new float[]{1,2,3};
        

        NDArrayFactoryRegistry.addFactory(new TestNDArrayFactory());
        NDArrayConverterRegistry.addConverter(new TNDToSerializedNDArray());
        NDArrayConverterRegistry.addConverter(new SerializedNDArrayToTND());
        NDArray nd = NDArray.create(new TestNDArrayObject(f));

        //TestNDArray -> SerializedNDArray -> float[]
        float[] fArr = nd.getAs(float[].class);
        assertArrayEquals(f, fArr, 0.0f);

        //float[] -> SerializedNDArray -> TestNDArray
        NDArray i2 = NDArray.create(f);
        TestNDArrayObject out = i2.getAs(TestNDArrayObject.class);
        float[] outF = out.getF();
        assertArrayEquals(f, outF, 0.0f);
    }

    @AllArgsConstructor
    @lombok.Data
    public static class TestNDArrayObject {
        private float[] f;
    }

    public static class TestNDArray extends BaseNDArray<TestNDArrayObject> {
        public TestNDArray(TestNDArrayObject o) {
            super(o);
        }
    }

    public  static class TestNDArrayFactory implements NDArrayFactory {

        @Override
        public Set<Class<?>> supportedTypes() {
            return Collections.singleton(TestNDArrayObject.class);
        }

        @Override
        public boolean canCreateFrom(Object o) {
            return o instanceof TestNDArrayObject;
        }

        @Override
        public NDArray create(Object o) {
            Preconditions.checkState(canCreateFrom(o));
            return new NDArrayTests.TestNDArray((TestNDArrayObject)o);
        }
    }

    public static class TNDToSerializedNDArray implements NDArrayConverter {
        @Override
        public boolean canConvert(NDArray from, NDArrayFormat<?> to) {
            return false;
        }

        @Override
        public boolean canConvert(NDArray from, Class<?> to) {
            return from.get().getClass() == TestNDArrayObject.class && to == SerializedNDArray.class;
        }

        @Override
        public <T> T convert(NDArray from, NDArrayFormat<T> to) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T convert(NDArray from, Class<T> to) {
            Preconditions.checkState(canConvert(from, to));
            TestNDArrayObject arr = (TestNDArrayObject) from.get();
            float[] fArr = arr.getF();
            ByteBuffer bb = ByteBuffer.allocateDirect(fArr.length * 4);
            bb.asFloatBuffer().put(fArr);
            return (T) new SerializedNDArray(NDArrayType.FLOAT, new long[]{fArr.length}, bb);
        }
    }

    public static class SerializedNDArrayToTND implements NDArrayConverter {

        @Override
        public boolean canConvert(NDArray from, NDArrayFormat<?> to) {
            return false;
        }

        @Override
        public boolean canConvert(NDArray from, Class<?> to) {
            return to == TestNDArrayObject.class && from.get().getClass() == SerializedNDArray.class;
        }

        @Override
        public <T> T convert(NDArray from, NDArrayFormat<T> to) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T convert(NDArray from, Class<T> to) {
            Preconditions.checkState(canConvert(from, to));
            SerializedNDArray s = (SerializedNDArray)from.get();
            FloatBuffer fb = s.getBuffer().asFloatBuffer();
            int len = fb.capacity();
            float[] out = new float[len];
            fb.get(out);
            return (T) new TestNDArrayObject(out);
        }
    }
    
}
