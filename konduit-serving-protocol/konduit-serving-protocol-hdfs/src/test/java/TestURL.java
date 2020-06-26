import ai.konduit.serving.common.test.BaseHttpUriTest;
import ai.konduit.serving.pipeline.api.protocol.Credentials;
import org.junit.Test;
import providers.HdfsAccessProvider;
import providers.HdfsStreamHandlerFactory;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLStreamHandlerFactory;

public class TestURL extends BaseHttpUriTest {

    @Test
    public void testHdfsAccess() throws IOException, URISyntaxException {
        System.setProperty("hadoop.home.dir", "D:\\install\\hadoop-2.8.0\\bin");
        URL url = new URL("hdfs://localhost:9000/user/root/config.json");
        HdfsAccessProvider accessProvider = new HdfsAccessProvider();
        accessProvider.connect(url, new Credentials(System.getenv("HDFS_ACCESS_KEY"),
                System.getenv("HDFS_SECRET_KEY")));
    }

    @Override
    public URLStreamHandlerFactory streamHandler() {
        return new HdfsStreamHandlerFactory();
    }
}
