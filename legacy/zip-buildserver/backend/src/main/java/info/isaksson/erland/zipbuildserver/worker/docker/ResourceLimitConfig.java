package info.isaksson.erland.zipbuildserver.worker.docker;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ResourceLimitConfig {
    private final String image;
    private final String memoryLimit;
    private final String cpuLimit;
    private final String networkMode;
    private final int maxOutputBytes;
    private final String workspaceContainerDirectory;
    private final String workspaceHostDirectory;

    public ResourceLimitConfig(
            @ConfigProperty(name = "zip-buildserver.worker.docker.image", defaultValue = "zip-buildserver-worker-node-maven:local")
            String image,
            @ConfigProperty(name = "zip-buildserver.worker.docker.memory", defaultValue = "2g")
            String memoryLimit,
            @ConfigProperty(name = "zip-buildserver.worker.docker.cpus", defaultValue = "2")
            String cpuLimit,
            @ConfigProperty(name = "zip-buildserver.worker.docker.network", defaultValue = "bridge")
            String networkMode,
            @ConfigProperty(name = "zip-buildserver.worker.max-output-bytes", defaultValue = "1048576")
            int maxOutputBytes,
            @ConfigProperty(name = "zip-buildserver.storage.workspaces-dir", defaultValue = "target/zip-buildserver/workspaces")
            String workspaceContainerDirectory,
            @ConfigProperty(name = "zip-buildserver.worker.docker.host-workspaces-dir", defaultValue = "")
            String workspaceHostDirectory) {
        this.image = image;
        this.memoryLimit = memoryLimit;
        this.cpuLimit = cpuLimit;
        this.networkMode = networkMode;
        this.maxOutputBytes = maxOutputBytes;
        this.workspaceContainerDirectory = workspaceContainerDirectory;
        this.workspaceHostDirectory = workspaceHostDirectory;
    }

    public String image() {
        return image;
    }

    public String memoryLimit() {
        return memoryLimit;
    }

    public String cpuLimit() {
        return cpuLimit;
    }

    public String networkMode() {
        return networkMode;
    }

    public int maxOutputBytes() {
        return maxOutputBytes;
    }

    public String workspaceContainerDirectory() {
        return workspaceContainerDirectory;
    }

    public String workspaceHostDirectory() {
        return workspaceHostDirectory;
    }
}
