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

package ai.konduit.serving.models.tensorflow.util;

import ai.konduit.serving.pipeline.api.data.NDArrayType;
import org.tensorflow.DataType;
import org.tensorflow.types.UInt8;

public class TensorFlowUtil {

    private TensorFlowUtil(){ }

    public static Class<?> toTFType(NDArrayType t){
        switch (t){
            case DOUBLE:
                return Double.class;
            case FLOAT:
                return Float.class;
            case INT64:
                return Long.class;
            case INT32:
                return Integer.class;
            case INT16:
                return Short.class;
            case INT8:
                return Byte.class;
            case UINT8:
                return UInt8.class;
            case BOOL:
                return Boolean.class;
            case UTF8:
                return String.class;
            case UINT64:
            case UINT32:
            case UINT16:
            case BFLOAT16:
            case FLOAT16:
            default:
                throw new UnsupportedOperationException("Type not supported by TF Java: " + t);
        }
    }

    public static NDArrayType fromTFType(DataType dataType){
        switch (dataType){
            case FLOAT:
                return NDArrayType.FLOAT;
            case DOUBLE:
                return NDArrayType.DOUBLE;
            case INT32:
                return NDArrayType.INT32;
            case UINT8:
                return NDArrayType.UINT8;
            case STRING:
                return NDArrayType.UTF8;
            case INT64:
                return NDArrayType.INT64;
            case BOOL:
                return NDArrayType.BOOL;
            default:
                throw new UnsupportedOperationException("Unknown TF type: " + dataType);
        }
    }

}
