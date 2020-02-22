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

package ai.konduit.serving.model.loader.onnx;

import ai.konduit.serving.model.loader.ModelLoader;
import io.vertx.core.buffer.Buffer;
import lombok.AllArgsConstructor;
import lombok.Data;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.onnxruntime.Env;
import org.bytedeco.onnxruntime.Session;
import org.bytedeco.onnxruntime.SessionOptions;
import static org.bytedeco.onnxruntime.global.onnxruntime.ORT_LOGGING_LEVEL_WARNING;
import static org.bytedeco.onnxruntime.global.onnxruntime.ORT_ENABLE_EXTENDED;
import static org.bytedeco.onnxruntime.global.onnxruntime.OrtSessionOptionsAppendExecutionProvider_Dnnl;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

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
	Env env = new Env(ORT_LOGGING_LEVEL_WARNING, new BytePointer("konduit-serving-onnx-session"));

        SessionOptions session_options = new SessionOptions();
	session_options.SetIntraOpNumThreads(1);

	session_options.SetGraphOptimizationLevel(ORT_ENABLE_EXTENDED);
        OrtSessionOptionsAppendExecutionProvider_Dnnl(session_options.asOrtSessionOptions(), 1);

	Session session = new Session(env, new BytePointer(model_path), session_options);
        return session;
    }
}
