package info.isaksson.erland.zipbuildserver.application.verification;

import info.isaksson.erland.zipbuildserver.domain.model.NetworkMode;
import info.isaksson.erland.zipbuildserver.domain.model.project.DetectedProject;
import info.isaksson.erland.zipbuildserver.domain.model.project.ProjectTechnology;
import info.isaksson.erland.zipbuildserver.domain.model.verification.VerificationCommand;
import info.isaksson.erland.zipbuildserver.domain.model.verification.VerificationPlan;
import info.isaksson.erland.zipbuildserver.domain.model.verification.VerificationPlanSelection;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

class VerificationPlanSelectorTest {
    private final VerificationPlanSelector selector = new VerificationPlanSelector();

    @Test
    void selectsFirstPlanMatchingDetectedProjectTechnology() {
        VerificationPlan node = plan("node-default", ProjectTechnology.NODE, "Use Node plan.");
        VerificationPlan maven = plan("maven-default", ProjectTechnology.MAVEN, "Use Maven plan.");

        VerificationPlanSelection selection = selector.selectPlan(
                List.of(node, maven),
                detectedProject(ProjectTechnology.MAVEN));

        assertTrue(selection.selected());
        assertEquals("maven-default", selection.selectedPlanId());
        assertEquals("Use Maven plan.", selection.reason());
    }

    @Test
    void usesDefaultReasonWhenMatchingPlanHasBlankReason() {
        VerificationPlan plan = plan("node-default", ProjectTechnology.NODE, " ");

        VerificationPlanSelection selection = selector.selectPlan(
                List.of(plan),
                detectedProject(ProjectTechnology.NODE));

        assertTrue(selection.selected());
        assertEquals("node-default", selection.selectedPlanId());
        assertEquals("Selected configured server-side verification plan.", selection.reason());
    }

    @Test
    void skipsWhenNoPlanMatchesDetectedProjectTechnology() {
        VerificationPlan plan = plan("node-default", ProjectTechnology.NODE, "Use Node plan.");

        VerificationPlanSelection selection = selector.selectPlan(
                List.of(plan),
                detectedProject(ProjectTechnology.MAVEN));

        assertFalse(selection.selected());
        assertNull(selection.selectedPlanId());
        assertEquals("No enabled server-side verification plan matched MAVEN.", selection.reason());
    }

    private static DetectedProject detectedProject(ProjectTechnology technology) {
        return new DetectedProject(
                ".",
                technology,
                List.of(),
                null,
                "Detected project.");
    }

    private static VerificationPlan plan(String id, ProjectTechnology technology, String selectionReason) {
        return new VerificationPlan(
                id,
                id,
                technology,
                List.of(),
                List.of(new VerificationCommand("Test", "${project.path}", "test", 600, false)),
                NetworkMode.DEPENDENCY,
                true,
                selectionReason);
    }
}
