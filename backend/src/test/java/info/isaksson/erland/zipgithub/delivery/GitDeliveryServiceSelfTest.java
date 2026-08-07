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
        GitDeliveryService service = new GitDeliveryService(id -> "",
                Clock.fixed(Instant.parse("2026-08-06T20:00:00Z"), ZoneOffset.UTC));
        GitCommitIdentity identity = new GitCommitIdentity("Other Author", "other@example.com", "Approver", "approver@example.com");
        String workBranch = "zip-github/work-" + UUID.randomUUID();
        GitDeliveryResult result = service.deliver("main", workBranch, applied, remote.toUri(), "", identity);
        if (!result.branchName().equals(workBranch)) throw new AssertionError();
        String pushed = run(root, "git", "--git-dir=" + remote, "rev-parse", "refs/heads/" + result.branchName()).trim();
        if (!pushed.equals(result.commitSha())) throw new AssertionError();
        String parent = run(root, "git", "--git-dir=" + remote, "rev-parse", pushed + "^1").trim();
        if (!parent.equals(base)) throw new AssertionError();
        String author = run(root, "git", "--git-dir=" + remote, "show", "-s", "--format=%an <%ae>", pushed).trim();
        String committer = run(root, "git", "--git-dir=" + remote, "show", "-s", "--format=%cn <%ce>", pushed).trim();
        if (!author.equals("Other Author <other@example.com>")) throw new AssertionError(author);
        if (!committer.equals("Approver <approver@example.com>")) throw new AssertionError(committer);

        Path workspace2 = root.resolve("workspace2");
        run(root, "git", "clone", remote.toUri().toString(), workspace2.toString());
        run(workspace2, "git", "checkout", workBranch);
        Files.writeString(workspace2.resolve("README.md"), "after second\n");
        UUID importId2 = UUID.randomUUID();
        AppliedImportWorkspace applied2 = new AppliedImportWorkspace(importId2, "owner/repo", pushed, "b".repeat(64), workspace2,
                List.of("README.md"), Instant.now());
        GitDeliveryResult second = service.deliver(workBranch, workBranch, applied2, remote.toUri(), "", identity);
        if (!second.branchName().equals(workBranch)) throw new AssertionError();
        String pushed2 = run(root, "git", "--git-dir=" + remote, "rev-parse", "refs/heads/" + workBranch).trim();
        if (!pushed2.equals(second.commitSha())) throw new AssertionError();
        String parent2 = run(root, "git", "--git-dir=" + remote, "rev-parse", pushed2 + "^1").trim();
        if (!parent2.equals(pushed)) throw new AssertionError("second import was not based on first work commit");
        System.out.println("GitDeliveryServiceSelfTest passed");
    }
    private static String run(Path dir, String... command) throws Exception {
        Process p = new ProcessBuilder(command).directory(dir.toFile()).redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes());
        if (p.waitFor()!=0) throw new IllegalStateException(out); return out;
    }
}
