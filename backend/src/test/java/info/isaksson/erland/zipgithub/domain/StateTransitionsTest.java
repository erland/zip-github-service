package info.isaksson.erland.zipgithub.domain;

import info.isaksson.erland.zipgithub.domain.model.*;
import info.isaksson.erland.zipgithub.domain.status.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class StateTransitionsTest {
    private static final Instant NOW = Instant.parse("2026-08-06T12:00:00Z");
    private static final String SHA40 = "0123456789012345678901234567890123456789";

    @Test
    void importSessionAllowsForwardTransitionAndIdempotentRepeat() {
        ImportSession session = new ImportSession(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "main", NOW);
        assertTrue(session.transitionTo(ImportSessionStatus.UPLOADING, NOW.plusSeconds(1)));
        assertFalse(session.transitionTo(ImportSessionStatus.UPLOADING, NOW.plusSeconds(2)));
        assertEquals(ImportSessionStatus.UPLOADING, session.status());
    }

    @Test
    void importSessionRejectsSkippedTransition() {
        ImportSession session = new ImportSession(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "main", NOW);
        assertThrows(DomainTransitionException.class,
                () -> session.transitionTo(ImportSessionStatus.APPROVED, NOW.plusSeconds(1)));
    }

    @Test
    void lockedBaseCommitIsImmutable() {
        ImportSession session = new ImportSession(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "main", NOW);
        session.lockBaseCommit(SHA40, NOW);
        session.lockBaseCommit(SHA40.toUpperCase(), NOW.plusSeconds(1));
        assertThrows(IllegalStateException.class,
                () -> session.lockBaseCommit("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", NOW.plusSeconds(2)));
    }

    @Test
    void sourceUploadRequiresMetadataBeforeStored() {
        SourceUpload upload = new SourceUpload(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "project.zip",
                NOW, NOW.plus(1, ChronoUnit.DAYS));
        upload.transitionTo(SourceUploadStatus.UPLOADING);
        assertThrows(IllegalStateException.class, () -> upload.transitionTo(SourceUploadStatus.STORED));
        upload.recordStoredContent(42, "a".repeat(64));
        assertTrue(upload.transitionTo(SourceUploadStatus.STORED));
        assertFalse(upload.transitionTo(SourceUploadStatus.STORED));
    }

    @Test
    void blockedImportPlanCannotBeApproved() {
        ImportPlanEntry blocked = new ImportPlanEntry(UUID.randomUUID(), ".github/workflows/build.yml",
                ImportPlanEntry.ChangeType.BLOCKED, null, null, 10, true,
                ImportPlanEntry.PolicyResult.BLOCKED, "protected path");
        ImportPlan plan = new ImportPlan(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), SHA40,
                "mvp-v1", List.of(blocked), NOW);
        plan.transitionTo(ImportPlanStatus.READY);
        assertThrows(IllegalStateException.class, () -> plan.transitionTo(ImportPlanStatus.APPROVED));
    }

    @Test
    void deliverySupportsRetryAndRejectsUnrelatedJump() {
        GitHubDelivery delivery = new GitHubDelivery(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "import-123", NOW);
        delivery.transitionTo(GitHubDeliveryStatus.PREPARING);
        delivery.transitionTo(GitHubDeliveryStatus.FAILED);
        assertTrue(delivery.transitionTo(GitHubDeliveryStatus.PREPARING));
        assertThrows(DomainTransitionException.class,
                () -> delivery.transitionTo(GitHubDeliveryStatus.PULL_REQUEST_CREATED));
    }

    @Test
    void ownershipIsExplicitOnUserOwnedAggregates() {
        UUID owner = UUID.randomUUID();
        UUID stranger = UUID.randomUUID();
        Project project = new Project(UUID.randomUUID(), owner, "Book", null, null, null, null,
                "main", true, NOW, NOW);
        ImportSession session = new ImportSession(UUID.randomUUID(), project.id(), owner, "main", NOW);
        assertTrue(project.isOwnedBy(owner));
        assertTrue(session.isOwnedBy(owner));
        assertFalse(project.isOwnedBy(stranger));
        assertFalse(session.isOwnedBy(stranger));
    }
}
