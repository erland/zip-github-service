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
        AppliedImportWorkspace applied = new AppliedImportWorkspace(importId, "owner/repo", base, "a".repeat(64), "c".repeat(64), workspace,
                List.of("README.md"), Instant.now());
        GitDeliveryService service = new GitDeliveryService(id -> "",
                Clock.fixed(Instant.parse("2026-08-06T20:00:00Z"), ZoneOffset.UTC));
        GitCommitIdentity identity = new GitCommitIdentity("Other Author", "other@example.com", "Approver", "approver@example.com");
        String workBranch = "zip-github/work-" + UUID.randomUUID();
        String customMessage = "User selected commit message";
        GitDeliveryResult result = service.deliver("main", workBranch, applied, remote.toUri(), "", identity, customMessage);
        if (!result.branchName().equals(workBranch)) throw new AssertionError();
        String pushed = run(root, "git", "--git-dir=" + remote, "rev-parse", "refs/heads/" + result.branchName()).trim();
        if (!pushed.equals(result.commitSha())) throw new AssertionError();
        String parent = run(root, "git", "--git-dir=" + remote, "rev-parse", pushed + "^1").trim();
        if (!parent.equals(base)) throw new AssertionError();
        String author = run(root, "git", "--git-dir=" + remote, "show", "-s", "--format=%an <%ae>", pushed).trim();
        String committer = run(root, "git", "--git-dir=" + remote, "show", "-s", "--format=%cn <%ce>", pushed).trim();
        if (!author.equals("Other Author <other@example.com>")) throw new AssertionError(author);
        if (!committer.equals("Approver <approver@example.com>")) throw new AssertionError(committer);
        String message = run(root, "git", "--git-dir=" + remote, "show", "-s", "--format=%B", pushed).strip();
        if (!message.equals(customMessage)) throw new AssertionError(message);

        Path workspace2 = root.resolve("workspace2");
        run(root, "git", "clone", remote.toUri().toString(), workspace2.toString());
        run(workspace2, "git", "checkout", "--detach", pushed);
        Files.writeString(workspace2.resolve("README.md"), "after second\n");
        UUID importId2 = UUID.randomUUID();
        AppliedImportWorkspace applied2 = new AppliedImportWorkspace(importId2, "owner/repo", pushed, "b".repeat(64), "d".repeat(64), workspace2,
                List.of("README.md"), Instant.now());
        GitDeliveryResult second = service.deliver(workBranch, workBranch, applied2, remote.toUri(), "", identity);
        if (!second.branchName().equals(workBranch)) throw new AssertionError();
        String pushed2 = run(root, "git", "--git-dir=" + remote, "rev-parse", "refs/heads/" + workBranch).trim();
        if (!pushed2.equals(second.commitSha())) throw new AssertionError();
        String parent2 = run(root, "git", "--git-dir=" + remote, "rev-parse", pushed2 + "^1").trim();
        if (!parent2.equals(pushed)) throw new AssertionError("second import was not based on first work commit");

        // Stale-base regression: an approved workspace must not be delivered if the work branch moved after review.
        Path staleWorkspace = root.resolve("stale-workspace");
        run(root, "git", "clone", remote.toUri().toString(), staleWorkspace.toString());
        run(staleWorkspace, "git", "checkout", "--detach", pushed2);
        Files.writeString(staleWorkspace.resolve("README.md"), "approved but now stale\n");
        AppliedImportWorkspace staleApplied = new AppliedImportWorkspace(UUID.randomUUID(), "owner/repo", pushed2,
                "e".repeat(64), "f".repeat(64), staleWorkspace, List.of("README.md"), Instant.now());

        Path mover = root.resolve("mover");
        run(root, "git", "clone", remote.toUri().toString(), mover.toString());
        run(mover, "git", "checkout", workBranch);
        run(mover, "git", "config", "user.name", "Concurrent");
        run(mover, "git", "config", "user.email", "concurrent@example.com");
        Files.writeString(mover.resolve("concurrent.txt"), "branch moved\n");
        run(mover, "git", "add", ".");
        run(mover, "git", "commit", "-m", "move branch");
        run(mover, "git", "push", "origin", workBranch);

        try {
            service.deliver(workBranch, workBranch, staleApplied, remote.toUri(), "", identity);
            throw new AssertionError("stale base branch was accepted");
        } catch (GitDeliveryException expected) {
            if (!expected.getMessage().contains("moved after approval")) throw expected;
        }

        System.out.println("GitDeliveryServiceSelfTest passed");
    }
    private static String run(Path dir, String... command) throws Exception {
        Process p = new ProcessBuilder(command).directory(dir.toFile()).redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes());
        if (p.waitFor()!=0) throw new IllegalStateException(out); return out;
    }
}
