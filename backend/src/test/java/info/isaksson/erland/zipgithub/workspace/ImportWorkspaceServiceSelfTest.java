package info.isaksson.erland.zipgithub.workspace;

import info.isaksson.erland.zipgithub.plan.ImmutableImportPlan;
import info.isaksson.erland.zipgithub.plan.ImmutableImportPlanEntry;
import info.isaksson.erland.zipgithub.selection.ApprovedSelection;
import info.isaksson.erland.zipgithub.selection.ApprovedSelectionOverride;

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
            Files.writeString(seed.resolve("keep.txt"), "keep old\n");
            Files.writeString(seed.resolve("obsolete.txt"), "remove me\n");
            Files.createDirectories(seed.resolve(".github/workflows"));
            Files.writeString(seed.resolve(".github/workflows/ci.yml"), "name: old\n");
            run(seed, "git", "add", ".");
            run(seed, "git", "commit", "-m", "base");
            String base = run(seed, "git", "rev-parse", "HEAD").trim();
            run(seed, "git", "remote", "add", "origin", remote.toString());
            run(seed, "git", "push", "origin", "HEAD:main");

            Path zip = root.resolve("source.zip");
            try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(zip))) {
                write(output, "wrapper/README.md", "new\n");
                write(output, "wrapper/keep.txt", "keep new but excluded\n");
                write(output, "wrapper/src/App.txt", "app but excluded\n");
                write(output, "wrapper/.github/workflows/ci.yml", "name: new\n");
                write(output, "wrapper/.git/config", "MALICIOUS_GIT_CONFIG\n");
                write(output, "wrapper/ignored.txt", "not in plan\n");
            }
            UUID importId = UUID.randomUUID();
            List<ImmutableImportPlanEntry> entries = List.of(
                    entry("README.md", "MODIFIED", "new\n"),
                    entry("keep.txt", "MODIFIED", "keep new but excluded\n"),
                    entry("src/App.txt", "ADDED", "app but excluded\n"),
                    new ImmutableImportPlanEntry(".github/workflows/ci.yml", "BLOCKED", "MODIFIED", "BLOCKING", "OVERRIDABLE_BLOCKED",
                            "GITHUB_WORKFLOW_PROTECTED", "Workflow changes require explicit approval.",
                            (long) "name: new\n".getBytes(StandardCharsets.UTF_8).length, sha("name: new\n"),
                            (long) "name: old\n".getBytes(StandardCharsets.UTF_8).length, sha("name: old\n"), true),
                    new ImmutableImportPlanEntry(".git/config", "BLOCKED", "MODIFIED", "BLOCKING", "HARD_BLOCKED",
                            "GIT_METADATA_PROTECTED", "Git metadata can never be imported.",
                            (long) "MALICIOUS_GIT_CONFIG\n".getBytes(StandardCharsets.UTF_8).length, sha("MALICIOUS_GIT_CONFIG\n"),
                            null, null, true),
                    new ImmutableImportPlanEntry("obsolete.txt", "BLOCKED", "WOULD_DELETE", "BLOCKING", "OVERRIDABLE_BLOCKED",
                            "DELETION_REQUIRES_APPROVAL", "Deletion requires explicit approval.", null, null,
                            (long) "remove me\n".getBytes(StandardCharsets.UTF_8).length, sha("remove me\n"), true),
                    new ImmutableImportPlanEntry("ignored.txt", "UNCHANGED", "UNCHANGED", "NONE", "NONE", null, null,
                            12L, sha("not in plan\n"), 12L, sha("not in plan\n"), true));
            ImmutableImportPlan plan = new ImmutableImportPlan(UUID.randomUUID(), importId, UUID.randomUUID(),
                    "a".repeat(64), base, "mvp-1", "b".repeat(64), "READY", true, entries, Instant.now());
            ApprovedSelection selection = new ApprovedSelection(UUID.randomUUID(), importId, plan.id(), plan.ownerUserId(),
                    plan.planDigestSha256(), plan.baseCommitSha(), "selection-1", "c".repeat(64),
                    List.of(".github/workflows/ci.yml", "README.md", "obsolete.txt"),
                    List.of(".git/config", "ignored.txt", "keep.txt", "src/App.txt"),
                    List.of(
                            new ApprovedSelectionOverride(".github/workflows/ci.yml", "OVERRIDABLE_BLOCKED",
                                    "GITHUB_WORKFLOW_PROTECTED", "Explicit workflow approval"),
                            new ApprovedSelectionOverride("obsolete.txt", "OVERRIDABLE_BLOCKED",
                                    "DELETION_REQUIRES_APPROVAL", "Explicit deletion approval")
                    ), Instant.now());
            ImportWorkspaceService service = new ImportWorkspaceService(id -> "", root.resolve("workspaces"),
                    Clock.fixed(Instant.parse("2026-08-06T20:00:00Z"), ZoneOffset.UTC));
            AppliedImportWorkspace result = service.prepare(importId, "local/test", remote.toUri(), "", zip, "wrapper", plan, selection);
            if (!result.appliedPaths().equals(List.of(".github/workflows/ci.yml", "README.md", "obsolete.txt"))) throw new AssertionError(result.appliedPaths());
            if (!Files.readString(result.workspacePath().resolve("README.md")).equals("new\n")) throw new AssertionError();
            if (!Files.readString(result.workspacePath().resolve(".github/workflows/ci.yml")).equals("name: new\n")) throw new AssertionError();
            if (!Files.readString(result.workspacePath().resolve("keep.txt")).equals("keep old\n")) throw new AssertionError("excluded modification was applied");
            if (Files.exists(result.workspacePath().resolve("src/App.txt"))) throw new AssertionError("excluded addition was applied");
            if (Files.exists(result.workspacePath().resolve("obsolete.txt"))) throw new AssertionError("selected deletion not applied");
            if (Files.exists(result.workspacePath().resolve("ignored.txt"))) throw new AssertionError("unplanned file applied");
            if (Files.readString(result.workspacePath().resolve(".git/config")).contains("MALICIOUS_GIT_CONFIG")) {
                throw new AssertionError("hard-blocked .git content reached workspace");
            }
            List<String> diffPaths = run(result.workspacePath(), "git", "diff", "--name-only", "--no-renames")
                    .lines().filter(line -> !line.isBlank()).sorted().toList();
            if (!diffPaths.equals(List.of(".github/workflows/ci.yml", "README.md", "obsolete.txt"))) {
                throw new AssertionError("workspace diff did not exactly match selection: " + diffPaths);
            }
            if (!run(result.workspacePath(), "git", "remote").isBlank()) throw new AssertionError("credentials remote retained");
            service.delete(result);
            if (Files.exists(result.workspacePath())) throw new AssertionError("workspace not deleted");
            System.out.println("ImportWorkspaceServiceSelfTest passed");
        } finally {
            delete(root);
        }
    }

    private static ImmutableImportPlanEntry entry(String path, String status, String content) throws Exception {
        return new ImmutableImportPlanEntry(path, status, status, "NONE", "NONE", null, null,
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
