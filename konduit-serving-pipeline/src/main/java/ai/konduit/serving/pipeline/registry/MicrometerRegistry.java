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
package ai.konduit.serving.pipeline.registry;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.*;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MicrometerRegistry {
    private static List<io.micrometer.core.instrument.MeterRegistry> registries;

    static {
        io.micrometer.core.instrument.Metrics.globalRegistry.add(new SimpleMeterRegistry());
    }

    public static io.micrometer.core.instrument.MeterRegistry getRegistry() {
        if (registries.isEmpty()) {
            return io.micrometer.core.instrument.Metrics.globalRegistry;
        }
        if (registries.size() > 1) {
            log.info("Loaded {} MeterRegistry instances. Loading the first one.", registries.size());
        }
        return registries.get(0);
    }

    public static void initRegistries() {

        ServiceLoader<io.micrometer.core.instrument.MeterRegistry> sl =
                ServiceLoader.load(io.micrometer.core.instrument.MeterRegistry.class);

        Iterator<io.micrometer.core.instrument.MeterRegistry> iterator = sl.iterator();
        while(iterator.hasNext()){
            registries.add(iterator.next());
        }
    }

    public static void registerRegistries(@NonNull io.micrometer.core.instrument.MeterRegistry registry) {
        if (registries == null) {
            initRegistries();
        }
        registries.add(registry);
    }
}
