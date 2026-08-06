package info.isaksson.erland.zipbuildserver.storage;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@ApplicationScoped
public class PackageStorageService {
    private final Path packageDirectory;

    public PackageStorageService(
            @ConfigProperty(name = "zip-buildserver.storage.packages-dir", defaultValue = "target/zip-buildserver/packages")
            String packageDirectory) {
        this.packageDirectory = Path.of(packageDirectory);
    }

    public StoredPackage store(Path uploadedFile, UUID packageId) {
        try {
            Files.createDirectories(packageDirectory);
            Path target = packageDirectory.resolve(packageId + ".zip");
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            try (InputStream input = Files.newInputStream(uploadedFile);
                 DigestInputStream digestInput = new DigestInputStream(input, digest)) {
                Files.copy(digestInput, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            long size = Files.size(target);
            String checksum = HexFormat.of().formatHex(digest.digest());
            return new StoredPackage(target.toString(), size, checksum);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not store uploaded package.", exception);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }

    public record StoredPackage(String storageReference, long compressedSizeBytes, String checksumSha256) {
    }
}
