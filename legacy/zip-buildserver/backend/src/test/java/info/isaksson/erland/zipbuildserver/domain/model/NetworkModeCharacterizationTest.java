package info.isaksson.erland.zipbuildserver.domain.model;

import info.isaksson.erland.zipbuildserver.application.verification.VerificationPlanService;
import info.isaksson.erland.zipbuildserver.domain.model.project.ProjectTechnology;
import info.isaksson.erland.zipbuildserver.domain.model.verification.VerificationPlan;
import info.isaksson.erland.zipbuildserver.infrastructure.persistence.entity.VerificationRunEntity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class NetworkModeCharacterizationTest {
    @Test
    void networkModeUsesCanonicalNamesAndOrder() {
        String[] modeNames = Arrays.stream(NetworkMode.values())
                .map(Enum::name)
                .toArray(String[]::new);

        assertArrayEquals(new String[]{"NONE", "DEPENDENCY", "FULL"}, modeNames);
    }

    @Test
    void verificationPlanParserAcceptsEveryCurrentNetworkMode() {
        for (NetworkMode mode : NetworkMode.values()) {
            VerificationPlan plan = VerificationPlanService.parsePlan("""
                    id: mode-%s
                    name: Mode %s
                    technology: NODE
                    enabled: true
                    networkMode: %s
                    selectionReason: Characterization test.
                    indicators:
                      - package.json
                    commands:
                      - label: Test
                        workingDirectory: ${project.path}
                        commandDisplay: npm test
                        timeoutSeconds: 120
                        optional: false
                    """.formatted(mode.name().toLowerCase(), mode.name(), mode.name()));

            assertEquals(ProjectTechnology.NODE, plan.technology());
            assertEquals(mode, plan.networkMode());
        }
    }

    @Test
    void verificationRunEntityPersistsNetworkModeAsStringEnumName() throws NoSuchFieldException {
        Field field = VerificationRunEntity.class.getField("networkMode");
        Enumerated enumerated = field.getAnnotation(Enumerated.class);

        assertEquals(EnumType.STRING, enumerated.value());
    }
}
