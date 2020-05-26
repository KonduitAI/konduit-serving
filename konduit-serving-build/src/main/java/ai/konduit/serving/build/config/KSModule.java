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

package ai.konduit.serving.build.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;
import org.nd4j.shade.jackson.annotation.JsonProperty;

@Data
@Accessors(fluent = true)
public class KSModule {

    public static final KSModule PIPELINE = new KSModule("konduit-serving-pipeline");
    public static final KSModule DL4J = new KSModule("konduit-serving-deeplearning4j");
    public static final KSModule SAMEDIFF = new KSModule("konduit-serving-samediff");
    public static final KSModule TENSORFLOW = new KSModule("konduit-serving-tensorflow");


    private final String name;

    public KSModule(@JsonProperty("name") String name){
        this.name = name;
    }
}
