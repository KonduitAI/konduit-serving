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

package ai.konduit.serving.protocols.handlers;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import ai.konduit.serving.pipeline.api.protocol.Credentials;
import ai.konduit.serving.protocols.providers.S3AccessProvider;
import org.jets3t.service.ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.security.AWSCredentials;

public class S3Handler extends URLStreamHandler {

    protected URLConnection openConnection(URL url) throws IOException {

        return new URLConnection(url) {

            @Override
            public InputStream getInputStream() throws IOException {

                String accessKey = null;
                String secretKey = null;

                if (url.getUserInfo() != null) {
                    String[] credentials = url.getUserInfo().split("[:]");
                    accessKey = credentials[0];
                    secretKey = credentials[1];
                }

                S3AccessProvider accessProvider = new S3AccessProvider();
                return accessProvider.connect(url, new Credentials(accessKey, secretKey));
            }

            @Override
            public void connect() throws IOException { }

        };
    }
}