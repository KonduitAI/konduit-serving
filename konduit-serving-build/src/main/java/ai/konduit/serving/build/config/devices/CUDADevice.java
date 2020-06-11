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

package ai.konduit.serving.build.config.devices;

import ai.konduit.serving.build.config.ComputeDevice;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class CUDADevice implements ComputeDevice {
    private String cudaVersion;

    public static CUDADevice forName(String s){
        String str = s.toLowerCase();
        if(str.contains("10.0")){
            return new CUDADevice("10.0");
        } else if(str.contains("10.1")){
            return new CUDADevice("10.1");
        } else if(str.contains("10.2")){
            return new CUDADevice("10.2");
        } else {
            throw new UnsupportedOperationException("Invalid, unknown, not supported or not yet implemneted CUDA version: " + s);
        }
    }
}
