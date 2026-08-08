package info.isaksson.erland.zipgithub.staging;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.security.MessageDigest;
import java.util.Optional;

/** Validates the low-privilege deployment credential used only to create staging uploads. */
@ApplicationScoped
public class StagingUploadCredential {
    private final byte[] configuredDigest;

    @Inject
    public StagingUploadCredential(@ConfigProperty(name = "zipgithub.staging.upload-credential") Optional<String> configured) {
        this.configuredDigest = configured.map(String::trim).filter(value -> !value.isEmpty())
                .map(StagingSecretCodec::digestBytes).orElse(null);
    }

    StagingUploadCredential(String configured) {
        this(Optional.ofNullable(configured));
    }

    public boolean accepts(String presented) {
        if (configuredDigest == null || presented == null || presented.isBlank()) return false;
        return MessageDigest.isEqual(configuredDigest, StagingSecretCodec.digestBytes(presented.trim()));
    }


}
