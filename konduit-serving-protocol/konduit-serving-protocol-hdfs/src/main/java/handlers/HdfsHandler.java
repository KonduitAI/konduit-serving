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

package handlers;

import ai.konduit.serving.pipeline.api.protocol.Credentials;
import lombok.extern.slf4j.Slf4j;
import providers.HdfsAccessProvider;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

@Slf4j
public class HdfsHandler extends URLStreamHandler {

    protected URLConnection openConnection(URL url) throws IOException {

        return new URLConnection(url) {

            @Override
            public InputStream getInputStream() throws IOException {
                HdfsAccessProvider accessProvider = new HdfsAccessProvider();
                try {
                    return accessProvider.connect(url, accessProvider.getCredentials());
                } catch (URISyntaxException e) {
                    log.error("Failed connection to " + url, e);
                    throw new IOException(e);
                }
            }

            @Override
            public void connect() throws IOException { }

        };
    }
}