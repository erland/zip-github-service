package info.isaksson.erland.zipbuildserver.domain.model.verification;

public record VerificationPlanSelection(
        String selectedPlanId,
        String reason,
        boolean selected) {
    public static VerificationPlanSelection selected(String planId, String reason) {
        return new VerificationPlanSelection(planId, reason, true);
    }

    public static VerificationPlanSelection skipped(String reason) {
        return new VerificationPlanSelection(null, reason, false);
    }
}
