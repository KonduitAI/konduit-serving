/*
 *  ******************************************************************************
 *  * Copyright (c) 2022 Konduit K.K.
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

package ai.konduit.serving.pipeline.api.protocol;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Paths;

@Slf4j
public class URIResolver {

    public static boolean isUrl(String input) {
        if (input.startsWith("http://") ||
            input.startsWith("https://") ||
            input.startsWith("ftp://")) {
            return true;
        }
        return false;
    }

    private static File cacheDirectory;
    static {
        File f = new File(System.getProperty("user.home"), ".konduit_cache/");
        if (!f.exists())
            f.mkdirs();
        cacheDirectory = f;
    }


    public static File getCachedFile(String uri) {
        URI u  = URI.create(uri);
        String fullPath = StringUtils.defaultIfEmpty(u.getScheme(), StringUtils.EMPTY);
        //System.out.println(u.getPath());
        String[] dirs = u.getPath().split("/");
        fullPath += File.separator + FilenameUtils.getName(uri);
        File effectiveDirectory = new File(cacheDirectory, fullPath);
        return effectiveDirectory;
    }

    public static File getFile(String uri) throws IOException {
        File f = getIfFile(uri);
        if(f != null){
            return f;
        }

        URI u = URI.create(uri);

        String scheme = u.getScheme();
        if (scheme.equals("file")) {
            return new File(u.getPath());
        }
        File cachedFile = getCachedFile(uri);
        if (cachedFile.exists()) {
            return cachedFile;
        }

        URL url = u.toURL();
        URLConnection connection = url.openConnection();
        FileUtils.copyURLToFile(url, cachedFile);

        return cachedFile;
    }

    public static void clearCache(){
        try {
            FileUtils.deleteDirectory(cacheDirectory);
        } catch (IOException e){
            log.warn("Failed to delete cache directory: {}", cacheDirectory);
        }
        cacheDirectory.mkdirs();
    }

    protected static File getIfFile(String path){
        if(path.matches("\\w+://.*") || path.startsWith("file:/"))       //Regex does not catch file:/C:/... out of (new File(...).toURI().toString()
            return null;

        try{
            File f = new File(path);
            f.getCanonicalPath();   //Throws an IOException if not a valid file path
            return f;
        } catch (IOException e2){
            return null;
        }
    }
}
