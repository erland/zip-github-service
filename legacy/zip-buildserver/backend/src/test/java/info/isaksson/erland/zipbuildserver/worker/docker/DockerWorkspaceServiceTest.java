package info.isaksson.erland.zipbuildserver.worker.docker;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DockerWorkspaceServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void extractsZipIntoTemporaryWorkspaceAndCleansItUp() throws IOException {
        Path zip = tempDir.resolve("package.zip");
        createZip(zip, "backend/pom.xml", "<project />");

        DockerWorkspaceService service = new DockerWorkspaceService(tempDir.resolve("workspaces").toString());
        Path workspace = service.createWorkspace(zip);

        assertTrue(Files.exists(workspace.resolve("backend/pom.xml")));
        assertEquals("<project />", Files.readString(workspace.resolve("backend/pom.xml")));

        service.cleanup(workspace);

        assertFalse(Files.exists(workspace));
    }

    private void createZip(Path zipPath, String name, String content) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            zip.putNextEntry(new ZipEntry(name));
            zip.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zip.closeEntry();
        }
    }
}
