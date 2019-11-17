
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

package ai.konduit.serving.util.python;


/**
 * Wrapper around INDArray for initializing from numpy array
 *
 * @author Fariz Rahman
 */
@lombok.Getter
@lombok.NoArgsConstructor
public class NumpyArray {

    private static org.nd4j.nativeblas.NativeOps nativeOps;
    private long address;
    private long[] shape;
    private long[] strides;
    private org.nd4j.linalg.api.buffer.DataType dtype;
    private org.nd4j.linalg.api.ndarray.INDArray nd4jArray;
    static {
        //ensure nd4j is initialized before use
        org.nd4j.linalg.factory.Nd4j.scalar(1.0);
        nativeOps =   org.nd4j.nativeblas.NativeOpsHolder.getInstance().getDeviceNativeOps();
    }

    @lombok.Builder
    public NumpyArray(long address, long[] shape, long strides[], boolean copy, org.nd4j.linalg.api.buffer.DataType dtype) {
        this.address = address;
        this.shape = shape;
        this.strides = strides;
        this.dtype = dtype;
        setND4JArray();
        if (copy){
            nd4jArray = nd4jArray.dup();
            this.address = nd4jArray.data().address();

        }
    }

    public ai.konduit.serving.util.python.NumpyArray copy(){
        return new ai.konduit.serving.util.python.NumpyArray(nd4jArray.dup());
    }

    public NumpyArray(long address, long[] shape, long strides[]){
        this(address, shape, strides, false, org.nd4j.linalg.api.buffer.DataType.FLOAT);
    }

    public NumpyArray(long address, long[] shape, long strides[], org.nd4j.linalg.api.buffer.DataType dtype){
        this(address, shape, strides, dtype, false);
    }

    public NumpyArray(long address, long[] shape, long strides[], org.nd4j.linalg.api.buffer.DataType dtype, boolean copy){
        this.address = address;
        this.shape = shape;
        this.strides = strides;
        this.dtype = dtype;
        setND4JArray();
        if (copy){
            nd4jArray = nd4jArray.dup();
            this.address = nd4jArray.data().address();
        }
    }

    private void setND4JArray() {
        long size = 1;
        for(long d: shape) {
            size *= d;
        }
        org.bytedeco.javacpp.Pointer ptr = nativeOps.pointerForAddress(address);
        ptr = ptr.limit(size);
        ptr = ptr.capacity(size);
        org.nd4j.linalg.api.buffer.DataBuffer buff = org.nd4j.linalg.factory.Nd4j.createBuffer(ptr, size, dtype);
        int elemSize = buff.getElementSize();
        long[] nd4jStrides = java.util.Arrays.stream(strides).map(stride -> stride / elemSize).toArray();
        this.nd4jArray = org.nd4j.linalg.factory.Nd4j.create(buff, shape, nd4jStrides, 0, org.nd4j.linalg.api.shape.Shape.getOrder(shape,nd4jStrides,1), dtype);

    }

    public NumpyArray(org.nd4j.linalg.api.ndarray.INDArray nd4jArray) {
        org.nd4j.linalg.api.buffer.DataBuffer buff = nd4jArray.data();
        address = buff.pointer().address();
        shape = nd4jArray.shape();
        long[] nd4jStrides = nd4jArray.stride();
        int elemSize = buff.getElementSize();
        strides = java.util.Arrays.stream(nd4jStrides).map(stride -> stride * elemSize).toArray();
        dtype = nd4jArray.dataType();
        this.nd4jArray = nd4jArray;
    }

}