/*
 *
 *  * ******************************************************************************
 *  *  * Copyright (c) 2015-2019 Skymind Inc.
 *  *  * Copyright (c) 2019 Konduit AI.
 *  *  *
 *  *  * This program and the accompanying materials are made available under the
 *  *  * terms of the Apache License, Version 2.0 which is available at
 *  *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  *  * License for the specific language governing permissions and limitations
 *  *  * under the License.
 *  *  *
 *  *  * SPDX-License-Identifier: Apache-2.0
 *  *  *****************************************************************************
 *
 *
 */

package ai.konduit.serving.metrics;

/**
 * Metric types for prometheus
 *
 * @author Adam Gibson
 */
public enum MetricType {
    CLASS_LOADER,
    JVM_MEMORY,
    JVM_GC,
    PROCESSOR,
    JVM_THREAD,
    LOGGING_METRICS,
    NATIVE,
    GPU,
    //note these are machine learning metrics, not system metrics
    //these are meant to analyze the output coming form the neural network when running
    //in production
    CLASSIFICATION,
    REGRESSION,
    CUSTOM_MULTI_LABEL
}
