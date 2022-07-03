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

package ai.konduit.serving.pipeline.api.data;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The data type of an {@link NDArray}
 */
@Schema(description = "An enum that specifies the data type of an n-dimensional array.")
public enum NDArrayType {
    DOUBLE,
    FLOAT,
    FLOAT16,
    BFLOAT16,
    INT64,
    INT32,
    INT16,
    INT8,
    UINT64,
    UINT32,
    UINT16,
    UINT8,
    BOOL,
    UTF8;

    public boolean isFixedWidth(){
        return width() > 0;
    }

    public int width() {
        switch (this){
            case DOUBLE:
            case INT64:
            case UINT64:
                return 8;
            case FLOAT:
            case INT32:
            case UINT32:
                return 4;
            case FLOAT16:
            case BFLOAT16:
            case INT16:
            case UINT16:
                return 2;
            case INT8:
            case UINT8:
            case BOOL:
                return 1;
            case UTF8:
            default:
                return 0;
        }
    }
}
