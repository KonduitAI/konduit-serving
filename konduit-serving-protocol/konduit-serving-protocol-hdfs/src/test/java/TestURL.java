import ai.konduit.serving.common.test.BaseHttpUriTest;
import ai.konduit.serving.pipeline.api.protocol.Credentials;
import lombok.val;
import org.apache.commons.io.FileUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import providers.HdfsAccessProvider;
import providers.HdfsStreamHandlerFactory;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLStreamHandlerFactory;

import static junit.framework.TestCase.assertTrue;

public class TestURL extends BaseHttpUriTest {

    @Rule
    public TemporaryFolder testDir = new TemporaryFolder();

    private File hdfsPath;

    @Before
    public void setUp() throws Exception {
        Configuration conf = new HdfsConfiguration();
        conf.set("fs.defaultFS", "hdfs://localhost");
        hdfsPath = new File(testDir + File.separator + "hadoop" + File.separator + "hdfs");
        conf.set(MiniDFSCluster.HDFS_MINIDFS_BASEDIR, hdfsPath.getAbsolutePath());
        MiniDFSCluster miniDFSCluster = new MiniDFSCluster.Builder(conf)
                .nameNodePort(1234)
                .nameNodeHttpPort(12341)
                .numDataNodes(1)
                .format(true)
                .racks(null)
                .build();
        miniDFSCluster.waitActive();
    }

    @Test
    public void testHdfsAccess() throws IOException, URISyntaxException {
        URL url = new URL("hdfs://localhost:1234/user/root/config.json");
        HdfsAccessProvider accessProvider = new HdfsAccessProvider();
        InputStream input = accessProvider.connect(url, new Credentials(System.getenv("HDFS_ACCESS_KEY"),
                System.getenv("HDFS_SECRET_KEY")));
        File targetFile = new File(hdfsPath, "config.json");
        FileUtils.copyInputStreamToFile(input, targetFile);
        assertTrue(targetFile.exists());
    }

    @Override
    public URLStreamHandlerFactory streamHandler() {
        return new HdfsStreamHandlerFactory();
    }
}
