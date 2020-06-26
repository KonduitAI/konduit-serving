package ai.konduit.serving.pipeline.api.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;

public interface URLAccessProvider {

    Credentials getCredentials();

    InputStream connect(URL url, Credentials credentials) throws IOException, URISyntaxException;
}
