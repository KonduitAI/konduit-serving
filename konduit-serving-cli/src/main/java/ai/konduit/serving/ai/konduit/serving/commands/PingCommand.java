package ai.konduit.serving.ai.konduit.serving.commands;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "ping")
public class PingCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("Pong!");
    }
}
