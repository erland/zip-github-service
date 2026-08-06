package info.isaksson.erland.zipgithub.archive;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ArchiveInventoryServiceSelfTest {
    public static void main(String[] args) throws Exception {
        if (!"project".equals(ArchiveNormalization.detectSingleWrapper(
                List.of("project/README.md", "project/src/App.java", "__MACOSX/._README.md")))) {
            throw new AssertionError("Expected wrapper detection");
        }
        if (ArchiveNormalization.detectSingleWrapper(List.of("README.md", "src/App.java")) != null) {
            throw new AssertionError("Root files must prevent wrapper stripping");
        }
        System.out.println("Archive inventory normalization self-test passed");
    }
}
