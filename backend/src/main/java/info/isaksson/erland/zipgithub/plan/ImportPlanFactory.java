package info.isaksson.erland.zipgithub.plan;

import info.isaksson.erland.zipgithub.archive.ArchiveInventory;
import info.isaksson.erland.zipgithub.archive.ArchiveInventoryEntry;
import info.isaksson.erland.zipgithub.policy.ImportPolicyEntry;
import info.isaksson.erland.zipgithub.policy.ImportPolicyResult;
import jakarta.enterprise.context.ApplicationScoped;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Creates a canonical immutable plan and digest from already validated source material. */
@ApplicationScoped
public class ImportPlanFactory {

    public ImmutableImportPlan create(UUID ownerUserId, String sourceUploadSha256,
                                      ArchiveInventory archive, ImportPolicyResult policy, Instant createdAt) {
        Map<String, ArchiveInventoryEntry> archiveFiles = new HashMap<>();
        for (ArchiveInventoryEntry file : archive.files()) archiveFiles.put(file.path(), file);

        List<ImmutableImportPlanEntry> entries = policy.entries().stream()
                .map(entry -> toPlanEntry(entry, archiveFiles.get(entry.path())))
                .sorted(Comparator.comparing(ImmutableImportPlanEntry::path))
                .toList();
        String status = policy.approvable() ? "READY" : "DRAFT";
        String digest = digest(policy.importId(), sourceUploadSha256, policy.baseCommitSha(),
                policy.policyVersion(), status, entries);
        return new ImmutableImportPlan(UUID.randomUUID(), policy.importId(), ownerUserId,
                sourceUploadSha256, policy.baseCommitSha(), policy.policyVersion(), digest,
                status, policy.approvable(), entries, createdAt);
    }

    private static ImmutableImportPlanEntry toPlanEntry(ImportPolicyEntry entry, ArchiveInventoryEntry archiveFile) {
        return new ImmutableImportPlanEntry(entry.path(), entry.status().name(),
                entry.comparisonStatus() == null ? null : entry.comparisonStatus().name(),
                entry.severity().name(), entry.blockerType().name(), entry.policyCode(), entry.message(),
                entry.archiveSizeBytes(), entry.archiveSha256(), entry.repositorySizeBytes(),
                entry.repositorySha256(), archiveFile != null && archiveFile.textCandidate());
    }

    static String digest(UUID importId, String uploadSha, String baseSha, String policyVersion,
                         String status, List<ImmutableImportPlanEntry> entries) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            append(digest, "importId", importId.toString());
            append(digest, "sourceUploadSha256", uploadSha);
            append(digest, "baseCommitSha", baseSha);
            append(digest, "policyVersion", policyVersion);
            append(digest, "status", status);
            for (ImmutableImportPlanEntry entry : entries) {
                append(digest, "path", entry.path());
                append(digest, "status", entry.status());
                append(digest, "comparisonStatus", entry.comparisonStatus());
                append(digest, "severity", entry.severity());
                append(digest, "blockerType", entry.blockerType());
                append(digest, "policyCode", entry.policyCode());
                append(digest, "message", entry.message());
                append(digest, "archiveSizeBytes", entry.archiveSizeBytes());
                append(digest, "archiveSha256", entry.archiveSha256());
                append(digest, "repositorySizeBytes", entry.repositorySizeBytes());
                append(digest, "repositorySha256", entry.repositorySha256());
                append(digest, "textCandidate", entry.textCandidate());
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private static void append(MessageDigest digest, String key, Object value) {
        String text = key + "=" + (value == null ? "<null>" : value.toString()) + "\n";
        digest.update(text.getBytes(StandardCharsets.UTF_8));
    }
}
