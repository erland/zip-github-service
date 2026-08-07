package info.isaksson.erland.zipgithub.policy;

import info.isaksson.erland.zipgithub.archive.ArchiveInventory;
import info.isaksson.erland.zipgithub.comparison.ImportComparison;
import info.isaksson.erland.zipgithub.comparison.ImportComparisonEntry;
import info.isaksson.erland.zipgithub.comparison.ImportFileStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.Set;

/** Applies the deterministic MVP import policy without mutating either source inventory. */
@ApplicationScoped
public class ImportPolicyService {
    public static final String POLICY_VERSION = "mvp-1";
    private static final Set<String> PRIVATE_KEY_FILENAMES = Set.of(
            "id_rsa", "id_dsa", "id_ecdsa", "id_ed25519", "identity", "credentials.json", "service-account.json");

    private final long maximumFileBytes;

    @Inject
    public ImportPolicyService(
            @ConfigProperty(name = "zipgithub.archive.max-single-file-bytes", defaultValue = "52428800") long maximumFileBytes) {
        if (maximumFileBytes <= 0) throw new IllegalArgumentException("maximumFileBytes must be positive");
        this.maximumFileBytes = maximumFileBytes;
    }


    public ImportPolicyResult evaluate(ArchiveInventory archive, ImportComparison comparison) {
        if (archive == null) throw new IllegalArgumentException("archive is required");
        if (comparison == null) throw new IllegalArgumentException("comparison is required");

        var entries = new ArrayList<ImportPolicyEntry>();
        for (String ignoredPath : archive.ignoredPaths()) {
            entries.add(new ImportPolicyEntry(ignoredPath, ImportFileStatus.IGNORED, null,
                    ImportPolicySeverity.NONE, "TRANSPORT_NOISE", "Transport metadata is ignored.",
                    null, null, null, null));
        }
        for (ImportComparisonEntry entry : comparison.entries()) entries.add(apply(entry));
        entries.sort(Comparator.comparing(ImportPolicyEntry::path));
        boolean approvable = entries.stream().noneMatch(item -> item.severity() == ImportPolicySeverity.BLOCKING);
        return new ImportPolicyResult(comparison.importId(), comparison.baseCommitSha(), POLICY_VERSION, approvable, entries);
    }

    private ImportPolicyEntry apply(ImportComparisonEntry entry) {
        String normalized = entry.path().replace('\\', '/');
        String lower = normalized.toLowerCase(Locale.ROOT);
        String filename = lower.substring(lower.lastIndexOf('/') + 1);

        if (lower.equals(".git") || lower.startsWith(".git/")) {
            return blocked(entry, "GIT_METADATA_PROTECTED", "Git repository metadata may never be imported.");
        }
        if (lower.equals(".github") || lower.startsWith(".github/")) {
            return blocked(entry, "GITHUB_WORKFLOW_PROTECTED", "Changes under .github/** are blocked in the MVP.");
        }
        if (entry.status() == ImportFileStatus.WOULD_DELETE) {
            return blocked(entry, "DELETION_BLOCKED", "Repository file deletion is blocked in the MVP.");
        }
        if (entry.archiveSizeBytes() != null && entry.archiveSizeBytes() > maximumFileBytes) {
            return blocked(entry, "FILE_TOO_LARGE", "The file exceeds the configured import-policy size limit.");
        }
        if (isHighRiskSecret(filename)) {
            return blocked(entry, "HIGH_RISK_SECRET_FILE", "The path resembles a private key or high-risk credential file.");
        }
        if (isEnvironmentFile(filename)) {
            return warning(entry, "ENVIRONMENT_FILE_WARNING", "Environment files may contain secrets and require careful review.");
        }
        return unchangedPolicy(entry);
    }

    private static boolean isHighRiskSecret(String filename) {
        return PRIVATE_KEY_FILENAMES.contains(filename)
                || filename.endsWith(".pem") || filename.endsWith(".key")
                || filename.endsWith(".p12") || filename.endsWith(".pfx")
                || filename.endsWith(".jks") || filename.endsWith(".keystore");
    }

    private static boolean isEnvironmentFile(String filename) {
        return filename.equals(".env") || (filename.startsWith(".env.") && !filename.equals(".env.example"));
    }

    private static ImportPolicyEntry blocked(ImportComparisonEntry entry, String code, String message) {
        return from(entry, ImportFileStatus.BLOCKED, ImportPolicySeverity.BLOCKING, code, message);
    }

    private static ImportPolicyEntry warning(ImportComparisonEntry entry, String code, String message) {
        return from(entry, entry.status(), ImportPolicySeverity.WARNING, code, message);
    }

    private static ImportPolicyEntry unchangedPolicy(ImportComparisonEntry entry) {
        return from(entry, entry.status(), ImportPolicySeverity.NONE, null, null);
    }

    private static ImportPolicyEntry from(ImportComparisonEntry entry, ImportFileStatus status,
                                          ImportPolicySeverity severity, String code, String message) {
        return new ImportPolicyEntry(entry.path(), status, entry.status(), severity, code, message,
                entry.archiveSizeBytes(), entry.archiveSha256(), entry.repositorySizeBytes(), entry.repositorySha256());
    }
}
