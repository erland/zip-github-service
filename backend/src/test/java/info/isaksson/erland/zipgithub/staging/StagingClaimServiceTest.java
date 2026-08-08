package info.isaksson.erland.zipgithub.staging;

import info.isaksson.erland.zipgithub.api.error.ApiException;
import info.isaksson.erland.zipgithub.domain.model.StagingImport;
import info.isaksson.erland.zipgithub.persistence.StagingImportPersistenceStore;
import info.isaksson.erland.zipgithub.upload.StoredUploadArtifact;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StagingClaimServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-08T06:00:00Z");

    @Test
    void hashesBearerTokenAndReturnsOwnerSafeMetadata() {
        var store = mock(StagingImportPersistenceStore.class);
        var service = new StagingClaimService(Clock.fixed(NOW, ZoneOffset.UTC));
        service.store = store;
        UUID owner = UUID.randomUUID();
        StagingImport staging = staging(owner);
        String rawToken = "A".repeat(43);
        String expectedHash = StagingSecretCodec.digestHex(rawToken);
        when(store.claimByTokenHash(expectedHash, owner, NOW, NOW.plusSeconds(14400))).thenReturn(
                new StagingImportPersistenceStore.ClaimOutcome(StagingImportPersistenceStore.ClaimResult.CLAIMED, staging));

        var claimed = service.claim(rawToken, owner);
        assertEquals(staging.id(), claimed.stagingId());
        assertEquals("project.zip", claimed.originalFilename());
        verify(store).claimByTokenHash(expectedHash, owner, NOW, NOW.plusSeconds(14400));
    }

    @Test
    void unavailableTokensHaveOneNeutralGoneResponse() {
        var store = mock(StagingImportPersistenceStore.class);
        var service = new StagingClaimService(Clock.fixed(NOW, ZoneOffset.UTC));
        service.store = store;
        UUID owner = UUID.randomUUID();
        when(store.claimByTokenHash(anyString(), eq(owner), eq(NOW), eq(NOW.plusSeconds(14400)))).thenReturn(
                new StagingImportPersistenceStore.ClaimOutcome(StagingImportPersistenceStore.ClaimResult.NOT_AVAILABLE, null));

        ApiException e = assertThrows(ApiException.class, () -> service.claim("B".repeat(43), owner));
        assertEquals(410, e.status());
        assertEquals("STAGING_CLAIM_UNAVAILABLE", e.code());
    }

    @Test
    void takenOrReusedTokenIsIndistinguishableFromExpiredToken() {
        var store = mock(StagingImportPersistenceStore.class);
        var service = new StagingClaimService(Clock.fixed(NOW, ZoneOffset.UTC));
        service.store = store;
        UUID secondUser = UUID.randomUUID();
        when(store.claimByTokenHash(anyString(), eq(secondUser), eq(NOW), eq(NOW.plusSeconds(14400)))).thenReturn(
                new StagingImportPersistenceStore.ClaimOutcome(StagingImportPersistenceStore.ClaimResult.NOT_AVAILABLE, null));

        ApiException e = assertThrows(ApiException.class, () -> service.claim("C".repeat(43), secondUser));
        assertEquals(410, e.status());
        assertEquals("STAGING_CLAIM_UNAVAILABLE", e.code());
        assertFalse(e.getMessage() != null && e.getMessage().toLowerCase().contains("owner"));
    }

    private static StagingImport staging(UUID owner) {
        var artifact = new StoredUploadArtifact(UUID.randomUUID(), "project.zip", 42, "a".repeat(64),
                Path.of("/tmp/project.zip"), NOW.minusSeconds(30), NOW.plusSeconds(3600), Map.of());
        var value = new StagingImport(UUID.randomUUID(), artifact, "b".repeat(64), NOW.minusSeconds(30), NOW.plusSeconds(3600));
        value.claim(owner, NOW.minusSeconds(5));
        return value;
    }
}
