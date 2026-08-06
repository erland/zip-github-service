package info.isaksson.erland.zipbuildserver.application.verification;

import info.isaksson.erland.zipbuildserver.domain.model.project.DetectedProject;
import info.isaksson.erland.zipbuildserver.domain.model.project.ProjectTechnology;
import info.isaksson.erland.zipbuildserver.domain.model.NetworkMode;
import info.isaksson.erland.zipbuildserver.domain.model.verification.VerificationCommand;
import info.isaksson.erland.zipbuildserver.domain.model.verification.VerificationPlan;
import info.isaksson.erland.zipbuildserver.domain.model.verification.VerificationPlanSelection;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VerificationPlanServiceTest {
    @Test
    void loadsDefaultPlansFromResources() {
        VerificationPlanService service = new VerificationPlanService();

        List<VerificationPlan> plans = service.listPlans();

        assertEquals(3, plans.size());
        assertTrue(plans.stream().anyMatch(plan -> plan.id().equals("node-default")));
        assertTrue(plans.stream().anyMatch(plan -> plan.id().equals("maven-default")));
        assertTrue(plans.stream().anyMatch(plan -> plan.id().equals("multi-project-default")));
    }

    @Test
    void parsesPlanCommands() {
        VerificationPlan plan = VerificationPlanService.parsePlan("""
                id: example-node
                name: Example Node
                technology: NODE
                enabled: true
                networkMode: DEPENDENCY
                selectionReason: Example selection.
                indicators:
                  - package.json
                commands:
                  - label: Install
                    workingDirectory: ${project.path}
                    commandDisplay: npm ci
                    timeoutSeconds: 120
                    optional: false
                """);

        assertEquals("example-node", plan.id());
        assertEquals(ProjectTechnology.NODE, plan.technology());
        assertEquals(NetworkMode.DEPENDENCY, plan.networkMode());
        assertEquals(List.of("package.json"), plan.indicators());
        assertEquals("npm ci", plan.commands().getFirst().commandDisplay());
        assertEquals(120, plan.commands().getFirst().timeoutSeconds());
        assertFalse(plan.commands().getFirst().optional());
    }

    @Test
    void parsesQuotedValuesAndStripsComments() {
        VerificationPlan plan = VerificationPlanService.parsePlan("""
                # Root comment
                id: 'quoted-plan' # inline comments are stripped
                name: "Quoted Plan"
                technology: MAVEN
                networkMode: FULL
                selectionReason: "Prefer quoted values."
                indicators:
                  - 'pom.xml'
                  - "mvnw" # wrapper indicator
                commands:
                  - label: "Test"
                    commandDisplay: "./mvnw test" # command comment
                """);

        assertEquals("quoted-plan", plan.id());
        assertEquals("Quoted Plan", plan.name());
        assertEquals(ProjectTechnology.MAVEN, plan.technology());
        assertEquals(NetworkMode.FULL, plan.networkMode());
        assertEquals("Prefer quoted values.", plan.selectionReason());
        assertEquals(List.of("pom.xml", "mvnw"), plan.indicators());
        assertEquals("Test", plan.commands().getFirst().label());
        assertEquals("./mvnw test", plan.commands().getFirst().commandDisplay());
    }

    @Test
    void appliesCommandDefaultsWhenOptionalFieldsAreMissing() {
        VerificationPlan plan = VerificationPlanService.parsePlan("""
                id: defaults
                name: Defaults
                technology: NODE
                commands:
                  - label: Test
                    commandDisplay: npm test
                """);

        VerificationCommand command = plan.commands().getFirst();

        assertEquals(NetworkMode.DEPENDENCY, plan.networkMode());
        assertTrue(plan.enabled());
        assertEquals("${project.path}", command.workingDirectory());
        assertEquals(600, command.timeoutSeconds());
        assertFalse(command.optional());
    }

    @Test
    void parsesMultipleCommandsInOrder() {
        VerificationPlan plan = VerificationPlanService.parsePlan("""
                id: multi-command
                name: Multi Command
                technology: MULTI_PROJECT
                commands:
                  - label: Backend tests
                    workingDirectory: backend
                    commandDisplay: mvn test
                    timeoutSeconds: 900
                    optional: false
                  - label: Frontend tests
                    workingDirectory: frontend
                    commandDisplay: npm test
                    timeoutSeconds: 300
                    optional: true
                """);

        assertEquals(2, plan.commands().size());

        VerificationCommand first = plan.commands().get(0);
        assertEquals("Backend tests", first.label());
        assertEquals("backend", first.workingDirectory());
        assertEquals("mvn test", first.commandDisplay());
        assertEquals(900, first.timeoutSeconds());
        assertFalse(first.optional());

        VerificationCommand second = plan.commands().get(1);
        assertEquals("Frontend tests", second.label());
        assertEquals("frontend", second.workingDirectory());
        assertEquals("npm test", second.commandDisplay());
        assertEquals(300, second.timeoutSeconds());
        assertTrue(second.optional());
    }

    @Test
    void filtersDisabledPlansAndSortsEnabledPlansById() {
        VerificationPlan beta = VerificationPlanService.parsePlan("""
                id: beta
                name: Beta
                technology: NODE
                commands:
                  - label: Test
                    commandDisplay: npm test
                """);
        VerificationPlan alpha = VerificationPlanService.parsePlan("""
                id: alpha
                name: Alpha
                technology: MAVEN
                commands:
                  - label: Test
                    commandDisplay: mvn test
                """);
        VerificationPlan disabled = VerificationPlanService.parsePlan("""
                id: disabled
                name: Disabled
                technology: NODE
                enabled: false
                commands:
                  - label: Test
                    commandDisplay: npm test
                """);

        VerificationPlanService service = new VerificationPlanService(List.of(beta, disabled, alpha));

        assertEquals(List.of("alpha", "beta"), service.listPlans().stream()
                .map(VerificationPlan::id)
                .toList());
    }

    @Test
    void rejectsPlansMissingRequiredRootFields() {
        VerificationPlanParseException exception = assertThrows(
                VerificationPlanParseException.class,
                () -> VerificationPlanService.parsePlan("""
                        name: Missing Id And Technology
                        commands:
                          - label: Test
                            commandDisplay: npm test
                        """));

        assertTrue(exception.getMessage().contains("id"));
        assertTrue(exception.getMessage().contains("technology"));
    }

    @Test
    void rejectsPlansWithoutCommands() {
        VerificationPlanParseException exception = assertThrows(
                VerificationPlanParseException.class,
                () -> VerificationPlanService.parsePlan("""
                        id: no-commands
                        name: No Commands
                        technology: NODE
                        """));

        assertEquals("Verification plan 'no-commands' must define at least one command.", exception.getMessage());
    }

    @Test
    void rejectsCommandsMissingLabels() {
        VerificationPlanParseException exception = assertThrows(
                VerificationPlanParseException.class,
                () -> VerificationPlanService.parsePlan("""
                        id: missing-label
                        name: Missing Label
                        technology: NODE
                        commands:
                          - label:
                            commandDisplay: npm test
                        """));

        assertEquals("Verification command is missing a label.", exception.getMessage());
    }

    @Test
    void rejectsCommandsMissingCommandDisplay() {
        VerificationPlanParseException exception = assertThrows(
                VerificationPlanParseException.class,
                () -> VerificationPlanService.parsePlan("""
                        id: missing-command-display
                        name: Missing Command Display
                        technology: NODE
                        commands:
                          - label: Test
                        """));

        assertEquals("Verification command 'Test' is missing commandDisplay.", exception.getMessage());
    }


    @Test
    void preservesHashCharactersInsideQuotedValues() {
        VerificationPlan plan = VerificationPlanService.parsePlan("""
                id: quoted-hash
                name: "Plan #1"
                technology: NODE
                selectionReason: "Use # tagged reason."
                indicators:
                  - "package#lock.json"
                commands:
                  - label: "Test #1"
                    commandDisplay: "npm test # not a comment"
                """);

        assertEquals("Plan #1", plan.name());
        assertEquals("Use # tagged reason.", plan.selectionReason());
        assertEquals(List.of("package#lock.json"), plan.indicators());
        assertEquals("Test #1", plan.commands().getFirst().label());
        assertEquals("npm test # not a comment", plan.commands().getFirst().commandDisplay());
    }

    @Test
    void rejectsUnsupportedRootKeysWithLineNumber() {
        VerificationPlanParseException exception = assertThrows(
                VerificationPlanParseException.class,
                () -> VerificationPlanService.parsePlan("""
                        id: unsupported-root
                        name: Unsupported Root
                        technology: NODE
                        unknown: value
                        commands:
                          - label: Test
                            commandDisplay: npm test
                        """));

        assertEquals("Unable to parse verification plan at line 4: unsupported root key.", exception.getMessage());
    }

    @Test
    void rejectsUnsupportedCommandKeysWithLineNumber() {
        VerificationPlanParseException exception = assertThrows(
                VerificationPlanParseException.class,
                () -> VerificationPlanService.parsePlan("""
                        id: unsupported-command
                        name: Unsupported Command
                        technology: NODE
                        commands:
                          - label: Test
                            commandDisplay: npm test
                            unexpected: value
                        """));

        assertEquals("Unable to parse verification plan at line 7: unsupported command key.", exception.getMessage());
    }

    @Test
    void rejectsInvalidBooleanValuesWithLineNumber() {
        VerificationPlanParseException exception = assertThrows(
                VerificationPlanParseException.class,
                () -> VerificationPlanService.parsePlan("""
                        id: invalid-boolean
                        name: Invalid Boolean
                        technology: NODE
                        enabled: yes
                        commands:
                          - label: Test
                            commandDisplay: npm test
                        """));

        assertEquals("Unable to parse verification plan at line 4: invalid boolean for enabled: 'yes'.", exception.getMessage());
    }

    @Test
    void selectsPlanByDetectedProjectTechnology() {
        VerificationPlanService service = new VerificationPlanService();
        DetectedProject project = new DetectedProject(
                ".",
                ProjectTechnology.MAVEN,
                List.of("pom.xml"),
                null,
                "Detected Maven project indicator.");

        VerificationPlanSelection selection = service.selectPlan(project);

        assertTrue(selection.selected());
        assertEquals("maven-default", selection.selectedPlanId());
    }
}
