package ai.konduit.serving.pipeline.api.protocol;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;

public class RemoteUtils {

    public static String configFromHttp(String uri) throws IOException {

        URI u = URI.create(uri);
        URL url = u.toURL();
        URLConnection connection = url.openConnection();

        StringBuilder content = new StringBuilder();
        try(BufferedReader br =
                    new BufferedReader(new InputStreamReader(connection.getInputStream()))) {

            String data = StringUtils.EMPTY;
            while ((data = br.readLine()) != null)
                content.append(data);
        }
        return content.toString();
    }
}
