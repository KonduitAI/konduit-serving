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

package ai.konduit.serving.pipeline.generator.data.impl.ndarray;

import ai.konduit.serving.config.SchemaType;
import ai.konduit.serving.pipeline.generator.data.DataGenerator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

public class NdarrayDataGenerator implements DataGenerator<INDArray> {

    protected long seed;
    protected long[] shape;

    public NdarrayDataGenerator(long seed, long[] shape) {
        this.seed = seed;
        this.shape = shape;
        Nd4j.getRandom().setSeed(seed);
    }

    @Override
    public SchemaType typeForGeneration() {
        return SchemaType.NDArray;
    }

    @Override
    public INDArray generate() {
        return Nd4j.rand(shape);
    }
}
