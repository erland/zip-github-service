package info.isaksson.erland.zipgithub.github;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/** Creates the one empty root commit required before GitHub can host Work branches in a brand-new repository. */
@ApplicationScoped
public class GitRepositoryBootstrapService {
    private static final String MESSAGE = "Initialize empty repository for zip-GitHub";
    private static final String NAME = "zip-GitHub";
    private static final String EMAIL = "zip-github@isaksson.info";

    @Inject GitHubInstallationTokenProvider tokens;

    public String bootstrapEmptyRepository(long installationId, String repositoryFullName, String defaultBranch) {
        String token = tokens.createInstallationToken(installationId);
        URI remote = URI.create("https://github.com/" + repositoryFullName + ".git");
        return bootstrapEmptyRepository(remote, token, defaultBranch);
    }

    String bootstrapEmptyRepository(URI remote, String token, String defaultBranch) {
        if (remote == null || defaultBranch == null || defaultBranch.isBlank() || unsafeBranch(defaultBranch))
            throw new IllegalArgumentException("Remote and a safe default branch are required.");
        Path workspace = null;
        try {
            workspace = Files.createTempDirectory("zip-github-empty-bootstrap-");
            runPlain(workspace, "git", "init", "--quiet");
            runPlain(workspace, "git", "config", "user.name", NAME);
            runPlain(workspace, "git", "config", "user.email", EMAIL);
            runPlain(workspace, "git", "checkout", "--quiet", "-b", defaultBranch);
            runPlain(workspace, "git", "commit", "--quiet", "--allow-empty", "-m", MESSAGE);
            String sha = runPlain(workspace, "git", "rev-parse", "HEAD^{commit}").trim();
            if (!sha.matches("[0-9a-fA-F]{40,64}")) throw new IllegalStateException("Git returned an invalid bootstrap commit SHA.");
            run(workspace, token, "git", "push", "--porcelain", remote.toString(), "HEAD:refs/heads/" + defaultBranch);
            return sha.toLowerCase();
        } catch (IOException e) {
            throw new IllegalStateException("Could not create temporary repository bootstrap workspace.", e);
        } finally {
            if (workspace != null) try { deleteRecursively(workspace); } catch (IOException ignored) { }
        }
    }

    private static boolean unsafeBranch(String branch) {
        return branch.contains("..") || branch.startsWith("/") || branch.endsWith("/") || branch.contains("\\");
    }

    private static String runPlain(Path directory, String... command) {
        try {
            Process process = new ProcessBuilder(command).directory(directory.toFile()).redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exit = process.waitFor();
            if (exit != 0) throw new IllegalStateException("Git bootstrap command failed (exit " + exit + "): " + output.trim());
            return output;
        } catch (IOException e) {
            throw new IllegalStateException("Could not start Git bootstrap command.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Git bootstrap command was interrupted.", e);
        }
    }

    private static String run(Path directory, String token, String... command) {
        try {
            ProcessBuilder builder = new ProcessBuilder(command).directory(directory.toFile()).redirectErrorStream(true);
            builder.environment().put("GIT_ASKPASS", "/usr/local/bin/zip-github-git-askpass");
            builder.environment().put("GIT_TERMINAL_PROMPT", "0");
            builder.environment().put("ZIP_GITHUB_GIT_TOKEN", token == null ? "" : token);
            Process process = builder.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exit = process.waitFor();
            if (exit != 0) throw new IllegalStateException("Git bootstrap push failed (exit " + exit + "): " + sanitize(output, token));
            return output;
        } catch (IOException e) {
            throw new IllegalStateException("Could not start Git bootstrap push.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Git bootstrap push was interrupted.", e);
        }
    }

    private static String sanitize(String value, String token) {
        String clean = value == null ? "" : value;
        if (token != null && !token.isBlank()) clean = clean.replace(token, "[REDACTED]");
        return clean.trim();
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) return;
        try (var stream = Files.walk(path)) {
            for (Path item : stream.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(item);
        }
    }
}
