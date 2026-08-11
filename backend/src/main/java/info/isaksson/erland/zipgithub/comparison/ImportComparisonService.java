package info.isaksson.erland.zipgithub.comparison;

import info.isaksson.erland.zipgithub.archive.ArchiveInventory;
import info.isaksson.erland.zipgithub.archive.ArchiveInventoryEntry;
import info.isaksson.erland.zipgithub.snapshot.RepositorySnapshot;
import info.isaksson.erland.zipgithub.snapshot.RepositorySnapshotEntry;
import info.isaksson.erland.zipgithub.upload.GitFileMode;
import info.isaksson.erland.zipgithub.upload.GitFileModeResolver;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

/** Deterministically compares normalized ZIP content with one immutable repository snapshot. */
@ApplicationScoped
public class ImportComparisonService {
    public ImportComparison compare(ArchiveInventory archive, RepositorySnapshot repository) {
        return compare(archive, repository, Map.of());
    }

    public ImportComparison compare(ArchiveInventory archive, RepositorySnapshot repository, Map<String, GitFileMode> uploadModes) {
        if (archive == null) throw new IllegalArgumentException("archive is required");
        if (repository == null) throw new IllegalArgumentException("repository is required");
        uploadModes = uploadModes == null ? Map.of() : Map.copyOf(uploadModes);

        Map<String, ArchiveInventoryEntry> archiveByPath = new TreeMap<>();
        for (ArchiveInventoryEntry entry : archive.files()) archiveByPath.put(entry.path(), entry);
        Map<String, RepositorySnapshotEntry> repositoryByPath = new TreeMap<>();
        for (RepositorySnapshotEntry entry : repository.entries()) {
            if ("blob".equals(entry.objectType())) repositoryByPath.put(entry.path(), entry);
        }

        var allPaths = new TreeMap<String, Boolean>();
        archiveByPath.keySet().forEach(path -> allPaths.put(path, Boolean.TRUE));
        repositoryByPath.keySet().forEach(path -> allPaths.put(path, Boolean.TRUE));

        Map<String, String> prospectiveGitIgnoreFiles = new TreeMap<>(repository.gitIgnoreFiles());
        prospectiveGitIgnoreFiles.keySet().removeIf(path -> !archiveByPath.containsKey(path));
        prospectiveGitIgnoreFiles.putAll(archive.gitIgnoreFiles());
        GitIgnoreMatcher ignoreMatcher = new GitIgnoreMatcher(prospectiveGitIgnoreFiles);
        var result = new ArrayList<ImportComparisonEntry>();
        for (String path : allPaths.keySet()) {
            ArchiveInventoryEntry source = archiveByPath.get(path);
            RepositorySnapshotEntry target = repositoryByPath.get(path);
            GitFileMode suppliedMode = source == null ? null : uploadModes.get(path);
            String archiveMode = suppliedMode == null ? null : suppliedMode.gitMode();
            String repositoryMode = target == null ? null : target.mode();
            String effectiveMode = source == null ? null : GitFileModeResolver.effectiveMode(suppliedMode, repositoryMode, target != null);
            boolean modeChanged = source != null && target != null && !effectiveMode.equals(repositoryMode);
            ImportFileStatus status;
            if (source == null) status = ImportFileStatus.WOULD_DELETE;
            else if (target == null && ignoreMatcher.isIgnored(path)) status = ImportFileStatus.IGNORED;
            else if (target == null) status = ImportFileStatus.ADDED;
            else if (source.sha256().equalsIgnoreCase(target.sha256()) && !modeChanged) status = ImportFileStatus.UNCHANGED;
            else status = ImportFileStatus.MODIFIED;
            result.add(new ImportComparisonEntry(path, status,
                    source == null ? null : source.size(), source == null ? null : source.sha256(),
                    target == null ? null : target.sizeBytes(), target == null ? null : target.sha256(),
                    archiveMode, repositoryMode, effectiveMode, modeChanged));
        }
        return new ImportComparison(repository.importId(), repository.baseCommitSha(), result);
    }
}
