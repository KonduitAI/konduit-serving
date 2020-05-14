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
import ai.konduit.serving.pipeline.api.data.ValueType;
import ai.konduit.serving.pipeline.impl.data.ndarray.SerializedNDArray;
import ai.konduit.serving.pipeline.api.format.NDArrayConverter;
import ai.konduit.serving.pipeline.api.format.NDArrayFormat;
import lombok.AllArgsConstructor;
import org.nd4j.common.base.Preconditions;

import java.nio.*;

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

    public static class DoubleToSerializedConverter implements NDArrayConverter {

        @Override
        public boolean canConvert(NDArray from, NDArrayFormat to) {
            return false;
        }

        @Override
        public boolean canConvert(NDArray from, Class<?> to) {
            if(SerializedNDArray.class == to){
                Object o = from.get();
                return o instanceof double[] ||
                        o instanceof double[][] ||
                        o instanceof double[][][] ||
                        o instanceof double[][][][] ||
                        o instanceof double[][][][][];
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


            return (T)new SerializedNDArray(NDArrayType.DOUBLE, shape, bb);
        }

        private ByteBuffer flatten(Object o){
            long[] shape = shape(o);
            long prod = shape.length == 0 ? 0 : 1;
            for(long l : shape)
                prod *= l;

            long bufferLength = prod * 8L;  //Double = 8 bytes per element
            Preconditions.checkState(prod < Integer.MAX_VALUE, "More than 2 billion bytes in Java float array - unable to convert to SerializedNDArray");

            ByteBuffer bb = ByteBuffer.allocateDirect((int)bufferLength).order(ByteOrder.LITTLE_ENDIAN);
            DoubleBuffer fb = bb.asDoubleBuffer();

            int rank = rank(o);
            switch (rank){
                case 1:
                    fb.put((double[])o);
                    break;
                case 2:
                    put((double[][])o, fb);
                    break;
                case 3:
                    put((double[][][])o, fb);
                    break;
                case 4:
                    put((double[][][][])o, fb);
                    break;
                case 5:
                    put((double[][][][][])o, fb);
                    break;
                default:
                    throw new IllegalStateException("Unable to convert: " + o.getClass());
            }
            return bb;
        }

        private int rank(Object o){
            if(o instanceof double[]) {
                return 1;
            } else if(o instanceof double[][]) {
                return 2;
            } else if(o instanceof double[][][]) {
                return 3;
            } else if(o instanceof double[][][][]) {
                return 4;
            } else if(o instanceof double[][][][][]) {
                return 5;
            }
            throw new UnsupportedOperationException();
        }

        private long[] shape(Object o) {
            int rank = rank(o);
            if(rank == 1) {
                double[] f = (double[])o;
                return new long[]{f.length};
            } else if(o instanceof double[][]) {
                double[][] f = (double[][])o;
                return new long[]{f.length, f[0].length};
            } else if(o instanceof double[][][]) {
                double[][][] f3 = (double[][][])o;
                return new long[]{f3.length, f3[0].length, f3[0][0].length};
            } else if(o instanceof double[][][][]) {
                double[][][][] f4 = (double[][][][])o;
                return new long[]{f4.length, f4[0].length, f4[0][0].length, f4[0][0][0].length};
            } else if(o instanceof double[][][][][]) {
                double[][][][][] f5 = (double[][][][][])o;
                return new long[]{f5.length, f5[0].length, f5[0][0].length, f5[0][0][0].length, f5[0][0][0][0].length};
            }
            throw new UnsupportedOperationException("All values are null");
        }

        private void put(double[][] toAdd, DoubleBuffer db){
            for( int i=0; i<toAdd.length; i++ ){
                db.put(toAdd[i]);
            }
        }

        private void put(double[][][] toAdd, DoubleBuffer db){
            for(int i=0; i<toAdd.length; i++ ){
                put(toAdd[i], db);
            }
        }

        private void put(double[][][][] toAdd, DoubleBuffer db){
            for(int i=0; i<toAdd.length; i++ ){
                put(toAdd[i], db);
            }
        }

        private void put(double[][][][][] toAdd, DoubleBuffer db){
            for(int i=0; i<toAdd.length; i++ ){
                put(toAdd[i], db);
            }
        }
    }

    public static class ByteToSerializedConverter implements NDArrayConverter {

        @Override
        public boolean canConvert(NDArray from, NDArrayFormat to) {
            return false;
        }

        @Override
        public boolean canConvert(NDArray from, Class<?> to) {
            if(SerializedNDArray.class == to){
                Object o = from.get();
                return o instanceof byte[] ||
                        o instanceof byte[][] ||
                        o instanceof byte[][][] ||
                        o instanceof byte[][][][] ||
                        o instanceof byte[][][][][];
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


            return (T)new SerializedNDArray(NDArrayType.INT8, shape, bb);
        }

        private ByteBuffer flatten(Object o){
            long[] shape = shape(o);
            long prod = shape.length == 0 ? 0 : 1;
            for(long l : shape)
                prod *= l;

            long bufferLength = prod * 1L;  //Float = 4 bytes per element
            Preconditions.checkState(prod < Integer.MAX_VALUE, "More than 2 billion bytes in Java float array - unable to convert to SerializedNDArray");

            ByteBuffer bb = ByteBuffer.allocateDirect((int)bufferLength).order(ByteOrder.LITTLE_ENDIAN);
            //ByteBuffer byteBuffer = bb.asReadOnlyBuffer();

            int rank = rank(o);
            switch (rank){
                case 1:
                    bb.put((byte[])o);
                    break;
                case 2:
                    put((byte[][])o, bb);
                    break;
                case 3:
                    put((byte[][][])o, bb);
                    break;
                case 4:
                    put((byte[][][][])o, bb);
                    break;
                case 5:
                    put((byte[][][][][])o, bb);
                    break;
                default:
                    throw new IllegalStateException("Unable to convert: " + o.getClass());
            }
            return bb;
        }

        private int rank(Object o){
            if(o instanceof byte[]) {
                return 1;
            } else if(o instanceof byte[][]) {
                return 2;
            } else if(o instanceof byte[][][]) {
                return 3;
            } else if(o instanceof byte[][][][]) {
                return 4;
            } else if(o instanceof byte[][][][][]) {
                return 5;
            }
            throw new UnsupportedOperationException();
        }

        private long[] shape(Object o) {
            int rank = rank(o);
            if(rank == 1) {
                byte[] f = (byte[])o;
                return new long[]{f.length};
            } else if(o instanceof byte[][]) {
                byte[][] f = (byte[][])o;
                return new long[]{f.length, f[0].length};
            } else if(o instanceof byte[][][]) {
                byte[][][] f3 = (byte[][][])o;
                return new long[]{f3.length, f3[0].length, f3[0][0].length};
            } else if(o instanceof byte[][][][]) {
                byte[][][][] f4 = (byte[][][][])o;
                return new long[]{f4.length, f4[0].length, f4[0][0].length, f4[0][0][0].length};
            } else if(o instanceof byte[][][][][]) {
                byte[][][][][] f5 = (byte[][][][][])o;
                return new long[]{f5.length, f5[0].length, f5[0][0].length, f5[0][0][0].length, f5[0][0][0][0].length};
            }
            throw new UnsupportedOperationException("All values are null");
        }

        private void put(byte[][] toAdd, ByteBuffer byteBuffer){
            for( int i=0; i<toAdd.length; i++ ){
                byteBuffer.put(toAdd[i]);
            }
        }

        private void put(byte[][][] toAdd, ByteBuffer byteBuffer){
            for(int i=0; i<toAdd.length; i++ ){
                put(toAdd[i], byteBuffer);
            }
        }

        private void put(byte[][][][] toAdd, ByteBuffer byteBuffer){
            for(int i=0; i<toAdd.length; i++ ){
                put(toAdd[i], byteBuffer);
            }
        }

        private void put(byte[][][][][] toAdd, ByteBuffer byteBuffer){
            for(int i=0; i<toAdd.length; i++ ){
                put(toAdd[i], byteBuffer);
            }
        }
    }

    public static class ShortToSerializedConverter implements NDArrayConverter {

        @Override
        public boolean canConvert(NDArray from, NDArrayFormat to) {
            return false;
        }

        @Override
        public boolean canConvert(NDArray from, Class<?> to) {
            if(SerializedNDArray.class == to){
                Object o = from.get();
                return o instanceof short[] ||
                        o instanceof short[][] ||
                        o instanceof short[][][] ||
                        o instanceof short[][][][] ||
                        o instanceof short[][][][][];
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


            return (T)new SerializedNDArray(NDArrayType.INT16, shape, bb);
        }

        private ByteBuffer flatten(Object o){
            long[] shape = shape(o);
            long prod = shape.length == 0 ? 0 : 1;
            for(long l : shape)
                prod *= l;

            long bufferLength = prod * 4L;  //Float = 4 bytes per element
            Preconditions.checkState(prod < Integer.MAX_VALUE, "More than 2 billion bytes in Java float array - unable to convert to SerializedNDArray");

            ByteBuffer bb = ByteBuffer.allocateDirect((int)bufferLength).order(ByteOrder.LITTLE_ENDIAN);
            ShortBuffer sb = bb.asShortBuffer();

            int rank = rank(o);
            switch (rank){
                case 1:
                    sb.put((short[])o);
                    break;
                case 2:
                    put((short[][])o, sb);
                    break;
                case 3:
                    put((short[][][])o, sb);
                    break;
                case 4:
                    put((short[][][][])o, sb);
                    break;
                case 5:
                    put((short[][][][][])o, sb);
                    break;
                default:
                    throw new IllegalStateException("Unable to convert: " + o.getClass());
            }
            return bb;
        }

        private int rank(Object o){
            if(o instanceof short[]) {
                return 1;
            } else if(o instanceof short[][]) {
                return 2;
            } else if(o instanceof short[][][]) {
                return 3;
            } else if(o instanceof short[][][][]) {
                return 4;
            } else if(o instanceof short[][][][][]) {
                return 5;
            }
            throw new UnsupportedOperationException();
        }

        private long[] shape(Object o) {
            int rank = rank(o);
            if(rank == 1) {
                short[] f = (short[])o;
                return new long[]{f.length};
            } else if(o instanceof short[][]) {
                short[][] f = (short[][])o;
                return new long[]{f.length, f[0].length};
            } else if(o instanceof short[][][]) {
                short[][][] f3 = (short[][][])o;
                return new long[]{f3.length, f3[0].length, f3[0][0].length};
            } else if(o instanceof short[][][][]) {
                short[][][][] f4 = (short[][][][])o;
                return new long[]{f4.length, f4[0].length, f4[0][0].length, f4[0][0][0].length};
            } else if(o instanceof short[][][][][]) {
                short[][][][][] f5 = (short[][][][][])o;
                return new long[]{f5.length, f5[0].length, f5[0][0].length, f5[0][0][0].length, f5[0][0][0][0].length};
            }
            throw new UnsupportedOperationException("All values are null");
        }

        private void put(short[][] toAdd, ShortBuffer sb){
            for( int i=0; i<toAdd.length; i++ ){
                sb.put(toAdd[i]);
            }
        }

        private void put(short[][][] toAdd, ShortBuffer sb){
            for(int i=0; i<toAdd.length; i++ ){
                put(toAdd[i], sb);
            }
        }

        private void put(short[][][][] toAdd, ShortBuffer sb){
            for(int i=0; i<toAdd.length; i++ ){
                put(toAdd[i], sb);
            }
        }

        private void put(short[][][][][] toAdd, ShortBuffer sb){
            for(int i=0; i<toAdd.length; i++ ){
                put(toAdd[i], sb);
            }
        }
    }

    public static class IntToSerializedConverter implements NDArrayConverter {

        @Override
        public boolean canConvert(NDArray from, NDArrayFormat to) {
            return false;
        }

        @Override
        public boolean canConvert(NDArray from, Class<?> to) {
            if(SerializedNDArray.class == to){
                Object o = from.get();
                return o instanceof int[] ||
                        o instanceof int[][] ||
                        o instanceof int[][][] ||
                        o instanceof int[][][][] ||
                        o instanceof int[][][][][];
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


            return (T)new SerializedNDArray(NDArrayType.INT32, shape, bb);
        }

        private ByteBuffer flatten(Object o){
            long[] shape = shape(o);
            long prod = shape.length == 0 ? 0 : 1;
            for(long l : shape)
                prod *= l;

            long bufferLength = prod * 32L;  //Float = 4 bytes per element
            Preconditions.checkState(prod < Integer.MAX_VALUE, "More than 2 billion bytes in Java float array - unable to convert to SerializedNDArray");

            ByteBuffer bb = ByteBuffer.allocateDirect((int)bufferLength).order(ByteOrder.LITTLE_ENDIAN);
            IntBuffer ib = bb.asIntBuffer();

            int rank = rank(o);
            switch (rank){
                case 1:
                    ib.put((int[])o);
                    break;
                case 2:
                    put((int[][])o, ib);
                    break;
                case 3:
                    put((int[][][])o, ib);
                    break;
                case 4:
                    put((int[][][][])o, ib);
                    break;
                case 5:
                    put((int[][][][][])o, ib);
                    break;
                default:
                    throw new IllegalStateException("Unable to convert: " + o.getClass());
            }
            return bb;
        }

        private int rank(Object o){
            if(o instanceof int[]) {
                return 1;
            } else if(o instanceof int[][]) {
                return 2;
            } else if(o instanceof int[][][]) {
                return 3;
            } else if(o instanceof int[][][][]) {
                return 4;
            } else if(o instanceof int[][][][][]) {
                return 5;
            }
            throw new UnsupportedOperationException();
        }

        private long[] shape(Object o) {
            int rank = rank(o);
            if(rank == 1) {
                int[] f = (int[])o;
                return new long[]{f.length};
            } else if(o instanceof int[][]) {
                int[][] f = (int[][])o;
                return new long[]{f.length, f[0].length};
            } else if(o instanceof int[][][]) {
                int[][][] f3 = (int[][][])o;
                return new long[]{f3.length, f3[0].length, f3[0][0].length};
            } else if(o instanceof int[][][][]) {
                int[][][][] f4 = (int[][][][])o;
                return new long[]{f4.length, f4[0].length, f4[0][0].length, f4[0][0][0].length};
            } else if(o instanceof int[][][][][]) {
                int[][][][][] f5 = (int[][][][][])o;
                return new long[]{f5.length, f5[0].length, f5[0][0].length, f5[0][0][0].length, f5[0][0][0][0].length};
            }
            throw new UnsupportedOperationException("All values are null");
        }

        private void put(int[][] toAdd, IntBuffer ib){
            for( int i=0; i<toAdd.length; i++ ){
                ib.put(toAdd[i]);
            }
        }

        private void put(int[][][] toAdd, IntBuffer ib){
            for(int i=0; i<toAdd.length; i++ ){
                put(toAdd[i], ib);
            }
        }

        private void put(int[][][][] toAdd, IntBuffer ib){
            for(int i=0; i<toAdd.length; i++ ){
                put(toAdd[i], ib);
            }
        }

        private void put(int[][][][][] toAdd, IntBuffer ib){
            for(int i=0; i<toAdd.length; i++ ){
                put(toAdd[i], ib);
            }
        }
    }

    public static class LongToSerializedConverter implements NDArrayConverter {

        @Override
        public boolean canConvert(NDArray from, NDArrayFormat to) {
            return false;
        }

        @Override
        public boolean canConvert(NDArray from, Class<?> to) {
            if(SerializedNDArray.class == to){
                Object o = from.get();
                return o instanceof long[] ||
                        o instanceof long[][] ||
                        o instanceof long[][][] ||
                        o instanceof long[][][][] ||
                        o instanceof long[][][][][];
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


            return (T)new SerializedNDArray(NDArrayType.INT64, shape, bb);
        }

        private ByteBuffer flatten(Object o){
            long[] shape = shape(o);
            long prod = shape.length == 0 ? 0 : 1;
            for(long l : shape)
                prod *= l;

            long bufferLength = prod * 64L;  //Float = 4 bytes per element
            Preconditions.checkState(prod < Integer.MAX_VALUE, "More than 2 billion bytes in Java float array - unable to convert to SerializedNDArray");

            ByteBuffer bb = ByteBuffer.allocateDirect((int)bufferLength).order(ByteOrder.LITTLE_ENDIAN);
            LongBuffer lb = bb.asLongBuffer();

            int rank = rank(o);
            switch (rank){
                case 1:
                    lb.put((long[])o);
                    break;
                case 2:
                    put((long[][])o, lb);
                    break;
                case 3:
                    put((long[][][])o, lb);
                    break;
                case 4:
                    put((long[][][][])o, lb);
                    break;
                case 5:
                    put((long[][][][][])o, lb);
                    break;
                default:
                    throw new IllegalStateException("Unable to convert: " + o.getClass());
            }
            return bb;
        }

        private int rank(Object o){
            if(o instanceof long[]) {
                return 1;
            } else if(o instanceof long[][]) {
                return 2;
            } else if(o instanceof long[][][]) {
                return 3;
            } else if(o instanceof long[][][][]) {
                return 4;
            } else if(o instanceof long[][][][][]) {
                return 5;
            }
            throw new UnsupportedOperationException();
        }

        private long[] shape(Object o) {
            int rank = rank(o);
            if(rank == 1) {
                long[] f = (long[])o;
                return new long[]{f.length};
            } else if(o instanceof long[][]) {
                long[][] f = (long[][])o;
                return new long[]{f.length, f[0].length};
            } else if(o instanceof long[][][]) {
                long[][][] f3 = (long[][][])o;
                return new long[]{f3.length, f3[0].length, f3[0][0].length};
            } else if(o instanceof long[][][][]) {
                long[][][][] f4 = (long[][][][])o;
                return new long[]{f4.length, f4[0].length, f4[0][0].length, f4[0][0][0].length};
            } else if(o instanceof long[][][][][]) {
                long[][][][][] f5 = (long[][][][][])o;
                return new long[]{f5.length, f5[0].length, f5[0][0].length, f5[0][0][0].length, f5[0][0][0][0].length};
            }
            throw new UnsupportedOperationException("All values are null");
        }

        private void put(long[][] toAdd, LongBuffer lb){
            for( int i=0; i<toAdd.length; i++ ){
                lb.put(toAdd[i]);
            }
        }

        private void put(long[][][] toAdd, LongBuffer lb){
            for(int i=0; i<toAdd.length; i++ ){
                put(toAdd[i], lb);
            }
        }

        private void put(long[][][][] toAdd, LongBuffer lb){
            for(int i=0; i<toAdd.length; i++ ){
                put(toAdd[i], lb);
            }
        }

        private void put(long[][][][][] toAdd, LongBuffer lb){
            for(int i=0; i<toAdd.length; i++ ){
                put(toAdd[i], lb);
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
            return arr.getType() == sourceType();
        }

        abstract protected NDArrayType sourceType();

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

        protected NDArrayType sourceType() { return NDArrayType.FLOAT; }
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

        protected NDArrayType sourceType() { return NDArrayType.FLOAT; }
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

        protected NDArrayType sourceType() { return NDArrayType.FLOAT; }
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

        protected NDArrayType sourceType() { return NDArrayType.FLOAT; }
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

        protected NDArrayType sourceType() { return NDArrayType.FLOAT; }
    }

    public static class SerializedToDouble1Converter extends BaseS2FConverter {
        public SerializedToDouble1Converter() {
            super(double[].class, 1);
        }

        @Override
        protected  <T> T doConversion(NDArray from, Class<T> to){
            SerializedNDArray sa = (SerializedNDArray) from.get();
            sa.getBuffer().rewind();
            DoubleBuffer db = sa.getBuffer().asDoubleBuffer();
            double[] out = new double[db.remaining()];
            db.get(out);
            return (T) out;
        }

        protected NDArrayType sourceType() { return NDArrayType.DOUBLE; }
    }

    public static class SerializedToDouble2Converter extends BaseS2FConverter {
        public SerializedToDouble2Converter() {
            super(double[][].class, 2);
        }

        @Override
        protected <T> T doConversion(NDArray from, Class<T> to){
            SerializedNDArray sa = (SerializedNDArray) from.get();
            sa.getBuffer().rewind();
            DoubleBuffer fb = sa.getBuffer().asDoubleBuffer();
            fb.position(0);
            long[] shape = sa.getShape();
            double[][] out = new double[(int) shape[0]][(int) shape[1]];
            for(double[] f : out){
                fb.get(f);
            }
            return (T) out;
        }

        protected NDArrayType sourceType() { return NDArrayType.DOUBLE; }
    }

    public static class SerializedToDouble3Converter extends BaseS2FConverter {
        public SerializedToDouble3Converter() {
            super(double[][][].class, 3);
        }

        @Override
        protected <T> T doConversion(NDArray from, Class<T> to){
            SerializedNDArray sa = (SerializedNDArray) from.get();
            sa.getBuffer().rewind();
            DoubleBuffer db = sa.getBuffer().asDoubleBuffer();
            db.position(0);
            long[] shape = sa.getShape();
            double[][][] out = new double[(int) shape[0]][(int) shape[1]][(int) shape[2]];
            for(double[][] f : out){
                for(double[] f2 : f) {
                    db.get(f2);
                }
            }
            return (T) out;
        }

        protected NDArrayType sourceType() { return NDArrayType.DOUBLE; }
    }

    public static class SerializedToDouble4Converter extends BaseS2FConverter {
        public SerializedToDouble4Converter() {
            super(double[][][][].class, 4);
        }

        @Override
        protected <T> T doConversion(NDArray from, Class<T> to){
            SerializedNDArray sa = (SerializedNDArray) from.get();
            sa.getBuffer().rewind();
            DoubleBuffer db = sa.getBuffer().asDoubleBuffer();
            db.position(0);
            long[] shape = sa.getShape();
            double[][][][] out = new double[(int) shape[0]][(int) shape[1]][(int) shape[2]][(int) shape[3]];
            for(double[][][] f : out){
                for(double[][] f2 : f) {
                    for(double[] f3 : f2) {
                        db.get(f3);
                    }
                }
            }
            return (T) out;
        }

        protected NDArrayType sourceType() { return NDArrayType.DOUBLE; }
    }

    public static class SerializedToDouble5Converter extends BaseS2FConverter {
        public SerializedToDouble5Converter() {
            super(double[][][][][].class, 5);
        }

        @Override
        protected <T> T doConversion(NDArray from, Class<T> to){
            SerializedNDArray sa = (SerializedNDArray) from.get();
            sa.getBuffer().rewind();
            DoubleBuffer db = sa.getBuffer().asDoubleBuffer();
            db.position(0);
            long[] shape = sa.getShape();
            double[][][][][] out = new double[(int) shape[0]][(int) shape[1]][(int) shape[2]][(int) shape[3]][(int)shape[4]];
            for(double[][][][] f : out){
                for(double[][][] f2 : f) {
                    for(double[][] f3 : f2) {
                        for(double[] f4 : f3) {
                            db.get(f4);
                        }
                    }
                }
            }
            return (T) out;
        }

        protected NDArrayType sourceType() { return NDArrayType.DOUBLE; }
    }

    public static class SerializedToByte1Converter extends BaseS2FConverter {
        public SerializedToByte1Converter() {
            super(byte[].class, 1);
        }

        @Override
        protected  <T> T doConversion(NDArray from, Class<T> to){
            SerializedNDArray sa = (SerializedNDArray) from.get();
            sa.getBuffer().rewind();
            ByteBuffer byteBuffer = sa.getBuffer().asReadOnlyBuffer();
            byte[] out = new byte[byteBuffer.remaining()];
            byteBuffer.get(out);
            return (T) out;
        }

        protected NDArrayType sourceType() { return NDArrayType.INT8; }
    }

    public static class SerializedToByte2Converter extends BaseS2FConverter {
        public SerializedToByte2Converter() {
            super(byte[][].class, 2);
        }

        @Override
        protected <T> T doConversion(NDArray from, Class<T> to){
            SerializedNDArray sa = (SerializedNDArray) from.get();
            sa.getBuffer().rewind();
            ByteBuffer byteBuffer = sa.getBuffer().asReadOnlyBuffer();
            byteBuffer.position(0);
            long[] shape = sa.getShape();
            byte[][] out = new byte[(int) shape[0]][(int) shape[1]];
            for(byte[] f : out){
                byteBuffer.get(f);
            }
            return (T) out;
        }

        protected NDArrayType sourceType() { return NDArrayType.INT8; }
    }

    public static class SerializedToByte3Converter extends BaseS2FConverter {
        public SerializedToByte3Converter() {
            super(byte[][][].class, 3);
        }

        @Override
        protected <T> T doConversion(NDArray from, Class<T> to){
            SerializedNDArray sa = (SerializedNDArray) from.get();
            sa.getBuffer().rewind();
            ByteBuffer byteBuffer = sa.getBuffer().asReadOnlyBuffer();
            byteBuffer.position(0);
            long[] shape = sa.getShape();
            byte[][][] out = new byte[(int) shape[0]][(int) shape[1]][(int) shape[2]];
            for(byte[][] f : out){
                for(byte[] f2 : f) {
                    byteBuffer.get(f2);
                }
            }
            return (T) out;
        }

        protected NDArrayType sourceType() { return NDArrayType.INT8; }
    }

    public static class SerializedToByte4Converter extends BaseS2FConverter {
        public SerializedToByte4Converter() {
            super(byte[][][][].class, 4);
        }

        @Override
        protected <T> T doConversion(NDArray from, Class<T> to){
            SerializedNDArray sa = (SerializedNDArray) from.get();
            sa.getBuffer().rewind();
            ByteBuffer byteBuffer = sa.getBuffer().asReadOnlyBuffer();
            byteBuffer.position(0);
            long[] shape = sa.getShape();
            byte[][][][] out = new byte[(int) shape[0]][(int) shape[1]][(int) shape[2]][(int) shape[3]];
            for(byte[][][] f : out){
                for(byte[][] f2 : f) {
                    for(byte[] f3 : f2) {
                        byteBuffer.get(f3);
                    }
                }
            }
            return (T) out;
        }

        protected NDArrayType sourceType() { return NDArrayType.INT8; }
    }

    public static class SerializedToByte5Converter extends BaseS2FConverter {
        public SerializedToByte5Converter() {
            super(byte[][][][][].class, 5);
        }

        @Override
        protected <T> T doConversion(NDArray from, Class<T> to){
            SerializedNDArray sa = (SerializedNDArray) from.get();
            sa.getBuffer().rewind();
            ByteBuffer byteBuffer = sa.getBuffer().asReadOnlyBuffer();
            byteBuffer.position(0);
            long[] shape = sa.getShape();
            byte[][][][][] out = new byte[(int) shape[0]][(int) shape[1]][(int) shape[2]][(int) shape[3]][(int)shape[4]];
            for(byte[][][][] f : out){
                for(byte[][][] f2 : f) {
                    for(byte[][] f3 : f2) {
                        for(byte[] f4 : f3) {
                            byteBuffer.get(f4);
                        }
                    }
                }
            }
            return (T) out;
        }

        protected NDArrayType sourceType() { return NDArrayType.INT8; }
    }

    public static class SerializedToShort1Converter extends BaseS2FConverter {
        public SerializedToShort1Converter() {
            super(short[].class, 1);
        }

        @Override
        protected  <T> T doConversion(NDArray from, Class<T> to){
            SerializedNDArray sa = (SerializedNDArray) from.get();
            sa.getBuffer().rewind();
            ShortBuffer sb = sa.getBuffer().asShortBuffer();
            short[] out = new short[sb.remaining()];
            sb.get(out);
            return (T) out;
        }

        protected NDArrayType sourceType() { return NDArrayType.INT16; }
    }

    public static class SerializedToShort2Converter extends BaseS2FConverter {
        public SerializedToShort2Converter() {
            super(short[][].class, 2);
        }

        @Override
        protected <T> T doConversion(NDArray from, Class<T> to){
            SerializedNDArray sa = (SerializedNDArray) from.get();
            sa.getBuffer().rewind();
            ShortBuffer sb = sa.getBuffer().asShortBuffer();
            sb.position(0);
            long[] shape = sa.getShape();
            short[][] out = new short[(int) shape[0]][(int) shape[1]];
            for(short[] f : out){
                sb.get(f);
            }
            return (T) out;
        }

        protected NDArrayType sourceType() { return NDArrayType.INT16; }
    }

    public static class SerializedToShort3Converter extends BaseS2FConverter {
        public SerializedToShort3Converter() {
            super(short[][][].class, 3);
        }

        @Override
        protected <T> T doConversion(NDArray from, Class<T> to){
            SerializedNDArray sa = (SerializedNDArray) from.get();
            sa.getBuffer().rewind();
            ShortBuffer sb = sa.getBuffer().asShortBuffer();
            sb.position(0);
            long[] shape = sa.getShape();
            short[][][] out = new short[(int) shape[0]][(int) shape[1]][(int) shape[2]];
            for(short[][] f : out){
                for(short[] f2 : f) {
                    sb.get(f2);
                }
            }
            return (T) out;
        }

        protected NDArrayType sourceType() { return NDArrayType.INT16; }
    }

    public static class SerializedToShort4Converter extends BaseS2FConverter {
        public SerializedToShort4Converter() {
            super(short[][][][].class, 4);
        }

        @Override
        protected <T> T doConversion(NDArray from, Class<T> to){
            SerializedNDArray sa = (SerializedNDArray) from.get();
            sa.getBuffer().rewind();
            ShortBuffer sb = sa.getBuffer().asShortBuffer();
            sb.position(0);
            long[] shape = sa.getShape();
            short[][][][] out = new short[(int) shape[0]][(int) shape[1]][(int) shape[2]][(int) shape[3]];
            for(short[][][] f : out){
                for(short[][] f2 : f) {
                    for(short[] f3 : f2) {
                        sb.get(f3);
                    }
                }
            }
            return (T) out;
        }

        protected NDArrayType sourceType() { return NDArrayType.INT16; }
    }

    public static class SerializedToShort5Converter extends BaseS2FConverter {
        public SerializedToShort5Converter() {
            super(short[][][][][].class, 5);
        }

        @Override
        protected <T> T doConversion(NDArray from, Class<T> to){
            SerializedNDArray sa = (SerializedNDArray) from.get();
            sa.getBuffer().rewind();
            ShortBuffer sb = sa.getBuffer().asShortBuffer();
            sb.position(0);
            long[] shape = sa.getShape();
            short[][][][][] out = new short[(int) shape[0]][(int) shape[1]][(int) shape[2]][(int) shape[3]][(int)shape[4]];
            for(short[][][][] f : out){
                for(short[][][] f2 : f) {
                    for(short[][] f3 : f2) {
                        for(short[] f4 : f3) {
                            sb.get(f4);
                        }
                    }
                }
            }
            return (T) out;
        }

        protected NDArrayType sourceType() { return NDArrayType.INT16; }
    }

    public static class SerializedToInt1Converter extends BaseS2FConverter {
        public SerializedToInt1Converter() {
            super(int[].class, 1);
        }

        @Override
        protected  <T> T doConversion(NDArray from, Class<T> to){
            SerializedNDArray sa = (SerializedNDArray) from.get();
            sa.getBuffer().rewind();
            IntBuffer ib = sa.getBuffer().asIntBuffer();
            int[] out = new int[ib.remaining()];
            ib.get(out);
            return (T) out;
        }

        protected NDArrayType sourceType() { return NDArrayType.INT32; }
    }

    public static class SerializedToInt2Converter extends BaseS2FConverter {
        public SerializedToInt2Converter() {
            super(int[][].class, 2);
        }

        @Override
        protected <T> T doConversion(NDArray from, Class<T> to){
            SerializedNDArray sa = (SerializedNDArray) from.get();
            sa.getBuffer().rewind();
            IntBuffer ib = sa.getBuffer().asIntBuffer();
            ib.position(0);
            long[] shape = sa.getShape();
            int[][] out = new int[(int) shape[0]][(int) shape[1]];
            for(int[] f : out){
                ib.get(f);
            }
            return (T) out;
        }

        protected NDArrayType sourceType() { return NDArrayType.INT32; }
    }

    public static class SerializedToInt3Converter extends BaseS2FConverter {
        public SerializedToInt3Converter() {
            super(int[][][].class, 3);
        }

        @Override
        protected <T> T doConversion(NDArray from, Class<T> to){
            SerializedNDArray sa = (SerializedNDArray) from.get();
            sa.getBuffer().rewind();
            IntBuffer ib = sa.getBuffer().asIntBuffer();
            ib.position(0);
            long[] shape = sa.getShape();
            int[][][] out = new int[(int) shape[0]][(int) shape[1]][(int) shape[2]];
            for(int[][] f : out){
                for(int[] f2 : f) {
                    ib.get(f2);
                }
            }
            return (T) out;
        }

        protected NDArrayType sourceType() { return NDArrayType.INT32; }
    }

    public static class SerializedToInt4Converter extends BaseS2FConverter {
        public SerializedToInt4Converter() {
            super(int[][][][].class, 4);
        }

        @Override
        protected <T> T doConversion(NDArray from, Class<T> to){
            SerializedNDArray sa = (SerializedNDArray) from.get();
            sa.getBuffer().rewind();
            IntBuffer ib = sa.getBuffer().asIntBuffer();
            ib.position(0);
            long[] shape = sa.getShape();
            int[][][][] out = new int[(int) shape[0]][(int) shape[1]][(int) shape[2]][(int) shape[3]];
            for(int[][][] f : out){
                for(int[][] f2 : f) {
                    for(int[] f3 : f2) {
                        ib.get(f3);
                    }
                }
            }
            return (T) out;
        }

        protected NDArrayType sourceType() { return NDArrayType.INT32; }
    }

    public static class SerializedToInt5Converter extends BaseS2FConverter {
        public SerializedToInt5Converter() {
            super(int[][][][][].class, 5);
        }

        @Override
        protected <T> T doConversion(NDArray from, Class<T> to){
            SerializedNDArray sa = (SerializedNDArray) from.get();
            sa.getBuffer().rewind();
            IntBuffer ib = sa.getBuffer().asIntBuffer();
            ib.position(0);
            long[] shape = sa.getShape();
            int[][][][][] out = new int[(int) shape[0]][(int) shape[1]][(int) shape[2]][(int) shape[3]][(int)shape[4]];
            for(int[][][][] f : out){
                for(int[][][] f2 : f) {
                    for(int[][] f3 : f2) {
                        for(int[] f4 : f3) {
                            ib.get(f4);
                        }
                    }
                }
            }
            return (T) out;
        }

        protected NDArrayType sourceType() { return NDArrayType.INT32; }
    }

    public static class SerializedToLong1Converter extends BaseS2FConverter {
        public SerializedToLong1Converter() {
            super(long[].class, 1);
        }

        @Override
        protected  <T> T doConversion(NDArray from, Class<T> to){
            SerializedNDArray sa = (SerializedNDArray) from.get();
            sa.getBuffer().rewind();
            LongBuffer lb = sa.getBuffer().asLongBuffer();
            long[] out = new long[lb.remaining()];
            lb.get(out);
            return (T) out;
        }

        protected NDArrayType sourceType() { return NDArrayType.INT64; }
    }

    public static class SerializedToLong2Converter extends BaseS2FConverter {
        public SerializedToLong2Converter() {
            super(long[][].class, 2);
        }

        @Override
        protected <T> T doConversion(NDArray from, Class<T> to){
            SerializedNDArray sa = (SerializedNDArray) from.get();
            sa.getBuffer().rewind();
            LongBuffer lb = sa.getBuffer().asLongBuffer();
            lb.position(0);
            long[] shape = sa.getShape();
            long[][] out = new long[(int) shape[0]][(int) shape[1]];
            for(long[] f : out){
                lb.get(f);
            }
            return (T) out;
        }

        protected NDArrayType sourceType() { return NDArrayType.INT64; }
    }

    public static class SerializedToLong3Converter extends BaseS2FConverter {
        public SerializedToLong3Converter() {
            super(long[][][].class, 3);
        }

        @Override
        protected <T> T doConversion(NDArray from, Class<T> to){
            SerializedNDArray sa = (SerializedNDArray) from.get();
            sa.getBuffer().rewind();
            LongBuffer lb = sa.getBuffer().asLongBuffer();
            lb.position(0);
            long[] shape = sa.getShape();
            long[][][] out = new long[(int) shape[0]][(int) shape[1]][(int) shape[2]];
            for(long[][] f : out){
                for(long[] f2 : f) {
                    lb.get(f2);
                }
            }
            return (T) out;
        }

        protected NDArrayType sourceType() { return NDArrayType.INT64; }
    }

    public static class SerializedToLong4Converter extends BaseS2FConverter {
        public SerializedToLong4Converter() {
            super(long[][][][].class, 4);
        }

        @Override
        protected <T> T doConversion(NDArray from, Class<T> to){
            SerializedNDArray sa = (SerializedNDArray) from.get();
            sa.getBuffer().rewind();
            LongBuffer lb = sa.getBuffer().asLongBuffer();
            lb.position(0);
            long[] shape = sa.getShape();
            long[][][][] out = new long[(int) shape[0]][(int) shape[1]][(int) shape[2]][(int) shape[3]];
            for(long[][][] f : out){
                for(long[][] f2 : f) {
                    for(long[] f3 : f2) {
                        lb.get(f3);
                    }
                }
            }
            return (T) out;
        }

        protected NDArrayType sourceType() { return NDArrayType.INT64; }
    }

    public static class SerializedToLong5Converter extends BaseS2FConverter {
        public SerializedToLong5Converter() {
            super(long[][][][][].class, 5);
        }

        @Override
        protected <T> T doConversion(NDArray from, Class<T> to){
            SerializedNDArray sa = (SerializedNDArray) from.get();
            sa.getBuffer().rewind();
            LongBuffer lb = sa.getBuffer().asLongBuffer();
            lb.position(0);
            long[] shape = sa.getShape();
            long[][][][][] out = new long[(int) shape[0]][(int) shape[1]][(int) shape[2]][(int) shape[3]][(int)shape[4]];
            for(long[][][][] f : out){
                for(long[][][] f2 : f) {
                    for(long[][] f3 : f2) {
                        for(long[] f4 : f3) {
                            lb.get(f4);
                        }
                    }
                }
            }
            return (T) out;
        }

        protected NDArrayType sourceType() { return NDArrayType.INT64; }
    }
}
