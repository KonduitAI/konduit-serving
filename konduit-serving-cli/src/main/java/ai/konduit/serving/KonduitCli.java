package ai.konduit.serving;

import ai.konduit.serving.ai.konduit.serving.commands.PingCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "konduit", version = "1.0.0", subcommands = {
        PingCommand.class
})
public class KonduitCli {
    @Option(names = { "-h", "--help" }, usageHelp = true, description = "display this help and exit")
    boolean showHelp;

    @Option(names = {"-v", "--version"}, versionHelp = true, description = "display version and exit.")
    boolean showVersion;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new KonduitCli()).execute(args);
        System.exit(exitCode);
    }
}
