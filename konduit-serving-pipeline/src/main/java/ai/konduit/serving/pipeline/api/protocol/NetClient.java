package ai.konduit.serving.pipeline.api.protocol;

import java.io.IOException;

public interface NetClient {
    void connect(String host) throws IOException;

    boolean login(String user, String password) throws IOException;
}
