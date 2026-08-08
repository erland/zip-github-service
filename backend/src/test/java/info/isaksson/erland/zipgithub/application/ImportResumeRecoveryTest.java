package info.isaksson.erland.zipgithub.application;

import info.isaksson.erland.zipgithub.api.dto.ImportResponse;
import info.isaksson.erland.zipgithub.api.error.ApiException;
import info.isaksson.erland.zipgithub.delivery.GitCommitIdentity;
import info.isaksson.erland.zipgithub.delivery.GitDeliveryResult;
import info.isaksson.erland.zipgithub.domain.model.ImportAuditMetadata;
import info.isaksson.erland.zipgithub.persistence.ImportResumePersistenceStore;
import info.isaksson.erland.zipgithub.plan.ImmutableImportPlan;
import info.isaksson.erland.zipgithub.plan.ImmutableImportPlanEntry;
import info.isaksson.erland.zipgithub.plan.ImportPlanApproval;
import info.isaksson.erland.zipgithub.selection.ApprovedSelection;
import info.isaksson.erland.zipgithub.snapshot.RepositorySnapshot;
import info.isaksson.erland.zipgithub.upload.StoredUpload;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/** Step 7.21 restart regression: JVM state may disappear, durable owner-bound state must be enough to resume safely. */
class ImportResumeRecoveryTest {
    private static final UUID OWNER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PROJECT = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID IMPORT = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID PLAN = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final String BASE = "1".repeat(40);
    private static final String PLAN_DIGEST = "a".repeat(64);
    private static final String SELECTION_DIGEST = "b".repeat(64);
    private static final Instant NOW = Instant.parse("2026-08-07T17:00:00Z");

    @Test
    void resumesReviewSelectionApprovalAndCompletedDeliveryAfterInMemoryRestart() {
        FakeResumeStore store = new FakeResumeStore();
        ProjectApplicationService service = service(store);

        // Restart while review is open: upload + locked plan/base must rehydrate without a new upload/plan.
        store.put(state("READY_FOR_REVIEW", upload(), plan(), null, null, null));
        service.clearInMemoryStateForTests();
        assertEquals(PLAN_DIGEST, service.getImportPlan(OWNER, IMPORT).planDigestSha256());
        assertEquals(BASE, service.getImportPlan(OWNER, IMPORT).baseCommitSha());
        assertTrue(service.findImportSelection(OWNER, IMPORT).isEmpty());

        // Restart after immutable selection: the same selection must be read, not recreated.
        store.put(state("READY_FOR_REVIEW", upload(), plan(), selection(), null, null));
        service.clearInMemoryStateForTests();
        assertEquals(SELECTION_DIGEST, service.getImportSelection(OWNER, IMPORT).selectionDigestSha256());
        assertTrue(service.findImportPlanApproval(OWNER, IMPORT).isEmpty());

        // Restart after approval: review recovery must reuse both immutable records.
        store.put(state("READY_FOR_REVIEW", upload(), plan(), selection(), approval(), null));
        service.clearInMemoryStateForTests();
        assertEquals(SELECTION_DIGEST, service.findImportPlanApproval(OWNER, IMPORT).orElseThrow().selectionDigestSha256());
        assertEquals("Resume-safe message", service.findImportPlanApproval(OWNER, IMPORT).orElseThrow().commitMessage());
        assertEquals(SELECTION_DIGEST, service.getImportSelection(OWNER, IMPORT).selectionDigestSha256());

        // Restart after delivery: existing delivery is recoverable, so retry UI can avoid a duplicate commit.
        GitDeliveryResult delivered = delivery();
        store.put(state("PUSHED", upload(), plan(), selection(), approval(), delivered));
        service.clearInMemoryStateForTests();
        assertEquals(delivered.commitSha(), service.findGitDelivery(OWNER, IMPORT).orElseThrow().commitSha());
        assertEquals("PUSHED", service.getImport(OWNER, IMPORT).status());
    }

    @Test
    void anotherOwnerCannotHydrateOrResumeTheImport() {
        FakeResumeStore store = new FakeResumeStore();
        store.put(state("READY_FOR_REVIEW", upload(), plan(), selection(), approval(), null));
        ProjectApplicationService service = service(store);
        service.clearInMemoryStateForTests();

        ApiException error = assertThrows(ApiException.class, () -> service.getImport(OTHER, IMPORT));
        assertEquals(404, error.status());
        assertThrows(ApiException.class, () -> service.findImportPlan(OTHER, IMPORT));
        assertThrows(ApiException.class, () -> service.findImportSelection(OTHER, IMPORT));
    }

    private static ProjectApplicationService service(FakeResumeStore store) {
        ProjectApplicationService service = new ProjectApplicationService();
        service.persistentImports = store;
        return service;
    }

    private static ImportResumePersistenceStore.ResumeState state(String status, StoredUpload upload,
            ImmutableImportPlan plan, ApprovedSelection selection, ImportPlanApproval approval,
            GitDeliveryResult delivery) {
        return new ImportResumePersistenceStore.ResumeState(OWNER,
                new ImportResponse(IMPORT, PROJECT, "main", status, NOW), upload, snapshot(), plan, selection, approval,
                new GitCommitIdentity("Erland", "1+erland@users.noreply.github.com", "Erland", "1+erland@users.noreply.github.com"),
                ImportAuditMetadata.webUpload(), delivery);
    }

    private static StoredUpload upload() {
        return new StoredUpload(UUID.fromString("66666666-6666-6666-6666-666666666666"), IMPORT, OWNER,
                "project.zip", 123, "c".repeat(64), Path.of("/tmp/project.zip"), NOW, NOW.plusSeconds(3600));
    }

    private static RepositorySnapshot snapshot() {
        return new RepositorySnapshot(IMPORT, "owner/repo", "main", BASE, List.of(), NOW);
    }

    private static ImmutableImportPlan plan() {
        return new ImmutableImportPlan(PLAN, IMPORT, OWNER, "c".repeat(64), BASE, "mvp-3", PLAN_DIGEST,
                "READY", true, List.of(new ImmutableImportPlanEntry("src/App.java", "MODIFIED", "MODIFIED",
                "NONE", "NONE", null, null, 1L, "d".repeat(64), 1L, "e".repeat(64), true)), NOW);
    }

    private static ApprovedSelection selection() {
        return new ApprovedSelection(UUID.fromString("77777777-7777-7777-7777-777777777777"), IMPORT, PLAN, OWNER,
                PLAN_DIGEST, BASE, "selection-1", SELECTION_DIGEST, List.of("src/App.java"), List.of(), List.of(), NOW);
    }

    private static ImportPlanApproval approval() {
        return new ImportPlanApproval(IMPORT, PLAN, OWNER, PLAN_DIGEST, SELECTION_DIGEST, "Resume-safe message", NOW.plusSeconds(10));
    }

    private static GitDeliveryResult delivery() {
        return new GitDeliveryResult(IMPORT, "owner/repo", "main", "zip-github/work-abc", BASE,
                "2".repeat(40), PLAN_DIGEST, NOW.plusSeconds(20));
    }

    private static final class FakeResumeStore extends ImportResumePersistenceStore {
        private final Map<UUID, ResumeState> states = new ConcurrentHashMap<>();
        void put(ResumeState state) { states.put(state.response().id(), state); }
        @Override public boolean enabled() { return true; }
        @Override public Optional<ResumeState> find(UUID owner, UUID importId) {
            ResumeState state = states.get(importId);
            return state != null && state.ownerUserId().equals(owner) ? Optional.of(state) : Optional.empty();
        }
    }
}
