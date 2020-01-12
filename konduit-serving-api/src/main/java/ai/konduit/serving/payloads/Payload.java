/*
 *       Copyright (c) 2019 Konduit AI.
 *
 *       This program and the accompanying materials are made available under the
 *       terms of the Apache License, Version 2.0 which is available at
 *       https://www.apache.org/licenses/LICENSE-2.0.
 *
 *       Unless required by applicable law or agreed to in writing, software
 *       distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *       WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *       License for the specific language governing permissions and limitations
 *       under the License.
 *
 *       SPDX-License-Identifier: Apache-2.0
 *
 */

package ai.konduit.serving.payloads;

import ai.konduit.serving.config.Input;
import ai.konduit.serving.config.Output;
import ai.konduit.serving.config.SchemaType;

import java.util.Map;

public interface Payload<T,ENCODER_TYPE extends PayloadDecoder<T>,DECODER_TYPE extends PayloadDecoder<T>> {

    ENCODER_TYPE encoder();

    DECODER_TYPE decoder();

    Input.DataFormat inputDataFormat();

    Output.DataFormat outputDataFormat();

    default  boolean hasSchema() {
        return false;
    }

    Map<String, SchemaType> schema();

}
