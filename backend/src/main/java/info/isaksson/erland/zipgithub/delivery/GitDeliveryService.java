package info.isaksson.erland.zipgithub.delivery;

import info.isaksson.erland.zipgithub.github.GitHubInstallationTokenProvider;
import info.isaksson.erland.zipgithub.workspace.AppliedImportWorkspace;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Creates one branch and one commit from a verified workspace, then pushes it without force. */
@ApplicationScoped
public class GitDeliveryService {
    private final GitHubInstallationTokenProvider tokens;
    private final String authorName;
    private final String authorEmail;
    private final Clock clock;

    @Inject
    public GitDeliveryService(GitHubInstallationTokenProvider tokens,
            @ConfigProperty(name="zipgithub.delivery.git-author-name") String authorName,
            @ConfigProperty(name="zipgithub.delivery.git-author-email") String authorEmail) {
        this(tokens, authorName, authorEmail, Clock.systemUTC());
    }

    GitDeliveryService(GitHubInstallationTokenProvider tokens, String authorName, String authorEmail, Clock clock) {
        this.tokens = tokens; this.authorName = authorName; this.authorEmail = authorEmail; this.clock = clock;
    }

    public GitDeliveryResult deliver(long installationId, String baseBranch, AppliedImportWorkspace workspace) {
        String token = tokens.createInstallationToken(installationId);
        URI remote = URI.create("https://github.com/" + workspace.repositoryFullName() + ".git");
        return deliver(baseBranch, workspace, remote, token);
    }

    GitDeliveryResult deliver(String baseBranch, AppliedImportWorkspace workspace, URI remote, String token) {
        validate(baseBranch, workspace, remote);
        Path directory = workspace.workspacePath().toAbsolutePath().normalize();
        String branchName = branchName(workspace.importId());
        try {
            String head = runPlain(directory, "git", "rev-parse", "HEAD^{commit}").trim();
            if (!head.equalsIgnoreCase(workspace.baseCommitSha()))
                throw new GitDeliveryException("Workspace HEAD no longer matches the approved base commit.");

            String remoteBase = remoteBranchSha(directory, remote, token, baseBranch);
            if (!workspace.baseCommitSha().equalsIgnoreCase(remoteBase))
                throw new GitDeliveryException("The base branch moved after approval; create a new import plan.");

            runPlain(directory, "git", "checkout", "--quiet", "-b", branchName);
            runPlain(directory, "git", "config", "user.name", authorName);
            runPlain(directory, "git", "config", "user.email", authorEmail);
            runPlain(directory, "git", "add", "--all");
            verifyStagedPaths(directory, workspace.appliedPaths());
            runPlain(directory, "git", "commit", "--quiet", "-m", "Apply approved ZIP import " + workspace.importId());
            String commitSha = runPlain(directory, "git", "rev-parse", "HEAD^{commit}").trim();
            String parent = runPlain(directory, "git", "rev-parse", "HEAD^1^{commit}").trim();
            if (!parent.equalsIgnoreCase(workspace.baseCommitSha()))
                throw new GitDeliveryException("Delivery commit is not based directly on the approved commit.");
            run(directory, token, "git", "push", "--porcelain", remote.toString(),
                    "HEAD:refs/heads/" + branchName);
            return new GitDeliveryResult(workspace.importId(), workspace.repositoryFullName(), baseBranch,
                    branchName, workspace.baseCommitSha(), commitSha, workspace.planDigestSha256(), Instant.now(clock));
        } catch (RuntimeException e) {
            if (e instanceof GitDeliveryException) throw e;
            throw new GitDeliveryException("Could not create and push the delivery commit.", e);
        }
    }

    static String branchName(UUID importId) { return "zip-github/import-" + importId.toString(); }

    private static String remoteBranchSha(Path directory, URI remote, String token, String branch) {
        String output = run(directory, token, "git", "ls-remote", "--heads", remote.toString(), "refs/heads/" + branch).trim();
        if (output.isBlank()) throw new GitDeliveryException("The approved base branch no longer exists.");
        String[] fields = output.split("\\s+");
        if (fields.length < 2 || !fields[0].matches("[0-9a-fA-F]{40,64}"))
            throw new GitDeliveryException("Could not resolve the approved base branch.");
        return fields[0].toLowerCase();
    }

    private static void verifyStagedPaths(Path directory, List<String> expectedPaths) {
        Set<String> actual = new HashSet<>(parseNul(runPlain(directory, "git", "diff", "--cached", "--name-only", "--no-renames", "-z")));
        Set<String> expected = new HashSet<>(expectedPaths);
        if (!actual.equals(expected)) throw new GitDeliveryException("Staged Git changes do not match the approved workspace.");
        if (actual.isEmpty()) throw new GitDeliveryException("The approved workspace contains no committable changes.");
    }

    private static List<String> parseNul(String value) {
        List<String> paths = new ArrayList<>();
        for (String item : value.split("\\u0000", -1)) if (!item.isEmpty()) paths.add(item);
        paths.sort(String::compareTo); return paths;
    }

    private static void validate(String baseBranch, AppliedImportWorkspace workspace, URI remote) {
        if (workspace == null || baseBranch == null || baseBranch.isBlank() || remote == null)
            throw new IllegalArgumentException("Base branch, workspace and remote are required.");
        if (!Files.isDirectory(workspace.workspacePath().resolve(".git")))
            throw new GitDeliveryException("The prepared Git workspace no longer exists.");
        if (authorUnsafe(baseBranch)) throw new IllegalArgumentException("Invalid base branch.");
    }

    private static boolean authorUnsafe(String branch) {
        return branch.contains("..") || branch.startsWith("/") || branch.endsWith("/") || branch.contains("\\");
    }

    private static String runPlain(Path directory, String... command) {
        try {
            Process process = new ProcessBuilder(command).directory(directory.toFile()).redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exit = process.waitFor();
            if (exit != 0) throw new GitDeliveryException("Git command failed (exit " + exit + "): " + output.trim());
            return output;
        } catch (IOException e) { throw new GitDeliveryException("Could not start Git.", e); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new GitDeliveryException("Git command was interrupted.", e); }
    }

    private static String run(Path directory, String token, String... command) {
        try {
            ProcessBuilder builder = new ProcessBuilder(command).directory(directory.toFile()).redirectErrorStream(true);
            configureGitEnvironment(builder, token);
            builder.environment().put("ZIP_GITHUB_GIT_TOKEN", token == null ? "" : token);
            Process process = builder.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exit = process.waitFor();
            if (exit != 0) {
                String clean = sanitize(output, token);
                throw new GitDeliveryException("Git command failed (exit " + exit + "): " + clean, isRetryable(clean));
            }
            return output;
        } catch (IOException e) { throw new GitDeliveryException("Could not start Git.", e); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new GitDeliveryException("Git command was interrupted.", e); }
        
    }

    private static void configureGitEnvironment(ProcessBuilder builder, String token) {
        builder.environment().put("GIT_ASKPASS", "/usr/local/bin/zip-github-git-askpass");
        builder.environment().put("GIT_TERMINAL_PROMPT", "0");
        builder.environment().put("ZIP_GITHUB_GIT_TOKEN", token == null ? "" : token);
    }

    static boolean isRetryable(String message) {
        String value = message == null ? "" : message.toLowerCase();
        return value.contains("could not resolve host") || value.contains("connection timed out")
                || value.contains("connection reset") || value.contains("remote end hung up")
                || value.contains("http 500") || value.contains("http 502")
                || value.contains("http 503") || value.contains("http 504");
    }

    private static String sanitize(String value, String token) {
        return token == null || token.isBlank() ? value.trim() : value.replace(token, "[REDACTED]").trim();
    }
}
