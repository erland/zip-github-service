package info.isaksson.erland.zipbuildserver.application.verification;

import info.isaksson.erland.zipbuildserver.domain.model.verification.VerificationCommand;
import info.isaksson.erland.zipbuildserver.domain.model.verification.VerificationPlan;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class VerificationPlanValidator {
    public void validate(VerificationPlan plan) {
        List<String> missing = new ArrayList<>();
        if (plan.id() == null) {
            missing.add("id");
        }
        if (plan.name() == null) {
            missing.add("name");
        }
        if (plan.technology() == null) {
            missing.add("technology");
        }
        if (!missing.isEmpty()) {
            throw new VerificationPlanParseException("Verification plan is missing required fields: " + missing);
        }
        if (plan.commands().isEmpty()) {
            throw new VerificationPlanParseException("Verification plan '" + plan.id() + "' must define at least one command.");
        }
        plan.commands().forEach(this::validateCommand);
    }

    private void validateCommand(VerificationCommand command) {
        if (command.label() == null || command.label().isBlank()) {
            throw new VerificationPlanParseException("Verification command is missing a label.");
        }
        if (command.commandDisplay() == null || command.commandDisplay().isBlank()) {
            throw new VerificationPlanParseException("Verification command '" + command.label() + "' is missing commandDisplay.");
        }
    }
}
