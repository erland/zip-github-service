package info.isaksson.erland.zipbuildserver.worker.docker;

import info.isaksson.erland.zipbuildserver.worker.CommandExecutionRequest;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class DockerRunCommandBuilder {
    private final ResourceLimitConfig resourceLimitConfig;
    private final DockerWorkspacePathMapper workspacePathMapper;

    public DockerRunCommandBuilder(ResourceLimitConfig resourceLimitConfig, DockerWorkspacePathMapper workspacePathMapper) {
        this.resourceLimitConfig = resourceLimitConfig;
        this.workspacePathMapper = workspacePathMapper;
    }

    List<String> build(CommandExecutionRequest request, String containerName) {
        String hostWorkspace = workspacePathMapper.hostWorkspacePath(request.workspaceRoot());
        String containerWorkingDirectory = workspacePathMapper.containerWorkingDirectory(request.workingDirectory());

        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("run");
        command.add("--rm");
        command.add("--name");
        command.add(containerName);
        command.add("--network");
        command.add(resourceLimitConfig.networkMode());
        command.add("--memory");
        command.add(resourceLimitConfig.memoryLimit());
        command.add("--cpus");
        command.add(resourceLimitConfig.cpuLimit());
        command.add("-v");
        command.add(hostWorkspace + ":/workspace:rw");
        command.add("-w");
        command.add(containerWorkingDirectory);
        command.add(resourceLimitConfig.image());
        command.add("/bin/sh");
        command.add("-lc");
        command.add(request.commandDisplay());
        return command;
    }

}
