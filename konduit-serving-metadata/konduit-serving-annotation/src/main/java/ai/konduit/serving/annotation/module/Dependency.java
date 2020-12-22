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

package ai.konduit.serving.annotation.module;

/**
 * gId = groupId - org.apache.commons, org.deeplearning4j, etc<br>
 * aId = artifactId - commons-lang3, deeplearning4j-core, etc<br>
 * ver = version - 3.6, 1.0.0-SNAPSHOT, etc<br>
 * classifier - may be null. Maven classifier, sometimes used for different hardware devices (linux-x86_64, etc)<br>
 * cReq - Only applies when multiple classifiers exist, at which point it specifies how those classifier dependencies
 * should be combined - i.e., do we need just ONE of them (i.e., ANY) or ALL of them?
 */
public @interface Dependency {
    String gId();

    String aId();

    String ver();

    String[] classifier() default {};      //None: means 'no classifier'

    Req cReq() default Req.ANY;
}
