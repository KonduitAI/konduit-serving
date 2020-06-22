package ai.konduit.serving.pipeline.api.protocol;

public class AzureClient implements NetClient {
    @Override
    public void connect(String host) {

    }

    @Override
    public boolean login(String user, String password) {
        return false;
    }
}
