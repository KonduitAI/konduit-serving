package ai.konduit.serving.util;

import java.io.IOException;
import java.net.ServerSocket;

public class PortUtils {

    /**
     * @return single available port number
     */
    public static int getAvailablePort() {
        try {
            try (ServerSocket socket = new ServerSocket(0)) {
                return socket.getLocalPort();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot find available port: " + e.getMessage(), e);
        }
    }
}
