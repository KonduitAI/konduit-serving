package ai.konduitai.serving.pipeline.protocol;

import ai.konduit.serving.pipeline.api.protocol.FtpClient;
import ai.konduit.serving.pipeline.api.protocol.RemoteUtils;
import ai.konduit.serving.pipeline.api.protocol.handlers.S3StreamHandlerFactory;
import ai.konduitai.serving.common.test.TestServer;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
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

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

public class TestURL {

    private TestServer server;
    private final static int PORT = 9090;
    private final static String HOST = "localhost";
    private static final String HTTP = "http://";
    private static final String HTTPS = "http://";
    private static final String FTP = "ftp://";

    private String configData = StringUtils.EMPTY;

    @Rule
    public TemporaryFolder testDir = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        URL.setURLStreamHandlerFactory(new S3StreamHandlerFactory());
        /*System.setProperty("java.protocol.handler.pkgs",
                "ai.konduit.serving.pipeline.api.protocol.handlers");*/
        server = new TestServer(HTTP, HOST, PORT);
        server.start();
        configData = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("config/config.json"),
                "UTF-8");
    }

    @After
    public void tearDown() throws Exception {
        server.stop();
    }

    @Test
    public void testHttpURL() throws Exception {
        String url = HTTP + HOST + ":" + PORT + "/src/test/resources/config/config.json";
        String data = RemoteUtils.configFromHttp(url);
        assertTrue(data.contains("pipelineSteps"));

        url = HTTPS + HOST + ":" + PORT + "/src/test/resources/config/config.json";
        data = RemoteUtils.configFromHttp(url);
        assertTrue(data.contains("pipelineSteps"));
    }

    @Ignore("Windows bound manual test")
    @Test
    public void testFTP() throws IOException, InterruptedException {
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
            String data = RemoteUtils.configFromHttp(url);
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
            Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String received = IOUtils.toString(reader);
            System.out.println(received);
        }
    }

    @Ignore
    @Test
    public void testHDFS() throws IOException {
        URL url = new URL("hdfs://someurl");

        URLConnection conn = url.openConnection();
        try (InputStream is = conn.getInputStream()) {
            Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String received = IOUtils.toString(reader);
            System.out.println(received);
        }
    }
}
