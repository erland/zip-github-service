package info.isaksson.erland.zipbuildserver.worker.docker;

import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class DockerContainerCleanup {
    private final ProcessStarter processStarter;

    public DockerContainerCleanup() {
        this(command -> new ProcessBuilder(command).start());
    }

    DockerContainerCleanup(ProcessStarter processStarter) {
        this.processStarter = processStarter;
    }

    public void cleanup(String containerName) {
        try {
            Process process = processStarter.start(List.of("docker", "rm", "-f", containerName));
            process.waitFor(5, TimeUnit.SECONDS);
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    interface ProcessStarter {
        Process start(List<String> command) throws IOException;
    }
}
