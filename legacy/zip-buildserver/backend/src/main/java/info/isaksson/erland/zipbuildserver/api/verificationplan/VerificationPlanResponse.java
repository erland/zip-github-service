package info.isaksson.erland.zipbuildserver.api.verificationplan;

import info.isaksson.erland.zipbuildserver.domain.model.verification.VerificationPlan;

import java.util.List;

public record VerificationPlanResponse(List<VerificationPlan> plans) {
}
