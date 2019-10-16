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

import ai.konduit.serving.model.loader.ModelGuesser;
import ai.konduit.serving.model.loader.ModelLoader;
import io.netty.buffer.Unpooled;
import io.vertx.core.buffer.Buffer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.imports.graphmapper.tf.TFGraphMapper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.List;

@AllArgsConstructor
@Slf4j
public class SameDiffModelLoader implements ModelLoader<SameDiff> {

    private File pathToModel;
    @Getter @Setter
    private List<String> inputNames,outputNames;

    public SameDiffModelLoader(File pathToModel) {
        this.pathToModel = pathToModel;
    }

    @Override
    public Buffer saveModel(SameDiff model) {
        if(model.getTrainingConfig() != null) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            //  model.save(byteArrayOutputStream,true);
            return Buffer.buffer(byteArrayOutputStream.toByteArray());

        }

        ByteBuffer byteBuffer = model.asFlatBuffers(true);
        return Buffer.buffer(Unpooled.wrappedBuffer(byteBuffer));
    }

    @Override
    public SameDiff loadModel() throws Exception {
        if(ModelGuesser.isTensorflowFile(pathToModel)) {
            log.debug("Loading tensorflow model from " + pathToModel.getAbsolutePath());
            return TFGraphMapper.getInstance().importGraph(pathToModel);
        }
        else if(ModelGuesser.isSameDiffZipFile(pathToModel)) {
            return SameDiff.load(pathToModel,true);
        }

        log.debug("Loading samediff model from " + pathToModel.getAbsolutePath());
        return SameDiff.fromFlatFile(pathToModel);
    }
}
