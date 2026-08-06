package info.isaksson.erland.zipbuildserver.application.run;

import info.isaksson.erland.zipbuildserver.application.project.ProjectDetectionService;
import info.isaksson.erland.zipbuildserver.domain.model.CheckStatus;
import info.isaksson.erland.zipbuildserver.domain.model.NetworkMode;
import info.isaksson.erland.zipbuildserver.domain.model.RunStatus;
import info.isaksson.erland.zipbuildserver.domain.model.SourcePackageStatus;
import info.isaksson.erland.zipbuildserver.domain.model.project.DetectedProject;
import info.isaksson.erland.zipbuildserver.domain.model.project.ProjectDetectionSummary;
import info.isaksson.erland.zipbuildserver.domain.model.project.ProjectTechnology;
import info.isaksson.erland.zipbuildserver.domain.model.verification.VerificationCommand;
import info.isaksson.erland.zipbuildserver.domain.model.verification.VerificationPlan;
import info.isaksson.erland.zipbuildserver.infrastructure.persistence.entity.SourcePackageEntity;
import info.isaksson.erland.zipbuildserver.infrastructure.persistence.entity.VerificationRunEntity;
import info.isaksson.erland.zipbuildserver.infrastructure.persistence.repository.VerificationRunRepository;
import info.isaksson.erland.zipbuildserver.worker.CommandExecutionRequest;
import info.isaksson.erland.zipbuildserver.worker.CommandExecutionResult;
import info.isaksson.erland.zipbuildserver.worker.CommandExecutor;
import info.isaksson.erland.zipbuildserver.worker.docker.DockerWorkspaceService;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RunExecutorTest {
    private final RecordingRunRepository runRepository = new RecordingRunRepository();
    private final RecordingProjectDetectionService projectDetectionService = new RecordingProjectDetectionService();
    private final RecordingCommandExecutor commandExecutor = new RecordingCommandExecutor();
    private final RecordingWorkspaceService workspaceService = new RecordingWorkspaceService();
    private final RecordingCommandResultPersister commandResultPersister = new RecordingCommandResultPersister();
    private final RunExecutor runExecutor = new RunExecutor(
            runRepository,
            projectDetectionService,
            commandExecutor,
            workspaceService,
            commandResultPersister,
            new RunStatusCalculator());

    @Test
    void executesCommandsAndCompletesPassedRun() {
        VerificationRunEntity run = run();
        SourcePackageEntity sourcePackage = sourcePackage("/tmp/package.zip");
        VerificationPlan plan = plan(List.of(command("build", "${project.path}", 60)));
        projectDetectionService.summary = new ProjectDetectionSummary(
                List.of(project("service", ProjectTechnology.MAVEN)),
                true,
                "Detected one project.");
        commandExecutor.results.add(CommandExecutionResult.passed("build", Duration.ofMillis(25), "ok", ""));

        runExecutor.execute(run, sourcePackage, plan);

        assertEquals(RunStatus.PASSED, run.status);
        assertEquals("Verification passed. 1 approved command(s) completed.", run.summary);
        assertEquals(Path.of("/tmp/package.zip"), workspaceService.createdFrom);
        assertEquals(workspaceService.workspace, workspaceService.cleanedWorkspace);
        assertEquals(1, runRepository.persistedRuns.size());
        assertEquals(run, runRepository.persistedRuns.get(0));
        assertEquals(1, commandExecutor.requests.size());
        CommandExecutionRequest request = commandExecutor.requests.get(0);
        assertEquals("build", request.commandLabel());
        assertEquals(workspaceService.workspace, request.workspaceRoot());
        assertEquals("service", request.workingDirectory());
        assertEquals("mvn test", request.commandDisplay());
        assertEquals(Duration.ofSeconds(60), request.timeout());
        assertEquals(1, commandResultPersister.persistedResults.size());
        assertEquals("service", commandResultPersister.persistedResults.get(0).workingDirectory);
        assertNotNull(run.startedAt);
        assertNotNull(run.completedAt);
        assertNotNull(run.durationMillis);
    }

    @Test
    void fallsBackToFirstDetectedProjectWhenPlanTechnologyDoesNotMatch() {
        VerificationRunEntity run = run();
        VerificationPlan plan = plan(List.of(command("test", "${project.path}/module", 30)));
        projectDetectionService.summary = new ProjectDetectionSummary(
                List.of(project("frontend", ProjectTechnology.NODE)),
                true,
                "Detected one project.");
        commandExecutor.results.add(CommandExecutionResult.passed("test", Duration.ofMillis(10), "", ""));

        runExecutor.execute(run, sourcePackage("/tmp/package.zip"), plan);

        assertEquals("frontend/module", commandExecutor.requests.get(0).workingDirectory());
        assertEquals(RunStatus.PASSED, run.status);
    }

    @Test
    void usesFallbackProjectWhenDetectionReturnsNoProjects() {
        VerificationRunEntity run = run();
        VerificationPlan plan = plan(List.of(command("test", "${project.path}", 30)));
        projectDetectionService.summary = ProjectDetectionSummary.unsupported("No projects.");
        commandExecutor.results.add(CommandExecutionResult.passed("test", Duration.ofMillis(10), "", ""));

        runExecutor.execute(run, sourcePackage("/tmp/package.zip"), plan);

        assertEquals(".", commandExecutor.requests.get(0).workingDirectory());
        assertEquals(RunStatus.PASSED, run.status);
    }

    @Test
    void persistsSkippedCommandsAfterFailureAndCompletesFailedRun() {
        VerificationRunEntity run = run();
        VerificationCommand first = command("unit", "${project.path}", 60);
        VerificationCommand second = command("integration", "${project.path}/integration", 60);
        VerificationPlan plan = plan(List.of(first, second));
        projectDetectionService.summary = new ProjectDetectionSummary(
                List.of(project("backend", ProjectTechnology.MAVEN)),
                true,
                "Detected one project.");
        commandExecutor.results.add(CommandExecutionResult.failed(
                "unit",
                1,
                Duration.ofMillis(50),
                "",
                "failed",
                "Tests failed."));

        runExecutor.execute(run, sourcePackage("/tmp/package.zip"), plan);

        assertEquals(RunStatus.FAILED, run.status);
        assertEquals("Verification failed. Review command-level failure details.", run.summary);
        assertEquals(1, commandExecutor.requests.size());
        assertEquals(1, commandResultPersister.persistedResults.size());
        assertEquals(1, commandResultPersister.skippedResults.size());
        SkippedResult skipped = commandResultPersister.skippedResults.get(0);
        assertEquals("integration", skipped.command.label());
        assertEquals("backend/integration", skipped.workingDirectory);
        assertEquals("Skipped because an earlier command failed.", skipped.reason);
        assertEquals(workspaceService.workspace, workspaceService.cleanedWorkspace);
    }

    private static VerificationRunEntity run() {
        VerificationRunEntity run = new VerificationRunEntity();
        run.id = UUID.randomUUID();
        run.sessionId = UUID.randomUUID();
        run.sourcePackageId = UUID.randomUUID();
        run.status = RunStatus.QUEUED;
        run.planId = "maven-default";
        run.networkMode = NetworkMode.NONE;
        return run;
    }

    private static SourcePackageEntity sourcePackage(String storageReference) {
        SourcePackageEntity sourcePackage = new SourcePackageEntity();
        sourcePackage.id = UUID.randomUUID();
        sourcePackage.sessionId = UUID.randomUUID();
        sourcePackage.checksumSha256 = "0".repeat(64);
        sourcePackage.compressedSizeBytes = 123L;
        sourcePackage.storageReference = storageReference;
        sourcePackage.status = SourcePackageStatus.ACCEPTED;
        sourcePackage.createdAt = OffsetDateTime.now();
        return sourcePackage;
    }

    private static VerificationPlan plan(List<VerificationCommand> commands) {
        return new VerificationPlan(
                "maven-default",
                "Maven default",
                ProjectTechnology.MAVEN,
                List.of("pom.xml"),
                commands,
                NetworkMode.NONE,
                true,
                "Selected Maven plan.");
    }

    private static VerificationCommand command(String label, String workingDirectory, int timeoutSeconds) {
        return new VerificationCommand(label, workingDirectory, "mvn test", timeoutSeconds, false);
    }

    private static DetectedProject project(String path, ProjectTechnology technology) {
        return new DetectedProject(path, technology, List.of(), null, "Detected project.");
    }

    private static final class RecordingRunRepository extends VerificationRunRepository {
        private final List<VerificationRunEntity> persistedRuns = new ArrayList<>();

        @Override
        public void persist(VerificationRunEntity entity) {
            persistedRuns.add(entity);
        }
    }

    private static final class RecordingProjectDetectionService extends ProjectDetectionService {
        private ProjectDetectionSummary summary = ProjectDetectionSummary.unsupported("No projects.");

        private RecordingProjectDetectionService() {
            super(null);
        }

        @Override
        public ProjectDetectionSummary detect(Path zipPath) {
            return summary;
        }
    }

    private static final class RecordingCommandExecutor implements CommandExecutor {
        private final List<CommandExecutionRequest> requests = new ArrayList<>();
        private final List<CommandExecutionResult> results = new ArrayList<>();

        @Override
        public CommandExecutionResult execute(CommandExecutionRequest request) {
            requests.add(request);
            return results.remove(0);
        }
    }

    private static final class RecordingWorkspaceService extends DockerWorkspaceService {
        private final Path workspace = Path.of("/tmp/workspace");
        private Path createdFrom;
        private Path cleanedWorkspace;

        private RecordingWorkspaceService() {
            super("/tmp/workspaces");
        }

        @Override
        public Path createWorkspace(Path zipPackagePath) {
            createdFrom = zipPackagePath;
            return workspace;
        }

        @Override
        public void cleanup(Path workspace) {
            cleanedWorkspace = workspace;
        }
    }

    private static final class RecordingCommandResultPersister extends CommandResultPersister {
        private final List<PersistedResult> persistedResults = new ArrayList<>();
        private final List<SkippedResult> skippedResults = new ArrayList<>();

        private RecordingCommandResultPersister() {
            super(null, null, null, null);
        }

        @Override
        public void persistResult(UUID runId, VerificationCommand command, String workingDirectory, CommandExecutionResult result) {
            persistedResults.add(new PersistedResult(runId, command, workingDirectory, result));
        }

        @Override
        public void persistSkipped(UUID runId, VerificationCommand command, String workingDirectory, String reason) {
            skippedResults.add(new SkippedResult(runId, command, workingDirectory, reason));
        }
    }

    private record PersistedResult(
            UUID runId,
            VerificationCommand command,
            String workingDirectory,
            CommandExecutionResult result) {
    }

    private record SkippedResult(
            UUID runId,
            VerificationCommand command,
            String workingDirectory,
            String reason) {
    }
}
