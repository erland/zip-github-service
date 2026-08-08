package info.isaksson.erland.zipgithub.staging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StagingUploadCredentialTest {
    @Test
    void missingConfigurationIsDenyAll() {
        var credential = new StagingUploadCredential((String) null);
        assertFalse(credential.accepts(null));
        assertFalse(credential.accepts("anything"));
    }

    @Test
    void acceptsOnlyExactConfiguredCredential() {
        var credential = new StagingUploadCredential("zg-staging-secret");
        assertTrue(credential.accepts("zg-staging-secret"));
        assertFalse(credential.accepts("zg-staging-secret-x"));
        assertFalse(credential.accepts(""));
    }

    @Test
    void rotationImmediatelyRejectsOldCredentialWithoutDataMigration() {
        var oldDeployment = new StagingUploadCredential("old-shortcut-secret");
        var rotatedDeployment = new StagingUploadCredential("new-shortcut-secret");
        assertTrue(oldDeployment.accepts("old-shortcut-secret"));
        assertFalse(rotatedDeployment.accepts("old-shortcut-secret"));
        assertTrue(rotatedDeployment.accepts("new-shortcut-secret"));
    }
}

