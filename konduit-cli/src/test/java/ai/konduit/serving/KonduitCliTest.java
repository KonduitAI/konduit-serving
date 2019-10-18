package ai.konduit.serving;

import org.junit.Test;
import picocli.CommandLine;

import static org.junit.Assert.*;

public class KonduitCliTest {
    @Test
    public void shouldPingCorrectly() {
        int exitCode = new CommandLine(new KonduitCli()).execute("ping");

        assertEquals(0, exitCode);
    }

    @Test
    public void shouldShowHelp() {
        KonduitCli cli = new KonduitCli();
        int exitcode = new CommandLine(cli).execute("-h");

        assertEquals(0, exitcode);
        assertTrue(cli.showHelp);
        assertFalse(cli.showVersion);
    }

    @Test
    public void shouldShowVersion() {
        KonduitCli cli = new KonduitCli();
        int exitCode = new CommandLine(cli).execute("-v");

        assertEquals(0, exitCode);
        assertTrue(cli.showVersion);
        assertFalse(cli.showHelp);
    }
}
