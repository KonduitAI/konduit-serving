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

import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.data.NDArrayType;
import ai.konduit.serving.pipeline.impl.data.ndarray.SerializedNDArray;
import ai.konduit.serving.pipeline.api.format.NDArrayConverter;
import ai.konduit.serving.pipeline.api.format.NDArrayFormat;
import lombok.AllArgsConstructor;
import org.nd4j.common.base.Preconditions;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class JavaNDArrayConverters {

    private JavaNDArrayConverters(){ }


    public static class IdentityConverter implements NDArrayConverter {


        @Override
        public boolean canConvert(NDArray from, NDArrayFormat<?> to) {
            return false;
        }

        @Override
        public boolean canConvert(NDArray from, Class<?> to) {
            return to.isAssignableFrom(from.get().getClass());
        }

        @Override
        public <T> T convert(NDArray from, NDArrayFormat<T> to) {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public <T> T convert(NDArray from, Class<T> to) {
            Preconditions.checkState(canConvert(from, to), "Unable to convert %s to %s", from.get().getClass(), to);
            return (T)from.get();
        }
    }



    public static class FloatToSerializedConverter implements NDArrayConverter {

        @Override
        public boolean canConvert(NDArray from, NDArrayFormat to) {
            return false;
        }

        @Override
        public boolean canConvert(NDArray from, Class<?> to) {
            if(SerializedNDArray.class == to){
                Object o = from.get();
                return o instanceof float[] ||
                        o instanceof float[][] ||
                        o instanceof float[][][] ||
                        o instanceof float[][][][] ||
                        o instanceof float[][][][][];
            }
            return false;
        }

        @Override
        public <T> T convert(NDArray from, NDArrayFormat<T> to) {
            throw new UnsupportedOperationException("Not supported: conversion to " + to);
        }

        @Override
        public <T> T convert(NDArray from, Class<T> to) {
            Preconditions.checkState(canConvert(from, to), "Not able to convert: %s to %s", from, to);

            Object o = from.get();
            long[] shape = shape(o);
            ByteBuffer bb = flatten(o);


            return (T)new SerializedNDArray(NDArrayType.FLOAT, shape, bb);
        }

        private ByteBuffer flatten(Object o){
            long[] shape = shape(o);
            long prod = shape.length == 0 ? 0 : 1;
            for(long l : shape)
                prod *= l;

            long bufferLength = prod * 4L;  //Float = 4 bytes per element
            Preconditions.checkState(prod < Integer.MAX_VALUE, "More than 2 billion bytes in Java float array - unable to convert to SerializedNDArray");

            ByteBuffer bb = ByteBuffer.allocateDirect((int)bufferLength).order(ByteOrder.LITTLE_ENDIAN);
            FloatBuffer fb = bb.asFloatBuffer();

            int rank = rank(o);
            switch (rank){
                case 1:
                    fb.put((float[])o);
                    break;
                case 2:
                    put((float[][])o, fb);
                    break;
                case 3:
                    put((float[][][])o, fb);
                    break;
                case 4:
                    put((float[][][][])o, fb);
                    break;
                case 5:
                    put((float[][][][][])o, fb);
                    break;
                default:
                    throw new IllegalStateException("Unable to convert: " + o.getClass());
            }
            return bb;
        }

        private int rank(Object o){
            if(o instanceof float[]) {
                return 1;
            } else if(o instanceof float[][]) {
                return 2;
            } else if(o instanceof float[][][]) {
                return 3;
            } else if(o instanceof float[][][][]) {
                return 4;
            } else if(o instanceof float[][][][][]) {
                return 5;
            }
            throw new UnsupportedOperationException();
        }

        private long[] shape(Object o) {
            int rank = rank(o);
            if(rank == 1) {
                float[] f = (float[])o;
                return new long[]{f.length};
            } else if(o instanceof float[][]) {
                float[][] f = (float[][])o;
                return new long[]{f.length, f[0].length};
            } else if(o instanceof float[][][]) {
                float[][][] f3 = (float[][][])o;
                return new long[]{f3.length, f3[0].length, f3[0][0].length};
            } else if(o instanceof float[][][][]) {
                float[][][][] f4 = (float[][][][])o;
                return new long[]{f4.length, f4[0].length, f4[0][0].length, f4[0][0][0].length};
            } else if(o instanceof float[][][][][]) {
                float[][][][][] f5 = (float[][][][][])o;
                return new long[]{f5.length, f5[0].length, f5[0][0].length, f5[0][0][0].length, f5[0][0][0][0].length};
            }
            throw new UnsupportedOperationException("All values are null");
        }

        private void put(float[][] toAdd, FloatBuffer fb){
            for( int i=0; i<toAdd.length; i++ ){
                fb.put(toAdd[i]);
            }
        }

        private void put(float[][][] toAdd, FloatBuffer fb){
            for(int i=0; i<toAdd.length; i++ ){
                put(toAdd[i], fb);
            }
        }

        private void put(float[][][][] toAdd, FloatBuffer fb){
            for(int i=0; i<toAdd.length; i++ ){
                put(toAdd[i], fb);
            }
        }

        private void put(float[][][][][] toAdd, FloatBuffer fb){
            for(int i=0; i<toAdd.length; i++ ){
                put(toAdd[i], fb);
            }
        }
    }

    @AllArgsConstructor
    protected abstract static class BaseS2FConverter implements NDArrayConverter {
        private Class<?> c;
        private int rank;

        @Override
        public boolean canConvert(NDArray from, NDArrayFormat to) {
            return false;
        }

        @Override
        public <T> T convert(NDArray from, NDArrayFormat<T> to) {
            throw new UnsupportedOperationException("Not supported: Conversion to " + to);
        }

        @Override
        public boolean canConvert(NDArray from, Class<?> to) {
            if(to != c)
                return false;
            if(!(from.get() instanceof SerializedNDArray)){
                return false;
            }
            SerializedNDArray arr = (SerializedNDArray)from.get();
            if(arr.getShape().length != rank)
                return false;
            //TODO do we allow type conversion? Float -> Double etc?
            return arr.getType() == NDArrayType.FLOAT;
        }

        @Override
        public <T> T convert(NDArray from, Class<T> to) {
            Preconditions.checkState(canConvert(from, to), "Unable to convert to format: %s", to);
            return doConversion(from, to);
        }

        protected abstract <T> T doConversion(NDArray from, Class<T> to);
    }

    public static class SerializedToFloat1Converter extends BaseS2FConverter {
        public SerializedToFloat1Converter() {
            super(float[].class, 1);
        }

        @Override
        protected  <T> T doConversion(NDArray from, Class<T> to){
            SerializedNDArray sa = (SerializedNDArray) from.get();
            sa.getBuffer().rewind();
            FloatBuffer fb = sa.getBuffer().asFloatBuffer();
            float[] out = new float[fb.remaining()];
            fb.get(out);
            return (T) out;
        }
    }

    public static class SerializedToFloat2Converter extends BaseS2FConverter {
        public SerializedToFloat2Converter() {
            super(float[][].class, 2);
        }

        @Override
        protected <T> T doConversion(NDArray from, Class<T> to){
            SerializedNDArray sa = (SerializedNDArray) from.get();
            sa.getBuffer().rewind();
            FloatBuffer fb = sa.getBuffer().asFloatBuffer();
            fb.position(0);
            long[] shape = sa.getShape();
            float[][] out = new float[(int) shape[0]][(int) shape[1]];
            for(float[] f : out){
                fb.get(f);
            }
            return (T) out;
        }
    }

    public static class SerializedToFloat3Converter extends BaseS2FConverter {
        public SerializedToFloat3Converter() {
            super(float[][][].class, 3);
        }

        @Override
        protected <T> T doConversion(NDArray from, Class<T> to){
            SerializedNDArray sa = (SerializedNDArray) from.get();
            sa.getBuffer().rewind();
            FloatBuffer fb = sa.getBuffer().asFloatBuffer();
            fb.position(0);
            long[] shape = sa.getShape();
            float[][][] out = new float[(int) shape[0]][(int) shape[1]][(int) shape[2]];
            for(float[][] f : out){
                for(float[] f2 : f) {
                    fb.get(f2);
                }
            }
            return (T) out;
        }
    }

    public static class SerializedToFloat4Converter extends BaseS2FConverter {
        public SerializedToFloat4Converter() {
            super(float[][][][].class, 4);
        }

        @Override
        protected <T> T doConversion(NDArray from, Class<T> to){
            SerializedNDArray sa = (SerializedNDArray) from.get();
            sa.getBuffer().rewind();
            FloatBuffer fb = sa.getBuffer().asFloatBuffer();
            fb.position(0);
            long[] shape = sa.getShape();
            float[][][][] out = new float[(int) shape[0]][(int) shape[1]][(int) shape[2]][(int) shape[3]];
            for(float[][][] f : out){
                for(float[][] f2 : f) {
                    for(float[] f3 : f2) {
                        fb.get(f3);
                    }
                }
            }
            return (T) out;
        }
    }

    public static class SerializedToFloat5Converter extends BaseS2FConverter {
        public SerializedToFloat5Converter() {
            super(float[][][][][].class, 5);
        }

        @Override
        protected <T> T doConversion(NDArray from, Class<T> to){
            SerializedNDArray sa = (SerializedNDArray) from.get();
            sa.getBuffer().rewind();
            FloatBuffer fb = sa.getBuffer().asFloatBuffer();
            fb.position(0);
            long[] shape = sa.getShape();
            float[][][][][] out = new float[(int) shape[0]][(int) shape[1]][(int) shape[2]][(int) shape[3]][(int)shape[4]];
            for(float[][][][] f : out){
                for(float[][][] f2 : f) {
                    for(float[][] f3 : f2) {
                        for(float[] f4 : f3) {
                            fb.get(f4);
                        }
                    }
                }
            }
            return (T) out;
        }
    }
}
