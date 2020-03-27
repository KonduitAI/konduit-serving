/*
 *
 *  * ******************************************************************************
 *  *  * Copyright (c) 2020 Konduit AI.
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

package ai.konduit.serving.model.loader;

import io.vertx.core.buffer.Buffer;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.CharPointer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.onnxruntime.Env;
import org.bytedeco.onnxruntime.Session;
import org.bytedeco.onnxruntime.SessionOptions;

import static org.bytedeco.onnxruntime.global.onnxruntime.ORT_LOGGING_LEVEL_WARNING;
import static org.bytedeco.onnxruntime.global.onnxruntime.OrtSessionOptionsAppendExecutionProvider_Dnnl;

@Data
@AllArgsConstructor

public class OnnxModelLoader implements ModelLoader<Session> {

    private String modelPath;

    @Override
    public Buffer saveModel(Session model) {
        throw new UnsupportedOperationException("Saving models not supported for ONNX");
    }

    @Override
    public Session loadModel() throws Exception {
        Env env = new Env(ORT_LOGGING_LEVEL_WARNING, new BytePointer("konduit-serving-onnx-session" + System.currentTimeMillis()));

        try (SessionOptions sessionOptions = new SessionOptions()) {
//            OrtSessionOptionsAppendExecutionProvider_Dnnl(session_options.asOrtSessionOptions(), 1);
            try (Pointer bp = Loader.getPlatform().toLowerCase().startsWith("windows") ? new CharPointer(modelPath) : new BytePointer(modelPath)) {
                return new Session(env, bp, sessionOptions);
            }
        }
    }
}
