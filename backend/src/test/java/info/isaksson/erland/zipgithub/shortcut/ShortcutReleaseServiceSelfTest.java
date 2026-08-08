package info.isaksson.erland.zipgithub.shortcut;

import java.nio.file.Files;

public final class ShortcutReleaseServiceSelfTest {
    public static void main(String[] args) throws Exception {
        if (ShortcutReleaseArtifact.available(null).isPresent()) throw new AssertionError("missing release must be unavailable");

        var dir = Files.createTempDirectory("shortcut-release-test");
        var file = dir.resolve("zip-github.shortcut");
        Files.write(file, new byte[]{1, 2, 3, 4});
        var available = ShortcutReleaseArtifact.available(file.toString());
        if (available.isEmpty() || Files.size(available.get()) != 4L) throw new AssertionError("published release should be available");
        if (ShortcutReleaseArtifact.sha256(file).length() != 64) throw new AssertionError("sha256 missing");
        System.out.println("ShortcutReleaseServiceSelfTest OK");
    }
}
