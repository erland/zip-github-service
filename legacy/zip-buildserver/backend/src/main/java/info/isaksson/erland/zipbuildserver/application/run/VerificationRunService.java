package info.isaksson.erland.zipbuildserver.application.run;

import info.isaksson.erland.zipbuildserver.api.run.CreateRunRequest;
import info.isaksson.erland.zipbuildserver.api.run.RunResponse;
import info.isaksson.erland.zipbuildserver.api.run.RunSummaryResponse;
import info.isaksson.erland.zipbuildserver.application.NotFoundException;
import info.isaksson.erland.zipbuildserver.application.mapper.RunResponseMapper;
import info.isaksson.erland.zipbuildserver.application.project.ProjectDetectionService;
import info.isaksson.erland.zipbuildserver.application.verification.VerificationPlanService;
import info.isaksson.erland.zipbuildserver.domain.model.NetworkMode;
import info.isaksson.erland.zipbuildserver.domain.model.RunStatus;
import info.isaksson.erland.zipbuildserver.domain.model.SessionStatus;
import info.isaksson.erland.zipbuildserver.domain.model.SourcePackageStatus;
import info.isaksson.erland.zipbuildserver.domain.model.project.DetectedProject;
import info.isaksson.erland.zipbuildserver.domain.model.project.ProjectDetectionSummary;
import info.isaksson.erland.zipbuildserver.domain.model.verification.VerificationPlan;
import info.isaksson.erland.zipbuildserver.infrastructure.persistence.entity.SourcePackageEntity;
import info.isaksson.erland.zipbuildserver.infrastructure.persistence.entity.VerificationCommandResultEntity;
import info.isaksson.erland.zipbuildserver.infrastructure.persistence.entity.VerificationRunEntity;
import info.isaksson.erland.zipbuildserver.infrastructure.persistence.entity.VerificationSessionEntity;
import info.isaksson.erland.zipbuildserver.infrastructure.persistence.repository.SourcePackageRepository;
import info.isaksson.erland.zipbuildserver.infrastructure.persistence.repository.VerificationCommandResultRepository;
import info.isaksson.erland.zipbuildserver.infrastructure.persistence.repository.VerificationRunRepository;
import info.isaksson.erland.zipbuildserver.infrastructure.persistence.repository.VerificationSessionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class VerificationRunService {
    private final VerificationSessionRepository sessionRepository;
    private final SourcePackageRepository packageRepository;
    private final VerificationRunRepository runRepository;
    private final VerificationCommandResultRepository commandRepository;
    private final ProjectDetectionService projectDetectionService;
    private final VerificationPlanService verificationPlanService;
    private final VerificationExecutionService executionService;
    private final RunResponseMapper runResponseMapper;

    public VerificationRunService(
            VerificationSessionRepository sessionRepository,
            SourcePackageRepository packageRepository,
            VerificationRunRepository runRepository,
            VerificationCommandResultRepository commandRepository,
            ProjectDetectionService projectDetectionService,
            VerificationPlanService verificationPlanService,
            VerificationExecutionService executionService,
            RunResponseMapper runResponseMapper) {
        this.sessionRepository = sessionRepository;
        this.packageRepository = packageRepository;
        this.runRepository = runRepository;
        this.commandRepository = commandRepository;
        this.projectDetectionService = projectDetectionService;
        this.verificationPlanService = verificationPlanService;
        this.executionService = executionService;
        this.runResponseMapper = runResponseMapper;
    }

    @Transactional
    public RunResponse create(UUID sessionId, CreateRunRequest request) {
        VerificationSessionEntity session = sessionRepository.findByIdOptional(sessionId)
                .orElseThrow(() -> new NotFoundException("Session was not found: " + sessionId));
        if (session.status == SessionStatus.CLOSED) {
            throw new BadRequestException("Cannot create a run for a closed session.");
        }

        UUID packageId = request == null ? null : request.packageId();
        if (packageId == null) {
            throw new BadRequestException("packageId is required.");
        }

        SourcePackageEntity sourcePackage = packageRepository.findByIdOptional(packageId)
                .orElseThrow(() -> new NotFoundException("Package was not found: " + packageId));
        if (!sourcePackage.sessionId.equals(sessionId)) {
            throw new BadRequestException("Package does not belong to the requested session.");
        }
        if (sourcePackage.status != SourcePackageStatus.ACCEPTED) {
            throw new BadRequestException("Cannot create a run for a rejected package.");
        }

        VerificationPlan plan = selectPlan(sourcePackage, normalize(request.requestedPlanId()));

        VerificationRunEntity entity = new VerificationRunEntity();
        entity.id = UUID.randomUUID();
        entity.sessionId = sessionId;
        entity.sourcePackageId = packageId;
        entity.status = RunStatus.QUEUED;
        entity.planId = plan.id();
        entity.requestedPlanId = normalize(request.requestedPlanId());
        entity.networkMode = plan.networkMode();
        entity.summary = "Run queued for fake verification execution.";
        entity.startedAt = null;
        entity.completedAt = null;
        entity.durationMillis = null;

        runRepository.persist(entity);
        executionService.execute(entity, sourcePackage, plan);
        return runResponseMapper.toResponse(entity, commandsFor(entity.id));
    }

    public RunResponse get(UUID runId) {
        VerificationRunEntity entity = runRepository.findByIdOptional(runId)
                .orElseThrow(() -> new NotFoundException("Run was not found: " + runId));
        return runResponseMapper.toResponse(entity, commandsFor(entity.id));
    }

    public List<RunResponse> listForSession(UUID sessionId) {
        if (!sessionRepository.findByIdOptional(sessionId).isPresent()) {
            throw new NotFoundException("Session was not found: " + sessionId);
        }
        return runRepository.list("sessionId", sessionId).stream()
                .sorted(Comparator.comparing((VerificationRunEntity run) -> run.id.toString()))
                .map(run -> runResponseMapper.toResponse(run, commandsFor(run.id)))
                .toList();
    }

    public RunSummaryResponse summary(UUID runId) {
        VerificationRunEntity entity = runRepository.findByIdOptional(runId)
                .orElseThrow(() -> new NotFoundException("Run was not found: " + runId));
        return runResponseMapper.toSummaryResponse(entity, commandsFor(entity.id));
    }

    private VerificationPlan selectPlan(SourcePackageEntity sourcePackage, String requestedPlanId) {
        ProjectDetectionSummary detection = projectDetectionService.detect(Path.of(sourcePackage.storageReference));
        if (!detection.supported() || detection.projects().isEmpty()) {
            throw new BadRequestException("No supported verification plan was found for the package.");
        }

        if (requestedPlanId != null) {
            VerificationPlan requested = verificationPlanService.findById(requestedPlanId)
                    .orElseThrow(() -> new BadRequestException("Requested verification plan is not available: " + requestedPlanId));
            boolean compatible = detection.projects().stream()
                    .anyMatch(project -> project.technology() == requested.technology());
            if (!compatible) {
                throw new BadRequestException("Requested verification plan is not compatible with the detected package.");
            }
            return requested;
        }

        DetectedProject selectedProject = detection.projects().get(0);
        if (selectedProject.selectedPlanId() == null) {
            throw new BadRequestException("Detected project does not have a selected verification plan.");
        }
        return verificationPlanService.findById(selectedProject.selectedPlanId())
                .orElseThrow(() -> new BadRequestException("Selected verification plan is not available: " + selectedProject.selectedPlanId()));
    }

    private List<VerificationCommandResultEntity> commandsFor(UUID runId) {
        return commandRepository.list("runId", runId).stream()
                .sorted(Comparator.comparing((VerificationCommandResultEntity command) -> command.startedAt).thenComparing(command -> command.commandLabel))
                .toList();
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
