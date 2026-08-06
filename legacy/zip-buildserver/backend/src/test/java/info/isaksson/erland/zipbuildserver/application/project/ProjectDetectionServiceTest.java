package info.isaksson.erland.zipbuildserver.application.project;

import info.isaksson.erland.zipbuildserver.application.verification.VerificationPlanService;
import info.isaksson.erland.zipbuildserver.domain.model.project.ProjectDetectionSummary;
import info.isaksson.erland.zipbuildserver.domain.model.project.ProjectTechnology;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectDetectionServiceTest {
    private final ProjectDetectionService service = new ProjectDetectionService(new VerificationPlanService());

    @Test
    void detectsMavenProjectAtRoot() throws IOException {
        Path zip = createZip("pom.xml");

        ProjectDetectionSummary summary = service.detect(zip);

        assertTrue(summary.supported());
        assertEquals(1, summary.projects().size());
        assertEquals(".", summary.projects().getFirst().path());
        assertEquals(ProjectTechnology.MAVEN, summary.projects().getFirst().technology());
        assertEquals("maven-default", summary.projects().getFirst().selectedPlanId());
    }

    @Test
    void detectsNodeProjectAtRoot() throws IOException {
        Path zip = createZip("package.json");

        ProjectDetectionSummary summary = service.detect(zip);

        assertTrue(summary.supported());
        assertEquals(1, summary.projects().size());
        assertEquals(ProjectTechnology.NODE, summary.projects().getFirst().technology());
        assertEquals("node-default", summary.projects().getFirst().selectedPlanId());
    }

    @Test
    void detectsBackendFrontendMultiProjectLayout() throws IOException {
        Path zip = createZip("backend/pom.xml", "frontend/package.json");

        ProjectDetectionSummary summary = service.detect(zip);

        assertTrue(summary.supported());
        assertEquals(1, summary.projects().size());
        assertEquals(".", summary.projects().getFirst().path());
        assertEquals(ProjectTechnology.MULTI_PROJECT, summary.projects().getFirst().technology());
        assertEquals("multi-project-default", summary.projects().getFirst().selectedPlanId());
    }

    @Test
    void reportsUnsupportedPackagesWithoutProjects() throws IOException {
        Path zip = createZip("README.md", "docs/notes.md");

        ProjectDetectionSummary summary = service.detect(zip);

        assertFalse(summary.supported());
        assertTrue(summary.projects().isEmpty());
    }

    private Path createZip(String... entries) throws IOException {
        Path zip = Files.createTempFile("project-detection-", ".zip");
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(zip))) {
            for (String entryName : entries) {
                output.putNextEntry(new ZipEntry(entryName));
                output.write("placeholder".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                output.closeEntry();
            }
        }
        return zip;
    }
}
