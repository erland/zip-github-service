package info.isaksson.erland.zipbuildserver.domain.model.verification;

import info.isaksson.erland.zipbuildserver.domain.model.NetworkMode;
import info.isaksson.erland.zipbuildserver.domain.model.project.ProjectTechnology;

import java.util.List;

public record VerificationPlan(
        String id,
        String name,
        ProjectTechnology technology,
        List<String> indicators,
        List<VerificationCommand> commands,
        NetworkMode networkMode,
        boolean enabled,
        String selectionReason) {
}
