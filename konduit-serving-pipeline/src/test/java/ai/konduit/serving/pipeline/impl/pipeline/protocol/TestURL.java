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

package ai.konduit.serving.pipeline.impl.pipeline.protocol;

import ai.konduit.serving.common.test.TestServer;
import ai.konduit.serving.pipeline.api.protocol.FtpClient;
import ai.konduit.serving.pipeline.api.protocol.URIResolver;
import ai.konduit.serving.pipeline.api.protocol.handlers.KSStreamHandlerFactory;
import lombok.val;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.WindowsFakeFileSystem;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

public class TestURL {

    private TestServer server;
    private final static int PORT = 9090;
    private final static String HOST = "localhost";
    private static final String HTTP = "http://";
    private static final String HTTPS = "https://";
    private static final String FTP = "ftp://";

    //private String configData = StringUtils.EMPTY;

    @Rule
    public TemporaryFolder testDir = new TemporaryFolder();

    static {
        URL.setURLStreamHandlerFactory(new KSStreamHandlerFactory());
    }

    @Before
    public void setUp() throws Exception {
        server = new TestServer(PORT, null);
        server.start();
        /*configData = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("config/config.json"),
                "UTF-8");*/
    }

    @After
    public void tearDown() throws Exception {
        server.stop();
    }

    @Test
    public void testFilePath() throws Exception {
        File folder = testDir.newFolder();
        File myFile = new File(folder, "myFile.txt");
        FileUtils.writeStringToFile(myFile, "My string!", StandardCharsets.UTF_8);

        File f2 = URIResolver.getFile(myFile.getAbsolutePath());
        assertEquals(myFile, f2);
    }

    @Test
    public void testHttpURL() throws Exception {
        String url = HTTP + HOST + ":" + PORT + "/src/test/resources/config/config.json";
        String data = FileUtils.readFileToString(URIResolver.getFile(url), "UTF-8");
        assertTrue(data.contains("pipelineSteps"));

        url = HTTPS + HOST + ":" + PORT + "/src/test/resources/config/config.json";
        data = FileUtils.readFileToString(URIResolver.getFile(url), "UTF-8");
        assertTrue(data.contains("pipelineSteps"));
    }

    @Test
    public void testFTP() throws IOException, InterruptedException {
        assumeTrue(SystemUtils.IS_OS_WINDOWS);
        FakeFtpServer fakeFtpServer = new FakeFtpServer();
        String homeDir = testDir.newFolder().getAbsolutePath();
        fakeFtpServer.addUserAccount(new UserAccount("user", "password",
                                    homeDir));
        val fileSystem = new WindowsFakeFileSystem();
        fileSystem.add(new DirectoryEntry(homeDir));
        fileSystem.add(new FileEntry(homeDir + File.separator + "config.json", "{\"pipelineSteps\": []}"));
        fakeFtpServer.setFileSystem(fileSystem);

        fakeFtpServer.start();
        System.out.println("FTP started on : " + fakeFtpServer.getServerControlPort());

        FtpClient ftpClient = new FtpClient();
        ftpClient.connect("localhost");
        if (ftpClient.login("user", "password")) {
            String url = FTP + "user:password@" + HOST + ":" + 21 + "/config.json";
            File dataFile = URIResolver.getFile(url);
            String data = FileUtils.readFileToString(dataFile, "UTF-8");
            assertTrue(data.contains("pipelineSteps"));
            fakeFtpServer.stop();
        }
        else {
            fakeFtpServer.stop();
            fail();
        }
    }

    @Ignore
    @Test
    public void testS3() throws IOException {
        URL url = new URL("s3://<access-key>:<secret-key>@<bucket>.s3.amazonaws.com/<filename>");

        URLConnection conn = url.openConnection();
        try (InputStream is = conn.getInputStream()) {
            Reader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String received = IOUtils.toString(reader);
            System.out.println(received);
        }
    }

    @Ignore
    @Test
    public void testHDFS() throws IOException {
        System.setProperty("hadoop.home.dir", "D:\\install\\hadoop-2.8.0\\bin");
        URL url = new URL("hdfs://localhost:9000/user/root/config.json");

        URLConnection conn = url.openConnection();
        try (InputStream is = conn.getInputStream()) {
            Reader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String received = IOUtils.toString(reader);
            System.out.println(received);
        }
    }
}
