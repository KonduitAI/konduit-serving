package ai.konduit.serving.pipeline.api.protocol;

public interface NetClient {
    void connect();

    boolean login();
}
