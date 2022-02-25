/*
 *  ******************************************************************************
 *  * Copyright (c) 2022 Konduit K.K.
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
import ai.konduit.serving.pipeline.api.data.NDArrayType;
import ai.konduit.serving.pipeline.impl.data.ndarray.BaseNDArray;
import org.nd4j.common.base.Preconditions;
import org.tensorflow.DataType;
import org.tensorflow.Tensor;

import java.util.Arrays;

public class TFNDArray extends BaseNDArray<Tensor> {
    public TFNDArray(Tensor array) {
        super(array);
    }

    @Override
    public NDArrayType type() {
        DataType dt = array.dataType();
        return TensorFlowUtil.fromTFType(dt);
    }

    @Override
    public long[] shape() {
        return array.shape();
    }

    @Override
    public long size(int dimension) {
        int rank = rank();
        Preconditions.checkState(dimension >= -rank && dimension < rank, "Invalid dimension: Got %s for rank %s array", dimension, rank);
        if(dimension < 0)
            dimension += rank;
        return array.shape()[dimension];
    }

    @Override
    public int rank() {
        return array.shape().length;
    }

    @Override
    public String toString() {
        return "TensorFlowNDArray(type=" + type() + ",shape=" + Arrays.toString(shape()) + ")";
    }
}
