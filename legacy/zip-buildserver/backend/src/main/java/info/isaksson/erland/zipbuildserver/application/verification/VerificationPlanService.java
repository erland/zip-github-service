package info.isaksson.erland.zipbuildserver.application.verification;

import info.isaksson.erland.zipbuildserver.domain.model.project.DetectedProject;
import info.isaksson.erland.zipbuildserver.domain.model.verification.VerificationPlan;
import info.isaksson.erland.zipbuildserver.domain.model.verification.VerificationPlanSelection;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class VerificationPlanService {
    private static final List<String> DEFAULT_PLAN_RESOURCES = List.of(
            "verification-plans/node-default.yml",
            "verification-plans/maven-default.yml",
            "verification-plans/multi-project-default.yml");

    private static final VerificationPlanParser DEFAULT_PARSER = new VerificationPlanParser();
    private static final VerificationPlanSelector DEFAULT_SELECTOR = new VerificationPlanSelector();

    private final List<VerificationPlan> plans;
    private final VerificationPlanSelector selector;

    public VerificationPlanService() {
        this(DEFAULT_PLAN_RESOURCES.stream()
                .map(VerificationPlanService::loadResource)
                .map(VerificationPlanService::parsePlan)
                .toList());
    }

    public VerificationPlanService(List<VerificationPlan> plans) {
        this(plans, DEFAULT_SELECTOR);
    }

    VerificationPlanService(List<VerificationPlan> plans, VerificationPlanSelector selector) {
        this.plans = plans.stream()
                .filter(VerificationPlan::enabled)
                .sorted(Comparator.comparing(VerificationPlan::id))
                .toList();
        this.selector = selector;
    }

    public List<VerificationPlan> listPlans() {
        return plans;
    }

    public Optional<VerificationPlan> findById(String planId) {
        return plans.stream()
                .filter(plan -> plan.id().equals(planId))
                .findFirst();
    }

    public VerificationPlanSelection selectPlan(DetectedProject project) {
        return selector.selectPlan(plans, project);
    }

    private static String loadResource(String resourceName) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream input = classLoader.getResourceAsStream(resourceName)) {
            if (input == null) {
                throw new VerificationPlanParseException("Missing verification plan resource: " + resourceName);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new VerificationPlanParseException("Unable to load verification plan resource: " + resourceName, exception);
        }
    }

    public static VerificationPlan parsePlan(String source) {
        return DEFAULT_PARSER.parse(source);
    }
}
