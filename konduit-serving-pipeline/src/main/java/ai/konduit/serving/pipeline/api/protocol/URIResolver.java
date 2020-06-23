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

    public static File getCachedFile(String uri) {
        URI u = URI.create(uri);
        String fullPath = StringUtils.defaultIfEmpty(u.getScheme(), StringUtils.EMPTY);
        System.out.println(u.getPath());
        String[] dirs = u.getPath().split("/");
        fullPath += File.separator + FilenameUtils.getName(uri);

        if (cacheDirectory == null) {
            File f = new File(System.getProperty("user.home"), ".konduit_cache/");
            if (!f.exists())
                f.mkdirs();
            cacheDirectory = f;
        }
        File effectiveDirectory = new File(cacheDirectory, fullPath);
        return effectiveDirectory;
    }

    public static File getFile(String uri) throws IOException {

        URI u = URI.create(uri);
        String scheme = u.getScheme();
        if (scheme.equals("file")) {
            return new File(u.getPath());
        }
        File cachedFile = getCachedFile(uri);
        if (cachedFile.exists()) {
            return cachedFile;
        }

        URL url = u.toURL();
        URLConnection connection = url.openConnection();
        FileUtils.copyURLToFile(url, cachedFile);

        return cachedFile;
    }
}
