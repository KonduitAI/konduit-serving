package ai.konduit.serving.protocols.test;

import ai.konduit.serving.common.test.BaseHttpUriTest;
import ai.konduit.serving.protocols.providers.S3StreamHandlerFactory;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandlerFactory;

public class TestURL extends BaseHttpUriTest {

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

    @Override
    public URLStreamHandlerFactory streamHandler() {
        return new S3StreamHandlerFactory();
    }
}