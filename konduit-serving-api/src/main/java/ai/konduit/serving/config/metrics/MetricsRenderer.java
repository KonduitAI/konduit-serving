/*
 *
 *  * ******************************************************************************
 *  *
 *  *  * Copyright (c) 2022 Konduit K.K.
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
package ai.konduit.serving.config.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

/**
 * An updatedable {@link MeterBinder} that allows
 * updates of metrics beyond {@link MeterBinder#bindTo(MeterRegistry)}
 *
 * This allows encapsulation of logic for doing things like
 * calling {@link Counter#increment()}
 *
 * @author Adam Gibson
 */
public interface MetricsRenderer extends MeterBinder  {


    /**
     * The configuration for the metrics
     * @return
     */
    MetricsConfig config();

    /**
     * Updates the metrics based on given arguments.
     * @param args
     */
    void updateMetrics(Object...args);


}
