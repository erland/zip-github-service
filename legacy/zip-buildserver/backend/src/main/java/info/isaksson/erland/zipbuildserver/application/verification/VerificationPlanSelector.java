package info.isaksson.erland.zipbuildserver.application.verification;

import info.isaksson.erland.zipbuildserver.domain.model.project.DetectedProject;
import info.isaksson.erland.zipbuildserver.domain.model.verification.VerificationPlan;
import info.isaksson.erland.zipbuildserver.domain.model.verification.VerificationPlanSelection;

import java.util.List;

public class VerificationPlanSelector {
    private static final String DEFAULT_SELECTION_REASON = "Selected configured server-side verification plan.";

    public VerificationPlanSelection selectPlan(List<VerificationPlan> plans, DetectedProject project) {
        return plans.stream()
                .filter(plan -> plan.technology() == project.technology())
                .findFirst()
                .map(plan -> VerificationPlanSelection.selected(
                        plan.id(),
                        reasonFor(plan)))
                .orElseGet(() -> VerificationPlanSelection.skipped(
                        "No enabled server-side verification plan matched " + project.technology() + "."));
    }

    private static String reasonFor(VerificationPlan plan) {
        if (plan.selectionReason() == null || plan.selectionReason().isBlank()) {
            return DEFAULT_SELECTION_REASON;
        }
        return plan.selectionReason();
    }
}
