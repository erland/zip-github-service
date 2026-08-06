package info.isaksson.erland.zipbuildserver.application.packageupload;

import info.isaksson.erland.zipbuildserver.application.PackageValidationException;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@ApplicationScoped
public class ArchiveValidationService {
    private final long maxCompressedSizeBytes;
    private final long maxExtractedSizeBytes;
    private final int maxFiles;
    private final int maxPathLength;

    public ArchiveValidationService(
            @ConfigProperty(name = "zip-buildserver.packages.max-compressed-size-bytes", defaultValue = "104857600")
            long maxCompressedSizeBytes,
            @ConfigProperty(name = "zip-buildserver.packages.max-extracted-size-bytes", defaultValue = "524288000")
            long maxExtractedSizeBytes,
            @ConfigProperty(name = "zip-buildserver.packages.max-files", defaultValue = "20000")
            int maxFiles,
            @ConfigProperty(name = "zip-buildserver.packages.max-path-length", defaultValue = "1024")
            int maxPathLength) {
        this.maxCompressedSizeBytes = maxCompressedSizeBytes;
        this.maxExtractedSizeBytes = maxExtractedSizeBytes;
        this.maxFiles = maxFiles;
        this.maxPathLength = maxPathLength;
    }

    public ArchiveValidationResult validate(Path zipFile, long compressedSizeBytes) {
        if (compressedSizeBytes <= 0) {
            throw new PackageValidationException("Uploaded package is empty.");
        }
        if (compressedSizeBytes > maxCompressedSizeBytes) {
            throw new PackageValidationException("Uploaded package exceeds the configured compressed size limit.");
        }

        long extractedSize = 0;
        int fileCount = 0;
        Set<String> topLevelEntries = new TreeSet<>();

        try (InputStream input = new BufferedInputStream(java.nio.file.Files.newInputStream(zipFile));
             ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;
            boolean sawEntry = false;
            byte[] buffer = new byte[8192];

            while ((entry = zip.getNextEntry()) != null) {
                sawEntry = true;
                String name = entry.getName();
                validateEntryName(name);
                collectTopLevel(name, topLevelEntries);

                if (!entry.isDirectory()) {
                    fileCount++;
                    if (fileCount > maxFiles) {
                        throw new PackageValidationException("Uploaded package exceeds the configured file count limit.");
                    }

                    int read;
                    while ((read = zip.read(buffer)) != -1) {
                        extractedSize += read;
                        if (extractedSize > maxExtractedSizeBytes) {
                            throw new PackageValidationException("Uploaded package exceeds the configured extracted size limit.");
                        }
                    }
                }

                zip.closeEntry();
            }

            if (!sawEntry) {
                throw new PackageValidationException("Uploaded package is an empty zip archive.");
            }
        } catch (PackageValidationException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new PackageValidationException("Uploaded package is not a readable zip archive.");
        }

        return new ArchiveValidationResult(extractedSize, fileCount, String.join(",", topLevelEntries));
    }

    private void validateEntryName(String name) {
        if (name == null || name.isBlank()) {
            throw new PackageValidationException("Uploaded package contains an entry with an empty path.");
        }
        if (name.length() > maxPathLength) {
            throw new PackageValidationException("Uploaded package contains a path that exceeds the configured path length limit.");
        }
        if (name.indexOf('\0') >= 0) {
            throw new PackageValidationException("Uploaded package contains an entry with a NUL byte in the path.");
        }
        if (name.startsWith("/") || name.startsWith("\\")) {
            throw new PackageValidationException("Uploaded package contains an absolute path.");
        }

        try {
            Path normalized = Path.of(name).normalize();
            if (normalized.isAbsolute() || normalized.startsWith("..") || normalized.toString().equals("..")) {
                throw new PackageValidationException("Uploaded package contains a path traversal entry.");
            }
            for (Path part : normalized) {
                if (part.toString().equals("..")) {
                    throw new PackageValidationException("Uploaded package contains a path traversal entry.");
                }
            }
        } catch (InvalidPathException exception) {
            throw new PackageValidationException("Uploaded package contains an invalid path.");
        }
    }

    private void collectTopLevel(String name, Set<String> topLevelEntries) {
        String normalized = name.replace('\\', '/');
        int slash = normalized.indexOf('/');
        String topLevel = slash >= 0 ? normalized.substring(0, slash) : normalized;
        if (!topLevel.isBlank()) {
            topLevelEntries.add(topLevel);
        }
    }
}
