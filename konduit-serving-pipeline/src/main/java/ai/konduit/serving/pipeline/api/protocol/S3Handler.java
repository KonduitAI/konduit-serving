package ai.konduit.serving.pipeline.api.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import org.jets3t.service.ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.security.AWSCredentials;

public class S3Handler extends URLStreamHandler {

    protected URLConnection openConnection(URL url) throws IOException {

        return new URLConnection(url) {

            @Override
            public InputStream getInputStream() throws IOException {

                //aws credentials
                String accessKey = null;
                String secretKey = null;

                if (url.getUserInfo() != null) {
                    String[] credentials = url.getUserInfo().split("[:]");
                    accessKey = credentials[0];
                    secretKey = credentials[1];
                }

                String bucket = url.getHost().substring(0, url.getHost().indexOf("."));
                String key = url.getPath().substring(1);

                try {
                    RestS3Service s3Service = new RestS3Service(new AWSCredentials(accessKey, secretKey));
                    S3Object s3obj = s3Service.getObject(bucket, key);
                    return s3obj.getDataInputStream();

                } catch (ServiceException e) {
                    throw new IOException(e);
                }
            }

            @Override
            public void connect() throws IOException { }

        };
    }
}