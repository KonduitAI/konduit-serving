package ai.konduit.serving.pipeline.api.protocol;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTPClient;

import java.io.IOException;
import java.io.PrintWriter;

public class FtpClient implements NetClient {

    private FTPClient ftp = new FTPClient();
    private int port = 21;

    @Override
    public void connect(String host) throws IOException {
        ftp.connect("localhost", port);
        ftp.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));
    }

    @Override
    public boolean login(String user, String password) throws IOException {
        return ftp.login(user, password);
    }
}
