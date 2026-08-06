package info.isaksson.erland.zipbuildserver.application.project;

import info.isaksson.erland.zipbuildserver.domain.model.project.DetectedProject;
import info.isaksson.erland.zipbuildserver.domain.model.project.ProjectDetectionSummary;
import info.isaksson.erland.zipbuildserver.application.verification.VerificationPlanService;
import info.isaksson.erland.zipbuildserver.domain.model.project.ProjectTechnology;
import info.isaksson.erland.zipbuildserver.domain.model.verification.VerificationPlanSelection;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@ApplicationScoped
public class ProjectDetectionService {
    private final VerificationPlanService verificationPlanService;

    public ProjectDetectionService(VerificationPlanService verificationPlanService) {
        this.verificationPlanService = verificationPlanService;
    }

    public ProjectDetectionSummary detect(Path zipPath) {
        Set<String> entries = readEntryNames(zipPath);
        List<DetectedProject> projects = new ArrayList<>();

        boolean backendMaven = entries.contains("backend/pom.xml");
        boolean frontendNode = entries.contains("frontend/package.json");
        if (backendMaven && frontendNode) {
            projects.add(createDetectedProject(
                    ".",
                    ProjectTechnology.MULTI_PROJECT,
                    List.of("backend/pom.xml", "frontend/package.json"),
                    "Detected backend Maven and frontend Node project indicators."));
        }

        for (String pom : sortedMatching(entries, "pom.xml")) {
            if (isCoveredByMultiProject(pom, projects)) {
                continue;
            }
            projects.add(createDetectedProject(
                    parentPath(pom),
                    ProjectTechnology.MAVEN,
                    List.of(pom),
                    "Detected Maven project indicator."));
        }

        for (String packageJson : sortedMatching(entries, "package.json")) {
            if (isCoveredByMultiProject(packageJson, projects)) {
                continue;
            }
            projects.add(createDetectedProject(
                    parentPath(packageJson),
                    ProjectTechnology.NODE,
                    List.of(packageJson),
                    "Detected Node project indicator."));
        }

        projects.sort(Comparator.comparing(DetectedProject::path).thenComparing(project -> project.technology().name()));
        if (projects.isEmpty()) {
            return ProjectDetectionSummary.unsupported("No supported Maven or Node project indicators were detected.");
        }
        return new ProjectDetectionSummary(projects, true, "Detected " + projects.size() + " supported project(s).");
    }

    private DetectedProject createDetectedProject(
            String path,
            ProjectTechnology technology,
            List<String> buildIndicators,
            String detectionReason) {
        DetectedProject detectedProject = new DetectedProject(path, technology, buildIndicators, null, detectionReason);
        VerificationPlanSelection selection = verificationPlanService.selectPlan(detectedProject);
        return new DetectedProject(
                path,
                technology,
                buildIndicators,
                selection.selectedPlanId(),
                detectionReason + " " + selection.reason());
    }

    private Set<String> readEntryNames(Path zipPath) {
        Set<String> entries = new LinkedHashSet<>();
        try (InputStream input = java.nio.file.Files.newInputStream(zipPath);
             ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    entries.add(normalize(entry.getName()));
                }
                zip.closeEntry();
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read package for project detection.", exception);
        }
        return entries;
    }

    private List<String> sortedMatching(Set<String> entries, String filename) {
        return entries.stream()
                .filter(entry -> entry.equals(filename) || entry.endsWith("/" + filename))
                .sorted()
                .toList();
    }

    private boolean isCoveredByMultiProject(String indicator, List<DetectedProject> projects) {
        return projects.stream().anyMatch(project ->
                project.technology() == ProjectTechnology.MULTI_PROJECT
                        && (indicator.equals("backend/pom.xml") || indicator.equals("frontend/package.json")));
    }

    private String parentPath(String entryName) {
        int slash = entryName.lastIndexOf('/');
        if (slash < 0) {
            return ".";
        }
        return entryName.substring(0, slash);
    }

    private String normalize(String entryName) {
        return entryName.replace('\\', '/');
    }
}
