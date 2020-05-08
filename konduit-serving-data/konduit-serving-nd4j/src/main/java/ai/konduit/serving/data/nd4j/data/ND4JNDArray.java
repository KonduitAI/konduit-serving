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

package ai.konduit.serving.data.nd4j.data;

import ai.konduit.serving.data.nd4j.util.ND4JUtil;
import ai.konduit.serving.pipeline.api.data.NDArrayType;
import ai.konduit.serving.pipeline.impl.data.ndarray.BaseNDArray;
import org.nd4j.linalg.api.ndarray.INDArray;

public class ND4JNDArray extends BaseNDArray<INDArray> {
    public ND4JNDArray(INDArray array) {
        super(array);
    }

    @Override
    public NDArrayType type() {
        return ND4JUtil.convertType(array.dataType());
    }

    @Override
    public long[] shape() {
        return array.shape();
    }
}
