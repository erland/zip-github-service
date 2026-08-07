package info.isaksson.erland.zipgithub.application;

import info.isaksson.erland.zipgithub.api.dto.CreateImportRequest;
import info.isaksson.erland.zipgithub.api.dto.CreateProjectRequest;
import info.isaksson.erland.zipgithub.api.error.ApiException;
import info.isaksson.erland.zipgithub.archive.ArchiveInspectionService;
import info.isaksson.erland.zipgithub.archive.ArchiveInventory;
import info.isaksson.erland.zipgithub.archive.ArchiveInventoryService;
import info.isaksson.erland.zipgithub.comparison.ImportComparison;
import info.isaksson.erland.zipgithub.comparison.ImportComparisonService;
import info.isaksson.erland.zipgithub.domain.model.ImportSource;
import info.isaksson.erland.zipgithub.persistence.ProjectPersistenceStore;
import info.isaksson.erland.zipgithub.persistence.WorkPersistenceStore;
import info.isaksson.erland.zipgithub.plan.ImmutableImportPlan;
import info.isaksson.erland.zipgithub.plan.ImportPlanFactory;
import info.isaksson.erland.zipgithub.policy.ImportPolicyResult;
import info.isaksson.erland.zipgithub.policy.ImportPolicyService;
import info.isaksson.erland.zipgithub.selection.ImportSelectionFactory;
import info.isaksson.erland.zipgithub.snapshot.RepositorySnapshot;
import info.isaksson.erland.zipgithub.snapshot.RepositorySnapshotEntry;
import info.isaksson.erland.zipgithub.upload.StoredUpload;
import info.isaksson.erland.zipgithub.upload.StreamingUploadService;
import info.isaksson.erland.zipgithub.upload.UploadTooLargeException;
import info.isaksson.erland.zipgithub.upload.ZipIngestionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Step 7.14: both ingestion paths must converge on the same ordinary import semantics. */
class AlternativeZipIngestionRegressionTest {
    private static final String BASE_SHA = "b".repeat(40);
    private static final Instant NOW = Instant.parse("2026-08-07T17:00:00Z");

    @TempDir Path temp;

    @Test
    void browserAndStoredZipProduceEquivalentInventoryPolicyAndPlanEntries() throws Exception {
        byte[] zipBytes = zip(
                "README.md", "new readme\n",
                "src/NewFile.java", "final class NewFile {}\n");
        Fixture fixture = fixture();
        ZipIngestionService ingestion = new ZipIngestionService(temp.resolve("uploads").toString(), 1_000_000, 24);
        StreamingUploadService browserUploader = new StreamingUploadService(ingestion);

        var browserImport = fixture.service.createImport(fixture.owner, fixture.projectId,
                new CreateImportRequest(null, null), "Erland", "erland@example.invalid");
        StoredUpload browserUpload = browserUploader.store(fixture.owner, browserImport.id(), "project.zip",
                zipBytes.length, new ByteArrayInputStream(zipBytes));
        fixture.service.recordUpload(fixture.owner, browserImport.id(), browserUpload);

        var stagedArtifact = ingestion.store(UUID.randomUUID(), "project.zip", zipBytes.length,
                new ByteArrayInputStream(zipBytes));
        var promoted = fixture.service.createImportFromStoredUpload(fixture.owner, fixture.projectId,
                new CreateImportRequest(null, null), "Erland", "erland@example.invalid", stagedArtifact,
                "staging-regression-1");

        assertNotEquals(browserImport.id(), promoted.importSession().id());
        assertEquals(browserUpload.sha256(), promoted.sourceUpload().sha256());
        assertEquals(ImportSource.WEB_UPLOAD,
                fixture.service.importAuditMetadata(fixture.owner, browserImport.id()).source());
        assertEquals(ImportSource.STORED_UPLOAD,
                fixture.service.importAuditMetadata(fixture.owner, promoted.importSession().id()).source());

        ArchiveInventoryService inventoryService = inventoryService();
        ArchiveInventory browserInventory = inventoryService.createInventory(browserUpload.storagePath());
        ArchiveInventory promotedInventory = inventoryService.createInventory(stagedArtifact.storagePath());
        assertEquals(browserInventory, promotedInventory, "identical ZIP bytes must inventory identically");

        RepositorySnapshot browserSnapshot = snapshot(browserImport.id());
        RepositorySnapshot promotedSnapshot = snapshot(promoted.importSession().id());
        ImportComparisonService comparisonService = new ImportComparisonService();
        ImportComparison browserComparison = comparisonService.compare(browserInventory, browserSnapshot);
        ImportComparison promotedComparison = comparisonService.compare(promotedInventory, promotedSnapshot);
        assertEquals(browserComparison.entries(), promotedComparison.entries());

        ImportPolicyService policyService = new ImportPolicyService(50_000_000);
        ImportPolicyResult browserPolicy = policyService.evaluate(browserInventory, browserComparison);
        ImportPolicyResult promotedPolicy = policyService.evaluate(promotedInventory, promotedComparison);
        assertEquals(browserPolicy.policyVersion(), promotedPolicy.policyVersion());
        assertEquals(browserPolicy.approvable(), promotedPolicy.approvable());
        assertEquals(browserPolicy.entries(), promotedPolicy.entries());

        ImportPlanFactory planFactory = new ImportPlanFactory();
        ImmutableImportPlan browserPlan = planFactory.create(fixture.owner, browserUpload.sha256(),
                browserInventory, browserPolicy, NOW);
        ImmutableImportPlan promotedPlan = planFactory.create(fixture.owner, promoted.sourceUpload().sha256(),
                promotedInventory, promotedPolicy, NOW);
        assertEquals(browserPlan.sourceUploadSha256(), promotedPlan.sourceUploadSha256());
        assertEquals(browserPlan.baseCommitSha(), promotedPlan.baseCommitSha());
        assertEquals(browserPlan.policyVersion(), promotedPlan.policyVersion());
        assertEquals(browserPlan.entries(), promotedPlan.entries());
        assertNotEquals(browserPlan.planDigestSha256(), promotedPlan.planDigestSha256(),
                "different import IDs are intentionally part of plan identity");
    }

    @Test
    void neutralAndBrowserIngestionShareTheSameAbsoluteSizeBoundary() {
        byte[] tooLarge = new byte[33];
        ZipIngestionService ingestion = new ZipIngestionService(temp.resolve("limits").toString(), 32, 24);
        StreamingUploadService browserUploader = new StreamingUploadService(ingestion);

        assertThrows(UploadTooLargeException.class, () -> ingestion.store(UUID.randomUUID(), "staged.zip",
                tooLarge.length, new ByteArrayInputStream(tooLarge)));
        assertThrows(UploadTooLargeException.class, () -> browserUploader.store(UUID.randomUUID(), UUID.randomUUID(),
                "browser.zip", tooLarge.length, new ByteArrayInputStream(tooLarge)));
    }

    @Test
    void promotionRetryOwnershipCleanupAndDigestContractsRemainStable() throws Exception {
        byte[] zipBytes = zip("README.md", "new readme\n");
        Fixture fixture = fixture();
        ZipIngestionService ingestion = new ZipIngestionService(temp.resolve("promotion").toString(), 1_000_000, 24);
        var artifact = ingestion.store(UUID.randomUUID(), "promotion.zip", zipBytes.length,
                new ByteArrayInputStream(zipBytes));

        var first = fixture.service.createImportFromStoredUpload(fixture.owner, fixture.projectId, null,
                "Erland", "erland@example.invalid", artifact, "same-operation");
        var retry = fixture.service.createImportFromStoredUpload(fixture.owner, fixture.projectId, null,
                "Erland", "erland@example.invalid", artifact, "same-operation");
        assertEquals(first.importSession().id(), retry.importSession().id());
        assertTrue(fixture.service.expiredUploads(Instant.parse("2100-01-01T00:00:00Z")).isEmpty(),
                "promoted artifacts use the ordinary resumable-import retention model and are protected until terminal delivery");

        UUID otherUser = UUID.randomUUID();
        ApiException notOwned = assertThrows(ApiException.class,
                () -> fixture.service.getImport(otherUser, first.importSession().id()));
        assertEquals(404, notOwned.status());

        ArchiveInventory inventory = inventoryService().createInventory(artifact.storagePath());
        ImportComparison comparison = new ImportComparisonService().compare(inventory,
                new RepositorySnapshot(first.importSession().id(), "erland/example", "main", BASE_SHA,
                        List.of(new RepositorySnapshotEntry("README.md", "100644", "blob", "1".repeat(40),
                                "old readme\n".getBytes(StandardCharsets.UTF_8).length, sha256("old readme\n"))), NOW));
        ImportPolicyResult policy = new ImportPolicyService(50_000_000).evaluate(inventory, comparison);
        ImportPlanFactory planFactory = new ImportPlanFactory();
        ImmutableImportPlan planA = planFactory.create(fixture.owner, artifact.sha256(), inventory, policy, NOW);
        ImmutableImportPlan planB = planFactory.create(fixture.owner, artifact.sha256(), inventory, policy, NOW.plusSeconds(5));
        assertEquals(planA.planDigestSha256(), planB.planDigestSha256(),
                "audit timestamps/source metadata are outside the immutable plan digest contract");

        ImportSelectionFactory selectionFactory = new ImportSelectionFactory();
        var selectionA = selectionFactory.create(fixture.owner, planA, planA.planDigestSha256(), BASE_SHA,
                List.of("README.md"), List.of(), NOW);
        var selectionB = selectionFactory.create(fixture.owner, planA, planA.planDigestSha256(), BASE_SHA,
                List.of("README.md"), List.of(), NOW.plusSeconds(10));
        assertEquals(selectionA.selectionDigestSha256(), selectionB.selectionDigestSha256(),
                "creation time and import-source audit metadata are outside selection identity");
    }

    private ArchiveInventoryService inventoryService() {
        ArchiveInspectionService.ArchiveLimitConfig limits = new ArchiveInspectionService.ArchiveLimitConfig() {
            public long maxCompressedBytes() { return 1_000_000; }
            public long maxUncompressedBytes() { return 5_000_000; }
            public int maxEntries() { return 100; }
            public long maxSingleFileBytes() { return 1_000_000; }
            public int maxPathLength() { return 512; }
            public double maxCompressionRatio() { return 100.0; }
        };
        return new ArchiveInventoryService(new ArchiveInspectionService(limits));
    }

    private RepositorySnapshot snapshot(UUID importId) {
        byte[] oldReadme = "old readme\n".getBytes(StandardCharsets.UTF_8);
        return new RepositorySnapshot(importId, "erland/example", "main", BASE_SHA,
                List.of(new RepositorySnapshotEntry("README.md", "100644", "blob", "1".repeat(40),
                        oldReadme.length, sha256(oldReadme))), NOW);
    }

    private static byte[] zip(String... pathContentPairs) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (int i = 0; i < pathContentPairs.length; i += 2) {
                zip.putNextEntry(new ZipEntry(pathContentPairs[i]));
                zip.write(pathContentPairs[i + 1].getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    private static String sha256(String text) { return sha256(text.getBytes(StandardCharsets.UTF_8)); }
    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static Fixture fixture() {
        ProjectApplicationService service = new ProjectApplicationService();
        service.persistentProjects = mock(ProjectPersistenceStore.class);
        service.persistentWork = mock(WorkPersistenceStore.class);
        service.githubConfiguration = mock(GitHubProjectConfigurationService.class);
        when(service.persistentProjects.enabled()).thenReturn(false);
        when(service.githubConfiguration.verify(anyString(), anyLong(), anyLong(), anyString()))
                .thenReturn(new GitHubProjectConfigurationService.VerifiedRepository(
                        10L, "erland", "selected", 20L, "erland/example", true, "main"));
        UUID owner = UUID.randomUUID();
        var project = service.createProject(owner, "token", new CreateProjectRequest("Example", 10L, 20L, "main"));
        return new Fixture(service, owner, project.id());
    }

    private record Fixture(ProjectApplicationService service, UUID owner, UUID projectId) { }
}
