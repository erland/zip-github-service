package info.isaksson.erland.zipgithub.delivery;

import info.isaksson.erland.zipgithub.workspace.AppliedImportWorkspace;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

public final class GitDeliveryServiceSelfTest {
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("delivery-selftest-");
        Path remote = root.resolve("remote.git");
        Path seed = root.resolve("seed");
        Path workspace = root.resolve("workspace");
        run(root, "git", "init", "--bare", remote.toString());
        Files.createDirectories(seed); run(seed, "git", "init", "--initial-branch=main");
        run(seed, "git", "config", "user.name", "Test"); run(seed, "git", "config", "user.email", "test@example.com");
        Files.writeString(seed.resolve("README.md"), "before\n"); run(seed, "git", "add", "."); run(seed, "git", "commit", "-m", "base");
        run(seed, "git", "remote", "add", "origin", remote.toUri().toString()); run(seed, "git", "push", "origin", "main");
        String base = run(seed, "git", "rev-parse", "HEAD").trim();
        run(root, "git", "clone", remote.toUri().toString(), workspace.toString());
        run(workspace, "git", "checkout", "main"); Files.writeString(workspace.resolve("README.md"), "after\n");
        UUID importId = UUID.randomUUID();
        AppliedImportWorkspace applied = new AppliedImportWorkspace(importId, "owner/repo", base, "a".repeat(64), workspace,
                List.of("README.md"), Instant.now());
        GitDeliveryService service = new GitDeliveryService(id -> "", "zip-github", "zip@example.com",
                Clock.fixed(Instant.parse("2026-08-06T20:00:00Z"), ZoneOffset.UTC));
        GitDeliveryResult result = service.deliver("main", applied, remote.toUri(), "");
        if (!result.branchName().equals("zip-github/import-" + importId)) throw new AssertionError();
        String pushed = run(root, "git", "--git-dir=" + remote, "rev-parse", "refs/heads/" + result.branchName()).trim();
        if (!pushed.equals(result.commitSha())) throw new AssertionError();
        String parent = run(root, "git", "--git-dir=" + remote, "rev-parse", pushed + "^1").trim();
        if (!parent.equals(base)) throw new AssertionError();
        System.out.println("GitDeliveryServiceSelfTest passed");
    }
    private static String run(Path dir, String... command) throws Exception {
        Process p = new ProcessBuilder(command).directory(dir.toFile()).redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes());
        if (p.waitFor()!=0) throw new IllegalStateException(out); return out;
    }
}
