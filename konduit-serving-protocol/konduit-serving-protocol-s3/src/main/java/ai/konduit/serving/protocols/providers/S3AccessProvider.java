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
package ai.konduit.serving.protocols.providers;

import ai.konduit.serving.pipeline.api.protocol.Credentials;
import ai.konduit.serving.pipeline.api.protocol.URLAccessProvider;
import org.jets3t.service.ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.security.AWSCredentials;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class S3AccessProvider implements URLAccessProvider {

    static {
        URL.setURLStreamHandlerFactory(new S3StreamHandlerFactory());
    }

    @Override
    public Credentials getCredentials() {
        String accessKey = System.getenv("S3_ACCESS_KEY");
        String secretKey = System.getenv("S3_SECRET_KEY");
        return new Credentials(accessKey, secretKey);
    }

    @Override
    public InputStream connect(URL url, Credentials credentials) throws IOException {
        String bucket = url.getHost().substring(0, url.getHost().indexOf("."));
        String key = url.getPath().substring(1);

        try {
            RestS3Service s3Service = new RestS3Service(
                    new AWSCredentials(credentials.getAccessKey(), credentials.getSecretKey()));
            S3Object s3obj = s3Service.getObject(bucket, key);
            return s3obj.getDataInputStream();
        } catch (ServiceException e) {
            throw new IOException(e);
        }
    }
}
