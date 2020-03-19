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

import java.util.Date;

import static org.bytedeco.onnxruntime.global.onnxruntime.ORT_ENABLE_EXTENDED;
import static org.bytedeco.onnxruntime.global.onnxruntime.ORT_LOGGING_LEVEL_WARNING;

@Data
@AllArgsConstructor

public class OnnxModelLoader implements ModelLoader<Session> {

    private String model_path;

    @Override
    public Buffer saveModel(Session model) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Session loadModel() throws Exception {
        Env env = new Env(ORT_LOGGING_LEVEL_WARNING, new BytePointer("konduit-serving-onnx-session" + (new Date()).getTime()));

        try (SessionOptions session_options = new SessionOptions()) {
            session_options.SetIntraOpNumThreads(1);

            session_options.SetGraphOptimizationLevel(ORT_ENABLE_EXTENDED);
//        OrtSessionOptionsAppendExecutionProvider_Dnnl(session_options.asOrtSessionOptions(), 1);

            try (Pointer bp = Loader.getPlatform().startsWith("windows") ? new CharPointer(model_path) : new BytePointer(model_path)) {
                Session session = new Session(env, bp, session_options);
                return session;
            }
        }
    }
}
