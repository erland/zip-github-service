package info.isaksson.erland.zipbuildserver.application.run;

import info.isaksson.erland.zipbuildserver.domain.model.verification.VerificationPlan;
import info.isaksson.erland.zipbuildserver.infrastructure.persistence.entity.SourcePackageEntity;
import info.isaksson.erland.zipbuildserver.infrastructure.persistence.entity.VerificationRunEntity;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class VerificationExecutionService {
    private final RunExecutor runExecutor;

    public VerificationExecutionService(RunExecutor runExecutor) {
        this.runExecutor = runExecutor;
    }

    public void execute(VerificationRunEntity run, SourcePackageEntity sourcePackage, VerificationPlan plan) {
        runExecutor.execute(run, sourcePackage, plan);
    }
}
