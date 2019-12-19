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

package ai.konduit.serving.pipeline.generator.data.impl.bool;

import ai.konduit.serving.config.SchemaType;
import ai.konduit.serving.pipeline.generator.data.BaseFakerDataGenerator;
import com.github.javafaker.Faker;

public class FakerBooleanDataGenerator extends BaseFakerDataGenerator<Boolean> {
    public FakerBooleanDataGenerator(Faker faker) {
        super(faker);
    }

    public FakerBooleanDataGenerator(long seed) {
        super(seed);
    }

    public FakerBooleanDataGenerator() {
    }

    @Override
    public SchemaType typeForGeneration() {
        return SchemaType.Boolean;
    }

    @Override
    public Boolean generate() {
        return faker.bool().bool();
    }
}
