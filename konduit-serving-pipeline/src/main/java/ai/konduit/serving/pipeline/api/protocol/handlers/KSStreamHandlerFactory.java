package ai.konduit.serving.pipeline.api.protocol.handlers;

import org.apache.hadoop.fs.FsUrlStreamHandlerFactory;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

public class KSStreamHandlerFactory implements URLStreamHandlerFactory {

    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
        if ("s3".equals(protocol)) {
            return new S3Handler();
        }
        else if ("hdfs".equals(protocol)) {
            return new FsUrlStreamHandlerFactory().createURLStreamHandler("hdfs");
        }
        return null;
    }

}
