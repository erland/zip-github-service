package info.isaksson.erland.zipbuildserver.storage;

import info.isaksson.erland.zipbuildserver.application.NotFoundException;
import info.isaksson.erland.zipbuildserver.domain.model.ArtifactType;
import info.isaksson.erland.zipbuildserver.infrastructure.persistence.entity.ArtifactReferenceEntity;
import info.isaksson.erland.zipbuildserver.infrastructure.persistence.repository.ArtifactReferenceRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ArtifactStorageService {
    private final ArtifactReferenceRepository artifactRepository;
    private final Path artifactsDir;
    private final int retentionDays;

    public ArtifactStorageService(
            ArtifactReferenceRepository artifactRepository,
            @ConfigProperty(name = "zip-buildserver.storage.artifacts-dir") String artifactsDir,
            @ConfigProperty(name = "zip-buildserver.artifacts.retention-days", defaultValue = "14") int retentionDays) {
        this.artifactRepository = artifactRepository;
        this.artifactsDir = Path.of(artifactsDir);
        this.retentionDays = retentionDays;
    }

    public ArtifactReferenceEntity storeText(UUID runId, ArtifactType type, String commandLabel, String content) {
        String safeCommandLabel = commandLabel == null || commandLabel.isBlank()
                ? "command"
                : commandLabel.replaceAll("[^A-Za-z0-9._-]", "_");
        UUID artifactId = UUID.randomUUID();
        Path runDir = artifactsDir.resolve(runId.toString()).normalize();
        Path artifactPath = runDir.resolve(artifactId + "-" + safeCommandLabel + "-" + type.name().toLowerCase() + ".log").normalize();

        if (!artifactPath.startsWith(runDir)) {
            throw new IllegalArgumentException("Artifact path escaped the run artifact directory.");
        }

        try {
            Files.createDirectories(runDir);
            byte[] bytes = (content == null ? "" : content).getBytes(StandardCharsets.UTF_8);
            Files.write(artifactPath, bytes);

            ArtifactReferenceEntity entity = new ArtifactReferenceEntity();
            entity.id = artifactId;
            entity.runId = runId;
            entity.type = type;
            entity.storageReference = artifactPath.toString();
            entity.sizeBytes = bytes.length;
            entity.createdAt = OffsetDateTime.now();
            entity.expiresAt = entity.createdAt.plusDays(retentionDays);
            artifactRepository.persist(entity);
            return entity;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store verification artifact.", e);
        }
    }

    public List<ArtifactReferenceEntity> listForRun(UUID runId) {
        return artifactRepository.list("runId", runId).stream()
                .sorted(Comparator.comparing((ArtifactReferenceEntity artifact) -> artifact.createdAt)
                        .thenComparing(artifact -> artifact.id))
                .toList();
    }

    public ArtifactReferenceEntity get(UUID artifactId) {
        return artifactRepository.findByIdOptional(artifactId)
                .orElseThrow(() -> new NotFoundException("Artifact was not found: " + artifactId));
    }

    public String readText(UUID artifactId) {
        ArtifactReferenceEntity artifact = get(artifactId);
        try {
            return Files.readString(Path.of(artifact.storageReference), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new NotFoundException("Artifact content was not found: " + artifactId);
        }
    }
}
