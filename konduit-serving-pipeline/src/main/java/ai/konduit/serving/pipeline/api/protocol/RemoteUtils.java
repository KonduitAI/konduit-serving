package ai.konduit.serving.build.remote;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

public class RemoteUtils {

    public static String configFromHttp(String uri) throws IOException {
        String content = StringUtils.EMPTY;
        URI u = URI.create(uri);
        URL url = u.toURL();
        URLConnection connection = url.openConnection();

        try(InputStream is = connection.getInputStream()) {
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            content = IOUtils.toString(is);
        }
        return content;
    }
}
