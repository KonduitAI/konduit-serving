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

package ai.konduit.serving.pipeline.api.protocol;

import com.jcabi.aspects.RetryOnFailure;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

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
        File f = new File(System.getProperty("user.home"), StringUtils.defaultIfEmpty(
                                                            System.getProperty("konduit.serving.cache.location"),
                                                            ".konduit_cache/"));
        if (!f.exists())
            f.mkdirs();
        cacheDirectory = f;
    }


    public static File getCachedFile(String uri) {
        URI u = URI.create(uri);
        String fullPath = StringUtils.defaultIfEmpty(u.getScheme(), StringUtils.EMPTY);
        System.out.println(u.getPath());
        String[] dirs = u.getPath().split("/");
        for (String dir : dirs) {
            fullPath += File.separator + dir;
        }
        fullPath += File.separator + FilenameUtils.getName(uri);
        File effectiveDirectory = new File(cacheDirectory, fullPath);
        return effectiveDirectory;
    }

    @RetryOnFailure(attempts = 3)
    private static File load(URL url, File cachedFile) throws IOException {
        URLConnection connection = url.openConnection();

        if (connection.getContentLength() == cachedFile.length() &&
            connection.getLastModified() == cachedFile.lastModified()) {
            return cachedFile;
        }
        else {
            cachedFile.delete();
        }
        return null;
    }

    public static File getFile(String uri) throws IOException {

        URI u = URI.create(uri);
        String scheme = u.getScheme();
        if (scheme.equals("file")) {
            return new File(u.getPath());
        }
        File cachedFile = getCachedFile(uri);

        URL url = u.toURL();
        if (cachedFile.exists()) {
            File verifiedFile = load(url, cachedFile);
            if (verifiedFile != null) {
                return verifiedFile;
            }
        }
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
}
