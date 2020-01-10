package ai.konduit.serving.miscutils;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.file.Paths;

@Parameters(commandNames = "clean-javacpp-cache",
        commandDescription = "Tool for clearing numpy javacpp cache",
        separators = "=")
public class CleanNumpyJavaCppCache {

    @Parameter(names = "--numpyJavacppVersion", required = true, description = "Numpy version that's used with javacpp")
    private String numpyJavacppVersion = null;

    @Parameter(names = "--javacppPlatform", required = true, description = "The javacpp platform")
    private String javacppPlatform = null;

    @Parameter(names = {"--help", "-h"}, description = "To see help menu.")
    boolean help = false;

    public CleanNumpyJavaCppCache() {
    }

    public static void main(String[] args) {
        new CleanNumpyJavaCppCache().runMain(args);
    }

    private void runMain(String[] args) {
        JCommander jCommander = new JCommander(this);

        try {
            jCommander.parse(args);

            if (help) {
                jCommander.usage();
            } else {
                File numpyJavacppCacheFolder = new File(Paths.get(System.getProperty("user.home"),
                        ".javacpp", "cache",
                        String.format("numpy-%s-%s.jar", numpyJavacppVersion, javacppPlatform)).toString());

                if(numpyJavacppCacheFolder.isDirectory()) {
                    try {
                        FileUtils.deleteDirectory(numpyJavacppCacheFolder);
                        System.out.println("Successfully deleted numpy javacpp cache at: " + numpyJavacppCacheFolder.getAbsolutePath());
                    } catch (Exception exception) {
                        System.out.println("Unable to delete numpy javacpp cache at: " + numpyJavacppCacheFolder.getAbsolutePath());
                        exception.printStackTrace();
                    }
                } else {
                    System.out.println("No numpy javacpp cache exists at: " + numpyJavacppCacheFolder.getAbsolutePath());
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            jCommander.usage();
        }
    }
}
