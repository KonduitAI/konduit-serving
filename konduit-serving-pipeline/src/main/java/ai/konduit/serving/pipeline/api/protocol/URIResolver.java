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
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
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

    public static void removeOutdatedCacheEntries(File metaFile) throws IOException {
        String lifeTimeProp = System.getProperty("konduit.serving.cache.lifetime");
        if (StringUtils.isEmpty(lifeTimeProp))
            return;
        final int daysTimeout = Integer.parseInt(lifeTimeProp);
        if (daysTimeout <= 0)
            return;
        File tempFile = new File(cacheDirectory, "metafile.temp");

        try (BufferedReader in = new BufferedReader(new FileReader(metaFile));
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(tempFile)))) {
            String line = StringUtils.EMPTY;
            while((line =in.readLine())!=null) {
                Iterable<CSVRecord> records = CSVFormat.DEFAULT.parse(new StringReader(line));
                for (CSVRecord record : records) {
                    long accessedTimestamp = Long.parseLong(record.get(3));
                    long elapsedMillis = System.currentTimeMillis() - accessedTimestamp;
                    long elapsedDays = elapsedMillis / (1000 * 60 * 60 * 24);
                    if (elapsedDays <= daysTimeout) {
                        writer.write(line);
                    } else {
                        log.info("Removing outdated cached file " + record.get(0));
                        new File(record.get(0)).delete();
                    }
                }
            }
            FileUtils.moveFile(tempFile, metaFile);
        }
    }

    private static File cacheDirectory;
    private static File metaFile;
    static {
        File f = new File(System.getProperty("user.home"), StringUtils.defaultIfEmpty(
                                                            System.getProperty("konduit.serving.cache.location"),
                                                            ".konduit_cache/"));
        if (!f.exists())
            f.mkdirs();
        cacheDirectory = f;
        metaFile = new File(cacheDirectory, ".metadata");
        try {
           if (!metaFile.exists()) {
               metaFile.createNewFile();
           }
           removeOutdatedCacheEntries(metaFile);
        } catch (IOException e) {
                log.error("Cache initialization failed", e);
        }
    }

    public static File getCachedFile(String uri) {
        URI u = URI.create(uri);
        String fullPath = StringUtils.defaultIfEmpty(u.getScheme(), StringUtils.EMPTY);
        System.out.println(u.getPath());
        String[] dirs = u.getPath().split("/");
        for (int i = 0; i < dirs.length-1; ++i) {
            fullPath += File.separator + dirs[i];
        }
        fullPath += File.separator + FilenameUtils.getName(uri);
        File effectiveDirectory = new File(cacheDirectory, fullPath);
        return effectiveDirectory;
    }

    @RetryOnFailure(attempts = 3)
    private static File load(URL url, File cachedFile) throws IOException {
        URLConnection connection = url.openConnection();

        int contentLength = 0;
        long lastModified = 0;

        BufferedReader in = new BufferedReader(new FileReader(metaFile));
        String line = StringUtils.EMPTY;
        while ((line = in.readLine()) != null) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT
                    .parse(new StringReader(line));
            for (CSVRecord record : records) {
                if (record.get(0).equals(cachedFile.getAbsolutePath())) {
                    contentLength = Integer.parseInt(record.get(1));
                    lastModified = Long.parseLong(record.get(2));
                    break;
                }
            }
        }
        if (lastModified > 0 && connection.getLastModified() == lastModified &&
                                contentLength == connection.getContentLength()) {
            // File is in cache and its timestamps are the same as of remote resource
            return cachedFile;
        }
        else {
            String warnOnly = StringUtils.defaultIfEmpty(System.getProperty("konduit.serving.cache.validation.warnonly"),"true");
            if (warnOnly.equals("true")) {
                log.error("Cached file " + cachedFile.getAbsolutePath() + " has inconsistent state.");
                return cachedFile;
            }
            else {
                log.error("Cached file " + cachedFile.getAbsolutePath() + " has inconsistent state and will be removed");
                cachedFile.delete();
            }
        }
        // File was either just deleted or didn't exist, so writing metadata here and caching in
        // the calling method.
        String metaData = cachedFile.getAbsolutePath() + "," + connection.getContentLength() + "," +
                connection.getLastModified() + "," + System.currentTimeMillis() + System.lineSeparator();
        FileUtils.writeStringToFile(metaFile, metaData, "UTF-8", true);
        return null;
    }

    public static File getFile(String uri) throws IOException {
        URI u = URI.create(uri);
        return getFile(u);
    }

    public static File getFile(URI uri) throws IOException {
        String scheme = uri.getScheme();
        if (scheme.equals("file")) {
            return new File(uri.getPath());
        }
        File cachedFile = getCachedFile(uri.getPath());

        URL url = uri.toURL();
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
