package ai.konduitai.serving.pipeline.protocol;

import ai.konduit.serving.build.remote.RemoteUtils;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertEquals;

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


}
