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

package ai.konduit.serving.endpoint;

import io.swagger.v3.oas.annotations.media.Schema;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.impl.MimeMapping;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.io.FilenameUtils;

import java.util.List;

/**
 *
 */
@Schema(description = "A base object for configuring a custom endpoint to serve assets.")
public abstract class AssetServingEndpoint implements Endpoint {

    @Schema(description = "Endpoint http path.")
    protected final String httpPath;

    @Schema(description = "Asset file path.")
    protected final String fileAssetPath;

    public AssetServingEndpoint(String httpPath, String fileAssetPath){
        this.httpPath = httpPath;
        this.fileAssetPath =  fileAssetPath;
    }

    @Override
    public HttpMethod type() {
        return HttpMethod.GET;
    }

    @Override
    public String path() {
        return httpPath;
    }

    @Override
    public List<String> consumes() {
        //Null for GET method
        return null;
    }

    @Override
    public List<String> produces() {
        return null;
    }

    @Override
    public Handler<RoutingContext> handler() {
        return rc -> {
            String path = rc.request().path();
            path = path.substring(8);   //Remove "/assets/", which is 8 characters
            String mime;
            String newPath;
            if (path.contains("webjars")) {
                newPath = "META-INF/resources/" + path.substring(path.indexOf("webjars"));
            } else {
                newPath = fileAssetPath + (path.startsWith("/") ? path.substring(1) : path);
            }
            mime = MimeMapping.getMimeTypeForFilename(FilenameUtils.getName(newPath));

            //System.out.println("PATH: " + path + " - mime = " + mime);
            rc.response()
                    .putHeader("content-type", mime)
                    .sendFile(newPath);
        };
    }
}
