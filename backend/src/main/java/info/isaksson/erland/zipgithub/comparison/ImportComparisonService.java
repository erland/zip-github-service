package info.isaksson.erland.zipgithub.comparison;

import info.isaksson.erland.zipgithub.archive.ArchiveInventory;
import info.isaksson.erland.zipgithub.archive.ArchiveInventoryEntry;
import info.isaksson.erland.zipgithub.snapshot.RepositorySnapshot;
import info.isaksson.erland.zipgithub.snapshot.RepositorySnapshotEntry;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

/** Deterministically compares normalized ZIP content with one immutable repository snapshot. */
@ApplicationScoped
public class ImportComparisonService {
    public ImportComparison compare(ArchiveInventory archive, RepositorySnapshot repository) {
        if (archive == null) throw new IllegalArgumentException("archive is required");
        if (repository == null) throw new IllegalArgumentException("repository is required");

        Map<String, ArchiveInventoryEntry> archiveByPath = new TreeMap<>();
        for (ArchiveInventoryEntry entry : archive.files()) archiveByPath.put(entry.path(), entry);
        Map<String, RepositorySnapshotEntry> repositoryByPath = new TreeMap<>();
        for (RepositorySnapshotEntry entry : repository.entries()) {
            if ("blob".equals(entry.objectType())) repositoryByPath.put(entry.path(), entry);
        }

        var allPaths = new TreeMap<String, Boolean>();
        archiveByPath.keySet().forEach(path -> allPaths.put(path, Boolean.TRUE));
        repositoryByPath.keySet().forEach(path -> allPaths.put(path, Boolean.TRUE));

        var result = new ArrayList<ImportComparisonEntry>();
        for (String path : allPaths.keySet()) {
            ArchiveInventoryEntry source = archiveByPath.get(path);
            RepositorySnapshotEntry target = repositoryByPath.get(path);
            ImportFileStatus status;
            if (source == null) status = ImportFileStatus.WOULD_DELETE;
            else if (target == null) status = ImportFileStatus.ADDED;
            else if (source.sha256().equalsIgnoreCase(target.sha256())) status = ImportFileStatus.UNCHANGED;
            else status = ImportFileStatus.MODIFIED;
            result.add(new ImportComparisonEntry(path, status,
                    source == null ? null : source.size(), source == null ? null : source.sha256(),
                    target == null ? null : target.sizeBytes(), target == null ? null : target.sha256()));
        }
        return new ImportComparison(repository.importId(), repository.baseCommitSha(), result);
    }
}
