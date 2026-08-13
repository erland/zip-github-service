package info.isaksson.erland.zipgithub.staging;

import info.isaksson.erland.zipgithub.api.dto.ImportResponse;
import info.isaksson.erland.zipgithub.api.dto.SourceUploadResponse;
import info.isaksson.erland.zipgithub.application.ProjectApplicationService;
import info.isaksson.erland.zipgithub.application.StoredUploadImportResult;
import info.isaksson.erland.zipgithub.domain.model.ImportSource;
import info.isaksson.erland.zipgithub.domain.model.StagingImport;
import info.isaksson.erland.zipgithub.persistence.StagingImportPersistenceStore;
import info.isaksson.erland.zipgithub.upload.StoredUploadArtifact;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StagingPromotionServiceTest {
    @Test
    void promotesClaimedUploadWithoutCopyUnderPersistenceLock() {
        UUID owner=UUID.randomUUID(), project=UUID.randomUUID(), stagingId=UUID.randomUUID(), importId=UUID.randomUUID();
        Instant now=Instant.parse("2026-08-08T05:00:00Z");
        StoredUploadArtifact artifact=new StoredUploadArtifact(UUID.randomUUID(),"project.zip",10,"a".repeat(64),Path.of("/tmp/project.zip"),now,now.plusSeconds(3600),Map.of());
        StagingImport item=new StagingImport(stagingId,artifact,"b".repeat(64),now.minusSeconds(10),now.plusSeconds(3600)); item.claim(owner,now.minusSeconds(5));
        StagingImportPersistenceStore store=mock(StagingImportPersistenceStore.class); ProjectApplicationService projects=mock(ProjectApplicationService.class);
        when(projects.findImportBySourceReference(owner, ImportSource.STAGING_IMPORT,"staging-import:"+stagingId)).thenReturn(Optional.empty());
        ImportResponse imported=new ImportResponse(importId,project,"main","UPLOADING",now);
        when(projects.createImportFromStoredUpload(eq(owner),eq(project),any(),eq("User"),eq("u@example.com"),same(artifact),eq("staging-import:"+stagingId),any()))
                .thenReturn(new StoredUploadImportResult(imported,new SourceUploadResponse(artifact.id(),importId,"project.zip",10,"a".repeat(64),"STORED",now,now.plusSeconds(3600))));
        when(store.promoteWithLock(eq(stagingId),eq(owner),eq(now),any())).thenAnswer(inv -> {
            StagingImportPersistenceStore.PromotionAction action=inv.getArgument(3);
            UUID created=action.createOrRecover(item);
            return new StagingImportPersistenceStore.PromotionOutcome(StagingImportPersistenceStore.PromotionResult.PROMOTED,created);
        });
        var result=new StagingPromotionService(store,projects,Clock.fixed(now, ZoneOffset.UTC)).promote(stagingId,owner,project,"User","u@example.com", false);
        assertEquals(importId,result.importId());
        verify(store).promoteWithLock(eq(stagingId),eq(owner),eq(now),any());
    }

    @Test
    void recoversPersistedImportByStableSourceReferenceInsideLock() {
        UUID owner=UUID.randomUUID(), project=UUID.randomUUID(), stagingId=UUID.randomUUID(), importId=UUID.randomUUID();
        Instant now=Instant.parse("2026-08-08T05:00:00Z");
        StoredUploadArtifact artifact=new StoredUploadArtifact(UUID.randomUUID(),"project.zip",10,"a".repeat(64),Path.of("/tmp/project.zip"),now,now.plusSeconds(3600),Map.of());
        StagingImport item=new StagingImport(stagingId,artifact,"b".repeat(64),now.minusSeconds(10),now.plusSeconds(3600)); item.claim(owner,now.minusSeconds(5));
        StagingImportPersistenceStore store=mock(StagingImportPersistenceStore.class); ProjectApplicationService projects=mock(ProjectApplicationService.class);
        ImportResponse imported=new ImportResponse(importId,project,"main","CREATED",now);
        when(projects.findImportBySourceReference(owner,ImportSource.STAGING_IMPORT,"staging-import:"+stagingId)).thenReturn(Optional.of(imported));
        when(projects.ensureStoredUploadAttached(owner,importId,artifact)).thenReturn(new StoredUploadImportResult(imported,null));
        when(store.promoteWithLock(eq(stagingId),eq(owner),eq(now),any())).thenAnswer(inv -> {
            StagingImportPersistenceStore.PromotionAction action=inv.getArgument(3);
            UUID recovered=action.createOrRecover(item);
            return new StagingImportPersistenceStore.PromotionOutcome(StagingImportPersistenceStore.PromotionResult.PROMOTED,recovered);
        });
        var result=new StagingPromotionService(store,projects,Clock.fixed(now,ZoneOffset.UTC)).promote(stagingId,owner,project,"User","u@example.com", false);
        assertEquals(importId,result.importId());
        verify(projects,never()).createImportFromStoredUpload(any(),any(),any(),any(),any(),any(),any(),any());
    }
}
