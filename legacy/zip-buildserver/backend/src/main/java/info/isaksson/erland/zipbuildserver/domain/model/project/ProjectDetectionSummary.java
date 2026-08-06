package info.isaksson.erland.zipbuildserver.domain.model.project;

import java.util.List;

public record ProjectDetectionSummary(
        List<DetectedProject> projects,
        boolean supported,
        String message) {
    public static ProjectDetectionSummary unsupported(String message) {
        return new ProjectDetectionSummary(List.of(), false, message);
    }
}
