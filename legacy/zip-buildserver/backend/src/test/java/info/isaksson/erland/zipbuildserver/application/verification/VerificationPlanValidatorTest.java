package info.isaksson.erland.zipbuildserver.application.verification;

import info.isaksson.erland.zipbuildserver.domain.model.NetworkMode;
import info.isaksson.erland.zipbuildserver.domain.model.project.ProjectTechnology;
import info.isaksson.erland.zipbuildserver.domain.model.verification.VerificationCommand;
import info.isaksson.erland.zipbuildserver.domain.model.verification.VerificationPlan;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VerificationPlanValidatorTest {
    private final VerificationPlanValidator validator = new VerificationPlanValidator();

    @Test
    void acceptsValidPlan() {
        VerificationPlan plan = plan("valid-plan", "Valid Plan", ProjectTechnology.NODE,
                List.of(command("Test", "npm test")));

        assertDoesNotThrow(() -> validator.validate(plan));
    }

    @Test
    void rejectsMissingRequiredRootFields() {
        VerificationPlan plan = plan(null, null, null, List.of(command("Test", "npm test")));

        VerificationPlanParseException exception = assertThrows(
                VerificationPlanParseException.class,
                () -> validator.validate(plan));

        assertTrue(exception.getMessage().contains("id"));
        assertTrue(exception.getMessage().contains("name"));
        assertTrue(exception.getMessage().contains("technology"));
    }

    @Test
    void rejectsPlansWithoutCommands() {
        VerificationPlan plan = plan("no-commands", "No Commands", ProjectTechnology.NODE, List.of());

        VerificationPlanParseException exception = assertThrows(
                VerificationPlanParseException.class,
                () -> validator.validate(plan));

        assertEquals("Verification plan 'no-commands' must define at least one command.", exception.getMessage());
    }

    @Test
    void rejectsCommandsMissingLabels() {
        VerificationPlan plan = plan("missing-label", "Missing Label", ProjectTechnology.NODE,
                List.of(command(null, "npm test")));

        VerificationPlanParseException exception = assertThrows(
                VerificationPlanParseException.class,
                () -> validator.validate(plan));

        assertEquals("Verification command is missing a label.", exception.getMessage());
    }

    @Test
    void rejectsCommandsMissingCommandDisplay() {
        VerificationPlan plan = plan("missing-command-display", "Missing Command Display", ProjectTechnology.NODE,
                List.of(command("Test", null)));

        VerificationPlanParseException exception = assertThrows(
                VerificationPlanParseException.class,
                () -> validator.validate(plan));

        assertEquals("Verification command 'Test' is missing commandDisplay.", exception.getMessage());
    }

    private static VerificationPlan plan(
            String id,
            String name,
            ProjectTechnology technology,
            List<VerificationCommand> commands) {
        return new VerificationPlan(id, name, technology, List.of(), commands,
                NetworkMode.DEPENDENCY, true, null);
    }

    private static VerificationCommand command(String label, String commandDisplay) {
        return new VerificationCommand(label, "${project.path}", commandDisplay, 600, false);
    }
}
