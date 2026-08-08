package info.isaksson.erland.zipgithub.upload;

import info.isaksson.erland.zipgithub.archive.ArchiveEntryDescriptor;
import info.isaksson.erland.zipgithub.archive.ArchiveEntryType;
import info.isaksson.erland.zipgithub.archive.ArchiveInspectionService;
import info.isaksson.erland.zipgithub.archive.ArchiveNormalization;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Extracts only trustworthy Unix executable metadata from a validated ZIP central directory. */
@ApplicationScoped
public class UploadFileModeService {
    private final ArchiveInspectionService inspection;

    @Inject
    public UploadFileModeService(ArchiveInspectionService inspection) {
        this.inspection = inspection;
    }

    public Map<String, GitFileMode> inspect(Path zipFile) throws IOException {
        List<ArchiveEntryDescriptor> entries = inspection.inspect(zipFile);
        List<String> filePaths = entries.stream()
                .filter(e -> e.type() == ArchiveEntryType.REGULAR_FILE)
                .map(ArchiveEntryDescriptor::path).toList();
        String wrapper = ArchiveNormalization.detectSingleWrapper(filePaths);
        Map<String, GitFileMode> modes = new LinkedHashMap<>();
        for (ArchiveEntryDescriptor entry : entries) {
            if (entry.type() != ArchiveEntryType.REGULAR_FILE || entry.unixMode() == 0) continue;
            String path = ArchiveNormalization.stripWrapper(entry.path(), wrapper);
            if (ArchiveNormalization.isTransportNoise(entry.path()) || ".DS_Store".equals(path)) continue;
            modes.put(path, GitFileMode.fromUnixMode(entry.unixMode()));
        }
        return Map.copyOf(modes);
    }
}
