package info.isaksson.erland.zipbuildserver.application.run;

import info.isaksson.erland.zipbuildserver.application.project.ProjectDetectionService;
import info.isaksson.erland.zipbuildserver.domain.model.CheckStatus;
import info.isaksson.erland.zipbuildserver.domain.model.RunStatus;
import info.isaksson.erland.zipbuildserver.domain.model.project.DetectedProject;
import info.isaksson.erland.zipbuildserver.domain.model.project.ProjectDetectionSummary;
import info.isaksson.erland.zipbuildserver.domain.model.verification.VerificationCommand;
import info.isaksson.erland.zipbuildserver.domain.model.verification.VerificationPlan;
import info.isaksson.erland.zipbuildserver.infrastructure.persistence.entity.SourcePackageEntity;
import info.isaksson.erland.zipbuildserver.infrastructure.persistence.entity.VerificationRunEntity;
import info.isaksson.erland.zipbuildserver.infrastructure.persistence.repository.VerificationRunRepository;
import info.isaksson.erland.zipbuildserver.worker.CommandExecutionRequest;
import info.isaksson.erland.zipbuildserver.worker.CommandExecutionResult;
import info.isaksson.erland.zipbuildserver.worker.CommandExecutor;
import info.isaksson.erland.zipbuildserver.worker.docker.DockerWorkspaceService;
import jakarta.enterprise.context.ApplicationScoped;

import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

@ApplicationScoped
public class RunExecutor {
    private final VerificationRunRepository runRepository;
    private final ProjectDetectionService projectDetectionService;
    private final CommandExecutor commandExecutor;
    private final DockerWorkspaceService workspaceService;
    private final CommandResultPersister commandResultPersister;
    private final RunStatusCalculator runStatusCalculator;

    public RunExecutor(
            VerificationRunRepository runRepository,
            ProjectDetectionService projectDetectionService,
            CommandExecutor commandExecutor,
            DockerWorkspaceService workspaceService,
            CommandResultPersister commandResultPersister,
            RunStatusCalculator runStatusCalculator) {
        this.runRepository = runRepository;
        this.projectDetectionService = projectDetectionService;
        this.commandExecutor = commandExecutor;
        this.workspaceService = workspaceService;
        this.commandResultPersister = commandResultPersister;
        this.runStatusCalculator = runStatusCalculator;
    }

    public void execute(VerificationRunEntity run, SourcePackageEntity sourcePackage, VerificationPlan plan) {
        OffsetDateTime started = OffsetDateTime.now();
        run.status = RunStatus.RUNNING;
        run.startedAt = started;
        run.summary = "Verification is running.";
        runRepository.persist(run);

        Path packagePath = Path.of(sourcePackage.storageReference);
        Path workspaceRoot = workspaceService.createWorkspace(packagePath);
        DetectedProject project = selectDetectedProject(packagePath, plan);

        boolean failed = false;
        boolean timedOut = false;
        try {
            for (VerificationCommand command : plan.commands()) {
                String workingDirectory = resolveWorkingDirectory(command.workingDirectory(), project);
                if (failed || timedOut) {
                    commandResultPersister.persistSkipped(run.id, command, workingDirectory, "Skipped because an earlier command failed.");
                    continue;
                }

                CommandExecutionRequest request = new CommandExecutionRequest(
                        command.label(),
                        workspaceRoot,
                        workingDirectory,
                        command.commandDisplay(),
                        Duration.ofSeconds(command.timeoutSeconds()));

                CommandExecutionResult result = commandExecutor.execute(request);
                commandResultPersister.persistResult(run.id, command, workingDirectory, result);

                if (result.status() == CheckStatus.TIMED_OUT) {
                    timedOut = true;
                } else if (runStatusCalculator.shouldSkipLaterCommands(result.status())) {
                    failed = true;
                }
            }
        } finally {
            workspaceService.cleanup(workspaceRoot);
        }

        OffsetDateTime completed = OffsetDateTime.now();
        run.completedAt = completed;
        run.durationMillis = Duration.between(started, completed).toMillis();
        run.status = runStatusCalculator.finalStatus(failed, timedOut);
        run.summary = runStatusCalculator.summaryFor(run.status, plan.commands().size());
    }

    private DetectedProject selectDetectedProject(Path packagePath, VerificationPlan plan) {
        ProjectDetectionSummary detection = projectDetectionService.detect(packagePath);
        return detection.projects().stream()
                .filter(project -> project.technology() == plan.technology())
                .findFirst()
                .orElseGet(() -> detection.projects().isEmpty()
                        ? new DetectedProject(".", plan.technology(), List.of(), plan.id(), "Fallback project for fake execution.")
                        : detection.projects().get(0));
    }

    private String resolveWorkingDirectory(String workingDirectory, DetectedProject project) {
        String projectPath = project.path() == null || project.path().isBlank() ? "." : project.path();
        return workingDirectory.replace("${project.path}", projectPath);
    }
}
