package info.isaksson.erland.zipbuildserver.domain.model.project;

import java.util.List;

public record DetectedProject(
        String path,
        ProjectTechnology technology,
        List<String> buildIndicators,
        String selectedPlanId,
        String selectionReason) {
}
