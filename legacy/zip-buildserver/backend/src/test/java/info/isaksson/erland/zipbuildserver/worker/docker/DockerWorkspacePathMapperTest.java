package info.isaksson.erland.zipbuildserver.worker.docker;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DockerWorkspacePathMapperTest {
    @TempDir
    Path workspace;

    @Test
    void mapsContainerWorkspacePathToConfiguredHostWorkspaceRoot() {
        Path containerRoot = workspace.getParent().resolve("container-workspaces");
        Path containerWorkspace = containerRoot.resolve("session-1").resolve("package");
        Path hostRoot = workspace.getParent().resolve("host-workspaces");

        DockerWorkspacePathMapper mapper = new DockerWorkspacePathMapper(new ResourceLimitConfig(
                "worker-image",
                "512m",
                "1",
                "none",
                4096,
                containerRoot.toString(),
                hostRoot.toString()));

        assertEquals(
                hostRoot.toAbsolutePath().normalize().resolve("session-1/package").toString(),
                mapper.hostWorkspacePath(containerWorkspace));
    }

    @Test
    void keepsContainerWorkspacePathWhenHostWorkspaceRootIsBlank() {
        DockerWorkspacePathMapper mapper = new DockerWorkspacePathMapper(new ResourceLimitConfig(
                "worker-image",
                "512m",
                "1",
                "none",
                4096,
                workspace.getParent().toString(),
                ""));

        assertEquals(
                workspace.toAbsolutePath().normalize().toString(),
                mapper.hostWorkspacePath(workspace));
    }

    @Test
    void keepsContainerWorkspacePathWhenWorkspaceIsOutsideConfiguredContainerRoot() {
        DockerWorkspacePathMapper mapper = new DockerWorkspacePathMapper(new ResourceLimitConfig(
                "worker-image",
                "512m",
                "1",
                "none",
                4096,
                workspace.getParent().resolve("different-root").toString(),
                workspace.getParent().resolve("host-workspaces").toString()));

        assertEquals(
                workspace.toAbsolutePath().normalize().toString(),
                mapper.hostWorkspacePath(workspace));
    }

    @Test
    void mapsBlankDotAndNestedWorkingDirectoriesIntoContainerWorkspace() {
        DockerWorkspacePathMapper mapper = new DockerWorkspacePathMapper(new ResourceLimitConfig(
                "worker-image",
                "512m",
                "1",
                "none",
                4096,
                workspace.getParent().toString(),
                workspace.getParent().toString()));

        assertEquals("/workspace", mapper.containerWorkingDirectory(null));
        assertEquals("/workspace", mapper.containerWorkingDirectory(""));
        assertEquals("/workspace", mapper.containerWorkingDirectory("."));
        assertEquals("/workspace/frontend", mapper.containerWorkingDirectory("./frontend"));
        assertEquals("/workspace/apps/web", mapper.containerWorkingDirectory("apps\\web"));
    }
}
