package info.isaksson.erland.zipbuildserver.api.packageupload;

import info.isaksson.erland.zipbuildserver.domain.model.SourcePackageStatus;
import info.isaksson.erland.zipbuildserver.domain.model.project.ProjectDetectionSummary;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PackageResponse(
        UUID id,
        UUID sessionId,
        String originalFilename,
        String checksumSha256,
        long compressedSizeBytes,
        Long extractedSizeBytes,
        Integer fileCount,
        String topLevelEntries,
        String storageReference,
        SourcePackageStatus status,
        String rejectionReason,
        OffsetDateTime createdAt,
        ProjectDetectionSummary projectDetection) {
}
