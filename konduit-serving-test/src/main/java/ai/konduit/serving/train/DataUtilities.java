/*
 *       Copyright (c) 2019 Konduit AI.
 *
 *       This program and the accompanying materials are made available under the
 *       terms of the Apache License, Version 2.0 which is available at
 *       https://www.apache.org/licenses/LICENSE-2.0.
 *
 *       Unless required by applicable law or agreed to in writing, software
 *       distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *       WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *       License for the specific language governing permissions and limitations
 *       under the License.
 *
 *       SPDX-License-Identifier: Apache-2.0
 *
 */

package ai.konduit.serving.train;

import java.io.IOException;

/**
 * Common data utility functions.
 *
 * @author fvaleri
 */
public class DataUtilities {

    /**
     * Download a remote file if it doesn't exist.
     *
     * @param remoteUrl URL of the remote file.
     * @param localPath Where to download the file.
     * @return True if and only if the file has been downloaded.
     * @throws Exception IO error.
     */
    public static boolean downloadFile(String remoteUrl, String localPath) throws IOException {
        boolean downloaded = false;
        if (remoteUrl == null || localPath == null)
            return downloaded;
        java.io.File file = new java.io.File(localPath);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            org.apache.http.impl.client.HttpClientBuilder builder = org.apache.http.impl.client.HttpClientBuilder.create();
            org.apache.http.impl.client.CloseableHttpClient client = builder.build();
            try (org.apache.http.client.methods.CloseableHttpResponse response = client.execute(new org.apache.http.client.methods.HttpGet(remoteUrl))) {
                org.apache.http.HttpEntity entity = response.getEntity();
                if (entity != null) {
                    try (java.io.FileOutputStream outstream = new java.io.FileOutputStream(file)) {
                        entity.writeTo(outstream);
                        outstream.flush();
                        outstream.close();
                    }
                }
            }
            downloaded = true;
        }
        if (!file.exists())
            throw new IOException("File doesn't exist: " + localPath);
        return downloaded;
    }

    /**
     * Extract a "tar.gz" file into a local folder.
     *
     * @param inputPath  Input file path.
     * @param outputPath Output directory path.
     * @throws IOException IO error.
     */
    public static void extractTarGz(String inputPath, String outputPath) throws IOException {
        if (inputPath == null || outputPath == null)
            return;
        final int bufferSize = 4096;
        if (!outputPath.endsWith("" + java.io.File.separatorChar))
            outputPath = outputPath + java.io.File.separatorChar;
        try (org.apache.commons.compress.archivers.tar.TarArchiveInputStream tais = new org.apache.commons.compress.archivers.tar.TarArchiveInputStream(
                new org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream(new java.io.BufferedInputStream(new java.io.FileInputStream(inputPath))))) {
            org.apache.commons.compress.archivers.tar.TarArchiveEntry entry;
            while ((entry = (org.apache.commons.compress.archivers.tar.TarArchiveEntry) tais.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    new java.io.File(outputPath + entry.getName()).mkdirs();
                } else {
                    int count;
                    byte data[] = new byte[bufferSize];
                    java.io.FileOutputStream fos = new java.io.FileOutputStream(outputPath + entry.getName());
                    java.io.BufferedOutputStream dest = new java.io.BufferedOutputStream(fos, bufferSize);
                    while ((count = tais.read(data, 0, bufferSize)) != -1) {
                        dest.write(data, 0, count);
                    }
                    dest.close();
                }
            }
        }
    }

}