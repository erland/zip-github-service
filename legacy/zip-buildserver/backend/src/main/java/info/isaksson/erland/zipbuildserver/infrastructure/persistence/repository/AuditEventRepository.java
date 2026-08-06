package info.isaksson.erland.zipbuildserver.infrastructure.persistence.repository;

import info.isaksson.erland.zipbuildserver.infrastructure.persistence.entity.AuditEventEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class AuditEventRepository implements PanacheRepositoryBase<AuditEventEntity, UUID> {
}
