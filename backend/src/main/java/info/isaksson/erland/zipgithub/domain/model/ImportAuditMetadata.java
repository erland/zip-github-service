package info.isaksson.erland.zipgithub.domain.model;

import java.util.Objects;

/** Non-secret import-source audit metadata. Secret capability/claim tokens must never be stored here. */
public record ImportAuditMetadata(ImportSource source, String sourceReference) {
    public ImportAuditMetadata {
        Objects.requireNonNull(source, "source");
        if (sourceReference != null) {
            sourceReference = sourceReference.trim();
            if (sourceReference.isEmpty()) sourceReference = null;
            if (sourceReference != null && sourceReference.length() > 255) {
                throw new IllegalArgumentException("sourceReference must be at most 255 characters");
            }
        }
    }

    public static ImportAuditMetadata webUpload() {
        return new ImportAuditMetadata(ImportSource.WEB_UPLOAD, null);
    }
}
