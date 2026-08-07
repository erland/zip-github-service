package info.isaksson.erland.zipgithub.application;

import info.isaksson.erland.zipgithub.api.dto.ImportResponse;
import info.isaksson.erland.zipgithub.api.error.ApiException;
import info.isaksson.erland.zipgithub.delivery.GitCommitIdentity;
import info.isaksson.erland.zipgithub.delivery.GitDeliveryResult;
import info.isaksson.erland.zipgithub.domain.model.ImportAuditMetadata;
import info.isaksson.erland.zipgithub.persistence.ImportResumePersistenceStore;
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

/** Step 7.22: an active import may be cancelled before Git delivery and safely retried. */
class ImportCancellationTest {
    private static final UUID OWNER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PROJECT = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID IMPORT = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final Instant NOW = Instant.parse("2026-08-07T18:00:00Z");

    @Test
    void cancelsPersistentlyAndIdempotentlyBeforeDelivery() {
        FakeResumeStore store = new FakeResumeStore();
        store.put(state("APPROVED", null));
        ProjectApplicationService service = service(store);

        ImportResponse first = service.cancelImport(OWNER, IMPORT);
        ImportResponse retry = service.cancelImport(OWNER, IMPORT);

        assertEquals("CANCELLED", first.status());
        assertEquals("CANCELLED", retry.status());
        assertEquals("CANCELLED", store.find(OWNER, IMPORT).orElseThrow().response().status());
        assertEquals(1, service.expiredUploads(NOW).size(), "a cancelled import makes its expired ZIP cleanup-eligible");
    }


    @Test
    void cancelsBeforeApprovalAndRemainsCancelledAfterInMemoryRestart() {
        FakeResumeStore store = new FakeResumeStore();
        store.put(state("READY_FOR_REVIEW", null));
        ProjectApplicationService service = service(store);

        ImportResponse cancelled = service.cancelImport(OWNER, IMPORT);
        assertEquals("CANCELLED", cancelled.status());
        assertTrue(service.findGitDelivery(OWNER, IMPORT).isEmpty(), "cancel must not create Git delivery");
        assertEquals(ImportAuditMetadata.webUpload(), store.find(OWNER, IMPORT).orElseThrow().auditMetadata(),
                "audit metadata must survive cancellation");

        service.clearInMemoryStateForTests();
        assertEquals("CANCELLED", service.getImport(OWNER, IMPORT).status(),
                "cancelled state must survive restart/lazy hydration");
        assertTrue(service.findGitDelivery(OWNER, IMPORT).isEmpty());
    }

    @Test
    void refusesCancelAfterGitDeliveryAndKeepsOwnerIsolation() {
        FakeResumeStore deliveredStore = new FakeResumeStore();
        deliveredStore.put(state("PUSHED", delivery()));
        ProjectApplicationService deliveredService = service(deliveredStore);

        ApiException delivered = assertThrows(ApiException.class, () -> deliveredService.cancelImport(OWNER, IMPORT));
        assertEquals(409, delivered.status());
        assertEquals("IMPORT_ALREADY_DELIVERED", delivered.code());

        FakeResumeStore activeStore = new FakeResumeStore();
        activeStore.put(state("READY_FOR_REVIEW", null));
        ProjectApplicationService activeService = service(activeStore);
        ApiException otherOwner = assertThrows(ApiException.class, () -> activeService.cancelImport(OTHER, IMPORT));
        assertEquals(404, otherOwner.status());
    }

    private static ProjectApplicationService service(FakeResumeStore store) {
        ProjectApplicationService service = new ProjectApplicationService();
        service.persistentImports = store;
        return service;
    }

    private static ImportResumePersistenceStore.ResumeState state(String status, GitDeliveryResult delivery) {
        StoredUpload upload = new StoredUpload(UUID.fromString("66666666-6666-6666-6666-666666666666"), IMPORT, OWNER,
                "project.zip", 123, "c".repeat(64), Path.of("/tmp/project.zip"), NOW.minusSeconds(7200), NOW.minusSeconds(3600));
        return new ImportResumePersistenceStore.ResumeState(OWNER,
                new ImportResponse(IMPORT, PROJECT, "main", status, NOW.minusSeconds(7200)), upload, null, null, null, null,
                new GitCommitIdentity("Erland", "1+erland@users.noreply.github.com", "Erland", "1+erland@users.noreply.github.com"),
                ImportAuditMetadata.webUpload(), delivery);
    }

    private static GitDeliveryResult delivery() {
        return new GitDeliveryResult(IMPORT, "owner/repo", "main", "zip-github/work-abc", "1".repeat(40),
                "2".repeat(40), "a".repeat(64), NOW.minusSeconds(60));
    }

    private static final class FakeResumeStore extends ImportResumePersistenceStore {
        private final Map<UUID, ResumeState> states = new ConcurrentHashMap<>();
        void put(ResumeState state) { states.put(state.response().id(), state); }
        @Override public boolean enabled() { return true; }
        @Override public Optional<ResumeState> find(UUID owner, UUID importId) {
            ResumeState state = states.get(importId);
            return state != null && state.ownerUserId().equals(owner) ? Optional.of(state) : Optional.empty();
        }
        @Override public List<ResumeState> list(UUID owner, UUID projectId) {
            return states.values().stream().filter(s -> s.ownerUserId().equals(owner) && s.response().projectId().equals(projectId)).toList();
        }
        @Override public void updateStatus(UUID owner, UUID importId, String status, String baseCommitSha) {
            ResumeState current = find(owner, importId).orElseThrow();
            ImportResponse r = current.response();
            put(new ResumeState(owner, new ImportResponse(r.id(), r.projectId(), r.baseBranch(), status, r.createdAt()),
                    current.upload(), current.snapshot(), current.plan(), current.selection(), current.approval(),
                    current.identity(), current.auditMetadata(), current.delivery()));
        }
        @Override public List<StoredUpload> listExpiredTerminalUploads(Instant now) {
            return states.values().stream()
                    .filter(s -> List.of("PUSHED", "PULL_REQUEST_CREATED", "CANCELLED").contains(s.response().status()))
                    .map(ResumeState::upload).filter(java.util.Objects::nonNull)
                    .filter(upload -> !upload.retentionDeadline().isAfter(now)).toList();
        }
    }
}
