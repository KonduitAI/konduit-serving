package ai.konduit.serving.pipeline.api.protocol.handlers;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

public class S3StreamHandlerFactory implements URLStreamHandlerFactory {

    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
        if ("s3".equals(protocol)) {
            return new S3Handler();
        }
        return null;
    }

}
