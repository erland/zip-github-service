package info.isaksson.erland.zipbuildserver.worker.docker;

import jakarta.enterprise.context.ApplicationScoped;

import java.nio.file.Path;

@ApplicationScoped
public class DockerWorkspacePathMapper {
    private final ResourceLimitConfig resourceLimitConfig;

    public DockerWorkspacePathMapper(ResourceLimitConfig resourceLimitConfig) {
        this.resourceLimitConfig = resourceLimitConfig;
    }

    String hostWorkspacePath(Path workspaceRoot) {
        String containerWorkspacePath = workspaceRoot.toAbsolutePath().normalize().toString();
        String containerRoot = resourceLimitConfig.workspaceContainerDirectory();
        String hostRoot = resourceLimitConfig.workspaceHostDirectory();
        if (hostRoot == null || hostRoot.isBlank()) {
            return containerWorkspacePath;
        }
        Path normalizedContainerRoot = Path.of(containerRoot).toAbsolutePath().normalize();
        Path normalizedWorkspace = Path.of(containerWorkspacePath).toAbsolutePath().normalize();
        if (!normalizedWorkspace.startsWith(normalizedContainerRoot)) {
            return containerWorkspacePath;
        }
        Path relative = normalizedContainerRoot.relativize(normalizedWorkspace);
        return Path.of(hostRoot).toAbsolutePath().normalize().resolve(relative).toString();
    }

    String containerWorkingDirectory(String workingDirectory) {
        if (workingDirectory == null || workingDirectory.isBlank() || ".".equals(workingDirectory)) {
            return "/workspace";
        }
        String normalized = workingDirectory.replace('\\', '/');
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        return "/workspace/" + normalized;
    }
}
