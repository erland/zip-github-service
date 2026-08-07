package info.isaksson.erland.zipgithub.workspace;

import info.isaksson.erland.zipgithub.plan.ImmutableImportPlan;
import info.isaksson.erland.zipgithub.plan.ImmutableImportPlanEntry;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ImportWorkspaceServiceSelfTest {
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("workspace-self-test-");
        try {
            Path remote = root.resolve("remote.git");
            run(root, "git", "init", "--bare", remote.toString());
            Path seed = root.resolve("seed");
            Files.createDirectories(seed);
            run(seed, "git", "init");
            run(seed, "git", "config", "user.name", "Test");
            run(seed, "git", "config", "user.email", "test@example.invalid");
            Files.writeString(seed.resolve("README.md"), "old\n");
            run(seed, "git", "add", ".");
            run(seed, "git", "commit", "-m", "base");
            String base = run(seed, "git", "rev-parse", "HEAD").trim();
            run(seed, "git", "remote", "add", "origin", remote.toString());
            run(seed, "git", "push", "origin", "HEAD:main");

            Path zip = root.resolve("source.zip");
            try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(zip))) {
                write(output, "wrapper/README.md", "new\n");
                write(output, "wrapper/src/App.txt", "app\n");
                write(output, "wrapper/ignored.txt", "not in plan\n");
            }
            UUID importId = UUID.randomUUID();
            List<ImmutableImportPlanEntry> entries = List.of(
                    entry("README.md", "MODIFIED", "new\n"),
                    entry("src/App.txt", "ADDED", "app\n"),
                    new ImmutableImportPlanEntry("ignored.txt", "UNCHANGED", "UNCHANGED", "NONE", null, null,
                            12L, sha("not in plan\n"), 12L, sha("not in plan\n"), true));
            ImmutableImportPlan plan = new ImmutableImportPlan(UUID.randomUUID(), importId, UUID.randomUUID(),
                    "a".repeat(64), base, "mvp-1", "b".repeat(64), "READY", true, entries, Instant.now());
            ImportWorkspaceService service = new ImportWorkspaceService(id -> "", root.resolve("workspaces"),
                    Clock.fixed(Instant.parse("2026-08-06T20:00:00Z"), ZoneOffset.UTC));
            AppliedImportWorkspace result = service.prepare(importId, "local/test", remote.toUri(), "", zip, "wrapper", plan);
            if (!result.appliedPaths().equals(List.of("README.md", "src/App.txt"))) throw new AssertionError(result.appliedPaths());
            if (!Files.readString(result.workspacePath().resolve("README.md")).equals("new\n")) throw new AssertionError();
            if (!Files.exists(result.workspacePath().resolve("src/App.txt"))) throw new AssertionError();
            if (Files.exists(result.workspacePath().resolve("ignored.txt"))) throw new AssertionError("unplanned file applied");
            if (!run(result.workspacePath(), "git", "remote").isBlank()) throw new AssertionError("credentials remote retained");
            service.delete(result);
            if (Files.exists(result.workspacePath())) throw new AssertionError("workspace not deleted");
            System.out.println("ImportWorkspaceServiceSelfTest passed");
        } finally {
            delete(root);
        }
    }

    private static ImmutableImportPlanEntry entry(String path, String status, String content) throws Exception {
        return new ImmutableImportPlanEntry(path, status, status, "NONE", null, null,
                (long) content.getBytes(StandardCharsets.UTF_8).length, sha(content), null, null, true);
    }

    private static String sha(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    private static void write(ZipOutputStream output, String path, String content) throws IOException {
        output.putNextEntry(new ZipEntry(path));
        output.write(content.getBytes(StandardCharsets.UTF_8));
        output.closeEntry();
    }

    private static String run(Path dir, String... command) throws Exception {
        Process process = new ProcessBuilder(command).directory(dir.toFile()).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (process.waitFor() != 0) throw new AssertionError(output);
        return output;
    }

    private static void delete(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }
}
