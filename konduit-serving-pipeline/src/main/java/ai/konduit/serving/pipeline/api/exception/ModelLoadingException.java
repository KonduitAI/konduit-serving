package ai.konduit.serving.pipeline.api.exception;

public class ModelLoadingException extends RuntimeException {

    public ModelLoadingException(String message) {
        super(message);
    }

    public ModelLoadingException(String message, Throwable cause) {
        super(message, cause);
    }

    public ModelLoadingException(Throwable cause) {
        super(cause);
    }
}
