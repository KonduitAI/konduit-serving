package ai.konduit.serving.pipeline.api.protocol;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

public class URIResolver {

    public static boolean isUrl(String input) {
        if (input.startsWith("http://") ||
            input.startsWith("https://") ||
            input.startsWith("ftp://")) {
            return true;
        }
        return false;
    }

    private static File cacheDirectory;

    public static File getCacheDirectory() {
        if (cacheDirectory == null) {
            File f = new File(System.getProperty("user.home"), ".konduit_cache/");
            if (!f.exists())
                f.mkdirs();
            cacheDirectory = f;
        }
        return cacheDirectory;
    }

    public static File getFile(String uri) throws IOException {

        String fileName = FilenameUtils.getName(uri);
        File cachedFile = new File(getCacheDirectory(), fileName);
        if (cachedFile.exists()) {
            return cachedFile;
        }

        URI u = URI.create(uri);
        URL url = u.toURL();
        URLConnection connection = url.openConnection();
        FileUtils.copyURLToFile(url, cachedFile);
        return cachedFile;
    }
}
