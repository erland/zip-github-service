package info.isaksson.erland.zipgithub.selection;

import info.isaksson.erland.zipgithub.api.error.ApiException;
import info.isaksson.erland.zipgithub.plan.ImmutableImportPlan;
import info.isaksson.erland.zipgithub.plan.ImmutableImportPlanEntry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ImportSelectionFactoryTest {
    private static final UUID OWNER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String PLAN_DIGEST = "a".repeat(64);
    private static final String BASE = "1".repeat(40);

    @Test
    void selectionDigestIsDeterministicAndPathsAreCanonical() {
        var plan = plan();
        var factory = new ImportSelectionFactory();
        var first = factory.create(OWNER, plan, PLAN_DIGEST, BASE,
                List.of("src/B.java", "src/A.java"), List.of(), decisions(), Instant.parse("2026-08-07T13:00:00Z"));
        var second = factory.create(OWNER, plan, PLAN_DIGEST, BASE,
                List.of("src/A.java", "src/B.java"), List.of(), decisions(), Instant.parse("2026-08-07T14:00:00Z"));

        assertEquals(List.of("src/A.java", "src/B.java"), first.selectedPaths());
        assertEquals(List.of(".git/config", ".github/workflows/ci.yml", "README.old"), first.excludedPaths());
        assertEquals(first.selectionDigestSha256(), second.selectionDigestSha256());
        assertNotEquals(first.id(), second.id());
        assertThrows(UnsupportedOperationException.class, () -> first.selectedPaths().add("x"));
    }

    @Test
    void hardBlockedPathCanNeverBeSelected() {
        ApiException error = assertThrows(ApiException.class, () -> new ImportSelectionFactory().create(
                OWNER, plan(), PLAN_DIGEST, BASE, List.of(".git/config"), List.of(), decisions(), Instant.now()));
        assertEquals("HARD_BLOCKED_PATH_SELECTED", error.code());
    }

    @Test
    void overridablePathRequiresExplicitAuditAcknowledgement() {
        var factory = new ImportSelectionFactory();
        ApiException missing = assertThrows(ApiException.class, () -> factory.create(
                OWNER, plan(), PLAN_DIGEST, BASE, List.of(".github/workflows/ci.yml"), List.of(), decisions(".github/workflows/ci.yml"), Instant.now()));
        assertEquals("OVERRIDE_REQUIRED", missing.code());

        var selection = factory.create(OWNER, plan(), PLAN_DIGEST, BASE,
                List.of(".github/workflows/ci.yml"),
                List.of(new ImportSelectionFactory.RequestedOverride(
                        ".github/workflows/ci.yml", "I understand that this changes repository automation.")), decisions(".github/workflows/ci.yml"), Instant.now());
        assertEquals(1, selection.overrides().size());
        assertEquals("OVERRIDABLE_BLOCKED", selection.overrides().getFirst().blockerType());
    }


    @Test
    void hardBlockCannotBeBypassedWithAnOverrideRecord() {
        var factory = new ImportSelectionFactory();
        ApiException error = assertThrows(ApiException.class, () -> factory.create(
                OWNER, plan(), PLAN_DIGEST, BASE,
                List.of(".git/config"),
                List.of(new ImportSelectionFactory.RequestedOverride(".git/config", "I accept the risk.")),
                decisions(), Instant.now()));
        assertEquals("HARD_BLOCKED_PATH_SELECTED", error.code());
    }

    @Test
    void overrideIsAuditedAndChangesSelectionDigest() {
        var factory = new ImportSelectionFactory();
        var first = factory.create(OWNER, plan(), PLAN_DIGEST, BASE,
                List.of("src/A.java", ".github/workflows/ci.yml"),
                List.of(new ImportSelectionFactory.RequestedOverride(
                        ".github/workflows/ci.yml", "Approved workflow change.")),
                decisions(".github/workflows/ci.yml"), Instant.parse("2026-08-07T15:00:00Z"));
        var second = factory.create(OWNER, plan(), PLAN_DIGEST, BASE,
                List.of("src/A.java", ".github/workflows/ci.yml"),
                List.of(new ImportSelectionFactory.RequestedOverride(
                        ".github/workflows/ci.yml", "Approved workflow change after review.")),
                decisions(".github/workflows/ci.yml"), Instant.parse("2026-08-07T15:01:00Z"));

        assertEquals(".github/workflows/ci.yml", first.overrides().getFirst().path());
        assertEquals("GITHUB_WORKFLOW_PROTECTED", first.overrides().getFirst().policyCode());
        assertEquals("Approved workflow change.", first.overrides().getFirst().acknowledgement());
        assertNotEquals(first.selectionDigestSha256(), second.selectionDigestSha256(),
                "audit acknowledgement is part of immutable selection identity");
    }

    @Test
    void deletionAlsoRequiresExplicitOverride() {
        var factory = new ImportSelectionFactory();
        assertCode("OVERRIDE_REQUIRED", () -> factory.create(
                OWNER, plan(), PLAN_DIGEST, BASE, List.of("README.old"), List.of(), decisions("README.old"), Instant.now()));

        var selection = factory.create(OWNER, plan(), PLAN_DIGEST, BASE, List.of("README.old"),
                List.of(new ImportSelectionFactory.RequestedOverride("README.old", "Approved deletion.")), decisions("README.old"), Instant.now());
        assertEquals(List.of("README.old"), selection.selectedPaths());
        assertEquals("DELETE_PROTECTED", selection.overrides().getFirst().policyCode());
    }

    @Test
    void requiresDecisionForEveryBlockingEntry() {
        var factory = new ImportSelectionFactory();
        ApiException error = assertThrows(ApiException.class, () -> factory.create(
                OWNER, plan(), PLAN_DIGEST, BASE, List.of("src/A.java"), List.of(), List.of(), Instant.now()));
        assertEquals("BLOCKER_DECISION_REQUIRED", error.code());
    }

    @Test
    void explicitExclusionsAndHardBlockAcknowledgementAreAudited() {
        var selection = new ImportSelectionFactory().create(OWNER, plan(), PLAN_DIGEST, BASE,
                List.of("src/A.java"), List.of(), decisions(), Instant.now());
        assertEquals(3, selection.blockerDecisions().size());
        assertTrue(selection.blockerDecisions().stream().anyMatch(item ->
                item.path().equals(".git/config") && item.decision().equals("ACKNOWLEDGE_EXCLUSION")));
        assertTrue(selection.blockerDecisions().stream().filter(item -> !item.path().equals(".git/config"))
                .allMatch(item -> item.decision().equals("EXCLUDE")));
    }

    @Test
    void rejectsEmptyStaleAndUnknownSelections() {
        var factory = new ImportSelectionFactory();
        assertCode("EMPTY_SELECTION", () -> factory.create(OWNER, plan(), PLAN_DIGEST, BASE, List.of(), List.of(), decisions(), Instant.now()));
        assertCode("IMPORT_PLAN_DIGEST_MISMATCH", () -> factory.create(OWNER, plan(), "b".repeat(64), BASE,
                List.of("src/A.java"), List.of(), decisions(), Instant.now()));
        assertCode("IMPORT_PLAN_BASE_MISMATCH", () -> factory.create(OWNER, plan(), PLAN_DIGEST, "2".repeat(40),
                List.of("src/A.java"), List.of(), decisions(), Instant.now()));
        assertCode("SELECTION_PATH_NOT_IN_PLAN", () -> factory.create(OWNER, plan(), PLAN_DIGEST, BASE,
                List.of("missing.txt"), List.of(), decisions(), Instant.now()));
    }

    private static List<ImportSelectionFactory.RequestedBlockerDecision> decisions(String... includedPaths) {
        var included = java.util.Set.of(includedPaths);
        return List.of(
                new ImportSelectionFactory.RequestedBlockerDecision(".git/config", "ACKNOWLEDGE_EXCLUSION"),
                new ImportSelectionFactory.RequestedBlockerDecision(".github/workflows/ci.yml",
                        included.contains(".github/workflows/ci.yml") ? "INCLUDE_OVERRIDE" : "EXCLUDE"),
                new ImportSelectionFactory.RequestedBlockerDecision("README.old",
                        included.contains("README.old") ? "INCLUDE_OVERRIDE" : "EXCLUDE")
        );
    }

    private static void assertCode(String code, org.junit.jupiter.api.function.Executable executable) {
        ApiException error = assertThrows(ApiException.class, executable);
        assertEquals(code, error.code());
    }

    private static ImmutableImportPlan plan() {
        UUID importId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        return new ImmutableImportPlan(UUID.fromString("44444444-4444-4444-4444-444444444444"), importId, OWNER,
                "f".repeat(64), BASE, "mvp-2", PLAN_DIGEST, "READY", true, List.of(
                entry("src/A.java", "MODIFIED", "NONE", null),
                entry("src/B.java", "ADDED", "NONE", null),
                entry(".git/config", "BLOCKED", "HARD_BLOCKED", "GIT_METADATA_PROTECTED"),
                entry(".github/workflows/ci.yml", "BLOCKED", "OVERRIDABLE_BLOCKED", "GITHUB_WORKFLOW_PROTECTED"),
                entry("README.old", "BLOCKED", "OVERRIDABLE_BLOCKED", "DELETE_PROTECTED")
        ), Instant.parse("2026-08-07T12:00:00Z"));
    }

    private static ImmutableImportPlanEntry entry(String path, String status, String blockerType, String policyCode) {
        return new ImmutableImportPlanEntry(path, status, status.equals("BLOCKED") ? "MODIFIED" : status,
                status.equals("BLOCKED") ? "BLOCKING" : "NONE", blockerType, policyCode, null,
                1L, "c".repeat(64), 1L, "d".repeat(64), true);
    }
}
