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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Dependencies that are required by this module in order to execute
 * Note these are dependencies other than the ones already included in the module's Maven dependencies
 * For example, backends (CPU or GPU) for ND4J, CPU or GPU native dependencies for Tensorflow, etc.
 */
@Retention(RetentionPolicy.RUNTIME)

public @interface RequiresDependenciesAny {
    Requires[] value();
}
