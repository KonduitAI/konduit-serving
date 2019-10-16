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

package ai.konduit.serving.model.loader.samediff;

import ai.konduit.serving.model.loader.ModelLoader;
import io.netty.buffer.Unpooled;
import io.vertx.core.buffer.Buffer;
import lombok.AllArgsConstructor;
import org.nd4j.autodiff.samediff.SameDiff;

import java.nio.ByteBuffer;

@AllArgsConstructor
public class InMemorySameDiffModelLoader implements ModelLoader<SameDiff> {

    private SameDiff sameDiff;

    @Override
    public Buffer saveModel(SameDiff model) {
        ByteBuffer byteBuffer = model.asFlatBuffers(true);
        return Buffer.buffer(Unpooled.wrappedBuffer(byteBuffer));
    }

    @Override
    public SameDiff loadModel() {
        return sameDiff;
    }
}
