package ai.konduit.serving.build.deployments;

import ai.konduit.serving.build.config.Deployment;
import ai.konduit.serving.build.config.DeploymentValidation;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.nd4j.shade.jackson.annotation.JsonProperty;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

@Data
@Accessors(fluent = true)
@NoArgsConstructor
public class ExeDeployment implements Deployment {
    public static final String DEFAULT_EXE_NAME = "konduit-serving-deployment.exe";
    public static final String PROP_OUTPUTDIR = "exe.outputdir";
    public static final String PROP_EXENAME = "exe.name";

    private String outputDir;
    private String exeName;
    private String version;

    public ExeDeployment(String outputDir) {
        this(outputDir, "ks", defaultVersion());
    }

    public ExeDeployment(@JsonProperty("outputDir") String outputDir, @JsonProperty("exeName") String exeName,
                         @JsonProperty("version") String version){
        this.outputDir = outputDir;
        this.exeName = exeName;
        this.version = version;
    }

    private static String defaultVersion(){
        long time = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("YYYYMMDD-HHmmss.SSS");
        return sdf.format(new Date(time));
    }

    @Override
    public List<String> propertyNames() {
        return Arrays.asList(PROP_OUTPUTDIR, PROP_EXENAME);
    }

    @Override
    public Map<String, String> asProperties() {
        Map<String,String> m = new LinkedHashMap<>();
        m.put(PROP_OUTPUTDIR, outputDir);
        m.put(PROP_EXENAME, exeName);
        return m;
    }

    @Override
    public void fromProperties(Map<String, String> props) {
        outputDir = props.getOrDefault(PROP_OUTPUTDIR, outputDir);
        exeName = props.getOrDefault(PROP_EXENAME, exeName);
    }

    @Override
    public DeploymentValidation validate() {
        return null;
    }

    @Override
    public String outputString() {
        File outFile = new File(outputDir, exeName);
        StringBuilder sb = new StringBuilder();
        sb.append("EXE location:   ").append(outFile.getAbsolutePath()).append("\n");
        String size;
        if(outFile.exists()){
            long bytes = outFile.length();
            double bytesPerMB = 1024 * 1024;
            double mb = bytes / bytesPerMB;
            size = String.format("%.2f", mb) + " MB";
        } else {
            size = "<EXE not found>";
        }
        sb.append("EXE size:       ").append(size);

        return sb.toString();
    }
}
