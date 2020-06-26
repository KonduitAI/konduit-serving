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
package providers;

import ai.konduit.serving.pipeline.api.protocol.Credentials;
import ai.konduit.serving.pipeline.api.protocol.URLAccessProvider;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class HdfsAccessProvider implements URLAccessProvider {

    static {
        URL.setURLStreamHandlerFactory(new HdfsStreamHandlerFactory());
    }

    @Override
    public Credentials getCredentials() {
        String accessKey = System.getenv("HADOOP_ACCESS_KEY");
        String secretKey = System.getenv("HADOOP_SECRET_KEY");
        return new Credentials(accessKey, secretKey);
    }

    @Override
    public InputStream connect(URL url, Credentials credentials) throws IOException {
        return null;
    }
}
