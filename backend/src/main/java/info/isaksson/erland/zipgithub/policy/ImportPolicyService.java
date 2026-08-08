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
    public static final String POLICY_VERSION = "mvp-4";
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
                    ImportPolicySeverity.NONE, ImportPolicyBlockerType.NONE, "TRANSPORT_NOISE", "Transport metadata is ignored.",
                    null, null, null, null));
        }
        for (ImportComparisonEntry entry : comparison.entries()) entries.add(apply(entry));
        entries.sort(Comparator.comparing(ImportPolicyEntry::path));
        // Policy-blocked entries are excluded from the current default delivery set. A mixed
        // plan can therefore proceed as long as at least one ordinary added/modified file remains.
        // Step 7.7 will make this default set explicit in an immutable selection object.
        boolean approvable = entries.stream().anyMatch(ImportPolicyService::isDefaultIncludedChange);
        return new ImportPolicyResult(comparison.importId(), comparison.baseCommitSha(), POLICY_VERSION, approvable, entries);
    }

    private ImportPolicyEntry apply(ImportComparisonEntry entry) {
        String normalized = entry.path().replace('\\', '/');
        String lower = normalized.toLowerCase(Locale.ROOT);
        String filename = lower.substring(lower.lastIndexOf('/') + 1);

        if (lower.equals(".git") || lower.startsWith(".git/")) {
            return hardBlocked(entry, "GIT_METADATA_PROTECTED", "Git repository metadata may never be imported or selected.");
        }
        if (entry.status() == ImportFileStatus.IGNORED) {
            return from(entry, ImportFileStatus.IGNORED, ImportPolicySeverity.WARNING, ImportPolicyBlockerType.NONE,
                    "GITIGNORE_IGNORED", "Filen matchar repositoryts .gitignore och kommer inte att tas med i Git-committen.");
        }
        if ((lower.equals(".github") || lower.startsWith(".github/")) && isRepositoryChange(entry.status())) {
            return overridableBlocked(entry, "GITHUB_WORKFLOW_PROTECTED", "Changes under .github/** are excluded by default and require explicit override before inclusion.");
        }
        if (entry.status() == ImportFileStatus.WOULD_DELETE) {
            return overridableBlocked(entry, "DELETION_REQUIRES_OVERRIDE", "Repository file deletion is excluded by default and requires explicit override before inclusion.");
        }
        if (entry.archiveSizeBytes() != null && entry.archiveSizeBytes() > maximumFileBytes) {
            return hardBlocked(entry, "FILE_TOO_LARGE", "The file exceeds the configured import-policy size limit and may not be selected.");
        }
        if (isHighRiskSecret(filename)) {
            return hardBlocked(entry, "HIGH_RISK_SECRET_FILE", "The path resembles a private key or high-risk credential file and may not be selected.");
        }
        if (isEnvironmentFile(filename)) {
            return warning(entry, "ENVIRONMENT_FILE_WARNING", "Environment files may contain secrets and require careful review.");
        }
        return unchangedPolicy(entry);
    }

    private static boolean isRepositoryChange(ImportFileStatus status) {
        return status == ImportFileStatus.ADDED
                || status == ImportFileStatus.MODIFIED
                || status == ImportFileStatus.WOULD_DELETE;
    }

    private static boolean isDefaultIncludedChange(ImportPolicyEntry entry) {
        return entry.blockerType() == ImportPolicyBlockerType.NONE
                && (entry.status() == ImportFileStatus.ADDED || entry.status() == ImportFileStatus.MODIFIED);
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

    private static ImportPolicyEntry hardBlocked(ImportComparisonEntry entry, String code, String message) {
        return from(entry, ImportFileStatus.BLOCKED, ImportPolicySeverity.BLOCKING,
                ImportPolicyBlockerType.HARD_BLOCKED, code, message);
    }

    private static ImportPolicyEntry overridableBlocked(ImportComparisonEntry entry, String code, String message) {
        return from(entry, ImportFileStatus.BLOCKED, ImportPolicySeverity.BLOCKING,
                ImportPolicyBlockerType.OVERRIDABLE_BLOCKED, code, message);
    }

    private static ImportPolicyEntry warning(ImportComparisonEntry entry, String code, String message) {
        return from(entry, entry.status(), ImportPolicySeverity.WARNING, ImportPolicyBlockerType.NONE, code, message);
    }

    private static ImportPolicyEntry unchangedPolicy(ImportComparisonEntry entry) {
        return from(entry, entry.status(), ImportPolicySeverity.NONE, ImportPolicyBlockerType.NONE, null, null);
    }

    private static ImportPolicyEntry from(ImportComparisonEntry entry, ImportFileStatus status,
                                          ImportPolicySeverity severity, ImportPolicyBlockerType blockerType,
                                          String code, String message) {
        return new ImportPolicyEntry(entry.path(), status, entry.status(), severity, blockerType, code, message,
                entry.archiveSizeBytes(), entry.archiveSha256(), entry.repositorySizeBytes(), entry.repositorySha256());
    }
}
