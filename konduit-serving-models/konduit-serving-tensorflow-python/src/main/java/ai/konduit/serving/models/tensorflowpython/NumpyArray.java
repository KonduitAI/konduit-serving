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

package ai.konduit.serving.models.tensorflowpython;

import ai.konduit.serving.pipeline.api.data.NDArrayType;
import ai.konduit.serving.pipeline.api.format.NDArrayFactory;
import ai.konduit.serving.pipeline.impl.data.ndarray.BaseNDArray;
import org.nd4j.python4j.PythonGC;
import org.nd4j.python4j.PythonObject;
import org.nd4j.python4j.PythonTypes;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NumpyArray {

    private final PythonObject pythonObject;

    public NumpyArray(PythonObject pythonObject) {
        // TODO validate python object
        this.pythonObject = pythonObject;
    }

    public long[] shape(){
        try (PythonGC gc = PythonGC.watch()){
            List ls = PythonTypes.LIST.toJava(pythonObject.attr("shape"));
            long[] ret = new long[ls.size()];
            for (int i=0; i < ls.size();i++){
                ret[i] = (Long)ls.get(i);
            }
            return ret;
        }
    }

    public String dtype(){
        try(PythonGC gc = PythonGC.watch()){
            return pythonObject.attr("dtype").attr("name").toString();
        }
    }

    public long size(){
        try(PythonGC gc = PythonGC.watch()){
            return pythonObject.attr("size").toLong();
        }
    }
    public int ndim(){
        try(PythonGC gc = PythonGC.watch()){
            return pythonObject.attr("ndim").toInt();
        }
    }

    @Override
    public String toString(){
        return pythonObject.toString();
    }

    public static class NumpyNDArray extends BaseNDArray<NumpyArray> {

        public NumpyNDArray(NumpyArray array) {
            super(array);
        }

        @Override
        public NDArrayType type() {
            NDArrayType type;
            String npDtype;
            try (PythonGC gc = PythonGC.watch()) {
                npDtype = ((NumpyArray) get()).getPythonObject().attr("dtype").attr("name").toString();
            }
            switch (npDtype) {
                case "float64":
                    type = NDArrayType.DOUBLE;
                    break;
                case "float32":
                    type = NDArrayType.FLOAT;
                    break;
                default:
                    try {
                        type = NDArrayType.valueOf(npDtype);
                    } catch (IllegalArgumentException iae) {
                        throw new UnsupportedOperationException("Unsupported numpy data type: " + npDtype);
                    }
            }
            return type;

        }

        @Override
        public long[] shape() {
            return ((NumpyArray)get()).shape();
        }

        @Override
        public long size(int dimension) {
            return shape()[dimension];
        }

        @Override
        public int rank() {
            return ((NumpyArray)get()).ndim();
        }
    }

    public PythonObject getPythonObject() {
        return pythonObject;
    }

    public static class Factory implements NDArrayFactory {
        @Override
        public Set<Class<?>> supportedTypes() {
            return Collections.singleton(NumpyArray.class);
        }

        @Override
        public boolean canCreateFrom(Object o) {
            return o instanceof NumpyArray;
        }

        @Override
        public NumpyNDArray create(Object o) {
            NumpyArray a;
            if (o instanceof NumpyArray) {
                a = (NumpyArray) o;
            } else {
                throw new IllegalStateException();
            }

            return new NumpyNDArray(a);
        }
    }
}
