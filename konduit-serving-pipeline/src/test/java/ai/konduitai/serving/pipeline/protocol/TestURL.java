package ai.konduitai.serving.pipeline.protocol;

import ai.konduit.serving.build.remote.RemoteUtils;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

import static junit.framework.TestCase.assertTrue;

public class TestURL {

    private TestServer server;
    private final static int PORT = 9090;
    private final static String HOST = "localhost";
    private static final String HTTP = "http://";

    @Rule
    public TemporaryFolder testDir = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        //System.setProperty("java.protocol.handler.pkgs", "HttpURLStreamHandler");
        server = new TestServer(HTTP, HOST, PORT);
        server.start();
    }

    @After
    public void tearDown() throws Exception {
        server.stop();
    }

    @Test
    public void testHttpURL() throws IOException {
        String pbUrl = HTTP + HOST + ":" + PORT + "/src/test/resources/config/config.json";
        URI u = URI.create(pbUrl);
        URL url = u.toURL();
        URLConnection connection = url.openConnection();

        File dir = testDir.newFolder();
        File localFile = new File(dir, "models/config.json");
        String data = RemoteUtils.configFromHttp(pbUrl);
        try (PrintWriter pw = new PrintWriter(new FileOutputStream(localFile))) {
            pw.write(data);
        }

        assertTrue(localFile.exists());
    }
}
