package info.isaksson.erland.zipbuildserver.infrastructure.persistence.repository;

import info.isaksson.erland.zipbuildserver.infrastructure.persistence.entity.ArtifactReferenceEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class ArtifactReferenceRepository implements PanacheRepositoryBase<ArtifactReferenceEntity, UUID> {
}
