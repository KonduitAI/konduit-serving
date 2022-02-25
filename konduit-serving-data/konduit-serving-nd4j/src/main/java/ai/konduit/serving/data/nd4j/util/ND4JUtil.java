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

package ai.konduit.serving.data.nd4j.util;

import ai.konduit.serving.pipeline.api.data.NDArrayType;
import lombok.NonNull;
import org.nd4j.linalg.api.buffer.DataType;

public class ND4JUtil {

    private ND4JUtil(){ }

    public static NDArrayType typeNd4jToNDArrayType(@NonNull DataType dataType) {
        switch (dataType){
            case DOUBLE:
                return NDArrayType.DOUBLE;
            case FLOAT:
                return NDArrayType.FLOAT;
            case HALF:
                return NDArrayType.FLOAT16;
            case LONG:
                return NDArrayType.INT64;
            case INT:
                return NDArrayType.INT32;
            case SHORT:
                return NDArrayType.INT16;
            case BYTE:
                return NDArrayType.INT8;
            case UBYTE:
                return NDArrayType.UINT8;
            case BOOL:
                return NDArrayType.BOOL;
            case UTF8:
                return NDArrayType.UTF8;
            case BFLOAT16:
                return NDArrayType.BFLOAT16;
            case UINT16:
                return NDArrayType.UINT16;
            case UINT32:
                return NDArrayType.UINT32;
            case UINT64:
                return NDArrayType.UINT64;
            case COMPRESSED:
            case UNKNOWN:
            default:
                throw new UnsupportedOperationException("Unknown or not supported type: " + dataType);
        }
    }

    public static DataType typeNDArrayTypeToNd4j(@NonNull NDArrayType type) {
        switch (type){
            case DOUBLE:
                return DataType.DOUBLE;
            case FLOAT:
                return DataType.FLOAT;
            case FLOAT16:
                return DataType.FLOAT16;
            case BFLOAT16:
                return DataType.BFLOAT16;
            case INT64:
                return DataType.INT64;
            case INT32:
                return DataType.INT32;
            case INT16:
                return DataType.INT16;
            case INT8:
                return DataType.INT8;
            case UINT64:
                return DataType.UINT64;
            case UINT32:
                return DataType.UINT32;
            case UINT16:
                return DataType.UINT16;
            case UINT8:
                return DataType.UINT8;
            case BOOL:
                return DataType.BOOL;
            case UTF8:
                return DataType.UTF8;
            default:
                throw new UnsupportedOperationException("Unable to convert datatype to ND4J datatype: " + type);
        }
    }


}
