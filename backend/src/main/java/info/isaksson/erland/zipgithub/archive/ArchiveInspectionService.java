package info.isaksson.erland.zipgithub.archive;

import io.smallrye.config.ConfigMapping;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@ApplicationScoped
public class ArchiveInspectionService {
    private final ArchiveResourceLimits limits;

    @Inject
    public ArchiveInspectionService(ArchiveLimitConfig config) {
        this(new ArchiveResourceLimits(
                config.maxCompressedBytes(),
                config.maxUncompressedBytes(),
                config.maxEntries(),
                config.maxSingleFileBytes(),
                config.maxPathLength(),
                config.maxCompressionRatio()));
    }

    ArchiveInspectionService(ArchiveResourceLimits limits) {
        this.limits = limits;
    }

    public List<ArchiveEntryDescriptor> inspect(Path zipFile) throws IOException {
        return new SecureZipInspector().inspect(zipFile, limits);
    }

    @ConfigMapping(prefix = "zipgithub.archive")
    public interface ArchiveLimitConfig {
        long maxCompressedBytes();
        long maxUncompressedBytes();
        int maxEntries();
        long maxSingleFileBytes();
        int maxPathLength();
        double maxCompressionRatio();
    }
}
