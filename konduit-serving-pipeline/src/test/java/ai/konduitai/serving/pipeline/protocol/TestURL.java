package ai.konduitai.serving.pipeline.protocol;

import ai.konduit.serving.pipeline.api.protocol.RemoteUtils;
import ai.konduit.serving.pipeline.api.protocol.S3Handler;
import ai.konduitai.serving.common.test.TestServer;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

import static junit.framework.Assert.assertTrue;

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
        System.setProperty("java.protocol.handler.pkgs",
                "ai.konduit.serving.pipeline.api.protocol");
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

    @Ignore
    @Test
    public void testFTP() throws IOException, InterruptedException {
        FakeFtpServer fakeFtpServer = new FakeFtpServer();
        fakeFtpServer.addUserAccount(new UserAccount("user", "password",
                                    testDir.newFolder().getAbsolutePath()));
        fakeFtpServer.start();

        FTPClient ftp = new FTPClient();
        ftp.connect("localhost");
        ftp.login("user", "password");
        FTPFile[] files = ftp.listFiles(".");

        String url = FTP + "user:password@" + HOST + ":" + 21 + "/src/test/resources/config/config.json";
        String data = RemoteUtils.configFromHttp(url);
        assertTrue(data.contains("pipelineSteps"));
    }

    @Ignore
    @Test
    public void testS3() throws IOException {
        System.out.println("Registered protocols:" + System.getProperty("java.protocol.handler.pkgs"));
        URL url = new URL("s3://<access-key>:<secret-key>@<bucket>.s3.amazonaws.com/<filename>");

        URLConnection conn = url.openConnection();
        InputStream is = conn.getInputStream();

        if (is != null) {

            Writer writer = new StringWriter();
            char[] buffer = new char[1024];
            try {
                Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                is.close();
            }

            System.out.println("connected");
            System.out.println(writer.toString());

        }
    }
}
