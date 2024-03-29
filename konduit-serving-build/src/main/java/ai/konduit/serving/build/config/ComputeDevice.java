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

package ai.konduit.serving.build.config;

import ai.konduit.serving.build.config.devices.CUDADevice;

public interface ComputeDevice {

    String CPU = "CPU";
    String CUDA_100 = "CUDA_10.0";
    String CUDA_101 = "CUDA_10.1";
    String CUDA_102 = "CUDA_10.2";
    String CUDA_110 = "CUDA_11.0";




    static ComputeDevice forName(String name){
        if(name.equalsIgnoreCase(CPU)) {
            return null;
        } else if(name.toLowerCase().contains("cuda")){
            return CUDADevice.forName(name);
        }
        throw new UnsupportedOperationException("Invalid, unknown, not supported or not yet implemented device type: " + name);
    }

}
