package info.isaksson.erland.zipgithub.domain;

import info.isaksson.erland.zipgithub.domain.model.StagingImport;
import info.isaksson.erland.zipgithub.domain.status.DomainTransitionException;
import info.isaksson.erland.zipgithub.domain.status.StagingImportStatus;
import info.isaksson.erland.zipgithub.upload.GitFileMode;
import info.isaksson.erland.zipgithub.upload.StoredUploadArtifact;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class StagingImportLifecycleTest {
    @Test void claimAndPromotionAreOwnerBoundAndIdempotent() {
        Instant now=Instant.parse("2026-08-08T04:00:00Z"); UUID owner=UUID.randomUUID(), importId=UUID.randomUUID();
        var a=new StoredUploadArtifact(UUID.randomUUID(),"p.zip",12,"a".repeat(64),Path.of("/tmp/p.zip"),now,now.plusSeconds(3600),Map.of("bin/run", GitFileMode.EXECUTABLE));
        var s=new StagingImport(UUID.randomUUID(),a,"b".repeat(64),now,now.plusSeconds(3600));
        assertTrue(s.claim(owner,now.plusSeconds(1),now.plusSeconds(300))); assertFalse(s.claim(owner,now.plusSeconds(2),now.plusSeconds(600)));
        assertEquals(now.plusSeconds(300), s.expiresAt(), "same-owner retry must not extend the claimed grace deadline");
        assertEquals(StagingImportStatus.CLAIMED,s.status()); assertThrows(IllegalStateException.class,()->s.promote(UUID.randomUUID(),importId,now.plusSeconds(3)));
        assertTrue(s.promote(owner,importId,now.plusSeconds(3))); assertFalse(s.promote(owner,importId,now.plusSeconds(4)));
        assertEquals(StagingImportStatus.PROMOTED,s.status());
    }
    @Test void expiredOrCancelledStateCannotBeClaimed() {
        Instant now=Instant.parse("2026-08-08T04:00:00Z"); var a=new StoredUploadArtifact(UUID.randomUUID(),"p.zip",1,"a".repeat(64),Path.of("/tmp/p"),now,now.plusSeconds(60));
        var expired=new StagingImport(UUID.randomUUID(),a,"b".repeat(64),now,now.plusSeconds(60)); expired.expire(now.plusSeconds(61));
        assertThrows(DomainTransitionException.class,()->expired.claim(UUID.randomUUID(),now.plusSeconds(62)));
        var cancelled=new StagingImport(UUID.randomUUID(),a,"c".repeat(64),now,now.plusSeconds(60)); cancelled.cancel(now.plusSeconds(1));
        assertThrows(DomainTransitionException.class,()->cancelled.claim(UUID.randomUUID(),now.plusSeconds(2)));
    }
    @Test void unixModeMappingNeverInfersFromFilename() {
        assertNull(GitFileMode.fromUnixMode(0));
        assertEquals(GitFileMode.REGULAR,GitFileMode.fromUnixMode(0100644));
        assertEquals(GitFileMode.EXECUTABLE,GitFileMode.fromUnixMode(0100755));
    }
}
