package info.isaksson.erland.zipbuildserver.application;

import info.isaksson.erland.zipbuildserver.api.session.CreateSessionRequest;
import info.isaksson.erland.zipbuildserver.api.session.SessionResponse;
import info.isaksson.erland.zipbuildserver.domain.model.SessionStatus;
import info.isaksson.erland.zipbuildserver.infrastructure.persistence.entity.VerificationSessionEntity;
import info.isaksson.erland.zipbuildserver.infrastructure.persistence.repository.VerificationSessionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class VerificationSessionService {
    private static final String DEFAULT_RETENTION_POLICY = "default";

    private final VerificationSessionRepository repository;

    public VerificationSessionService(VerificationSessionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public SessionResponse create(CreateSessionRequest request) {
        VerificationSessionEntity entity = new VerificationSessionEntity();
        entity.id = UUID.randomUUID();
        entity.label = normalize(request == null ? null : request.label());
        entity.status = SessionStatus.OPEN;
        entity.createdAt = OffsetDateTime.now();
        entity.createdBy = null;
        entity.retentionPolicy = normalizeRetention(request == null ? null : request.retentionPolicy());

        repository.persist(entity);
        return toResponse(entity);
    }

    public SessionResponse get(UUID id) {
        return toResponse(findRequired(id));
    }

    public List<SessionResponse> list() {
        return repository.listAll().stream()
                .sorted(Comparator.comparing((VerificationSessionEntity session) -> session.createdAt).reversed())
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SessionResponse close(UUID id) {
        VerificationSessionEntity entity = findRequired(id);
        if (entity.status != SessionStatus.CLOSED) {
            entity.status = SessionStatus.CLOSED;
            entity.closedAt = OffsetDateTime.now();
        }
        return toResponse(entity);
    }

    private VerificationSessionEntity findRequired(UUID id) {
        if (id == null) {
            throw new NotFoundException("Session was not found.");
        }
        return repository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Session was not found: " + id));
    }

    private SessionResponse toResponse(VerificationSessionEntity entity) {
        return new SessionResponse(
                entity.id,
                entity.label,
                entity.status,
                entity.createdAt,
                entity.closedAt,
                entity.createdBy,
                entity.retentionPolicy);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String normalizeRetention(String value) {
        String normalized = normalize(value);
        return normalized == null ? DEFAULT_RETENTION_POLICY : normalized;
    }
}
