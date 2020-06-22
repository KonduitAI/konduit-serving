package ai.konduit.serving.pipeline.api.protocol;

public class HdfsClient implements NetClient {
    @Override
    public void connect() {

    }

    @Override
    public boolean login() {
        return false;
    }
}
