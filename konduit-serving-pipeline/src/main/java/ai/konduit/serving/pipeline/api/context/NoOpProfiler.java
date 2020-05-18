package ai.konduit.serving.pipeline.api.context;

public class NoOpProfiler implements Profiler {

    @Override
    public boolean profilerEnabled() {
        return false;
    }

    @Override
    public void eventStart(String key) {
    }

    @Override
    public void eventEnd(String key) {
    }
}
