package info.isaksson.erland.zipgithub.snapshot;

import info.isaksson.erland.zipgithub.github.GitHubInstallationTokenProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Creates a shallow, temporary Git workspace and inventories one exact branch commit. */
@ApplicationScoped
public class RepositorySnapshotService {
    private final GitHubInstallationTokenProvider tokens;
    private final Path workspaceRoot;
    private final Clock clock;

    @Inject
    public RepositorySnapshotService(
            GitHubInstallationTokenProvider tokens,
            @ConfigProperty(name = "zipgithub.snapshot.workspace-root") String workspaceRoot) {
        this(tokens, Path.of(workspaceRoot), Clock.systemUTC());
    }

    RepositorySnapshotService(GitHubInstallationTokenProvider tokens, Path workspaceRoot, Clock clock) {
        this.tokens = tokens;
        this.workspaceRoot = workspaceRoot.toAbsolutePath().normalize();
        this.clock = clock;
    }

    public RepositorySnapshot create(UUID importId, long installationId, String repositoryFullName, String branch) {
        URI remote = URI.create("https://github.com/" + repositoryFullName + ".git");
        String token = tokens.createInstallationToken(installationId);
        return create(importId, repositoryFullName, branch, remote, token);
    }

    RepositorySnapshot create(UUID importId, String repositoryFullName, String branch, URI remote, String token) {
        validate(importId, repositoryFullName, branch, remote);
        Path workspace = workspaceRoot.resolve(importId.toString()).normalize();
        if (!workspace.startsWith(workspaceRoot)) throw new RepositorySnapshotException("Invalid snapshot workspace path");
        try {
            deleteRecursively(workspace);
            Files.createDirectories(workspace);
            run(workspace, token, "git", "init", "--quiet");
            run(workspace, token, "git", "remote", "add", "origin", remote.toString());

            String ref = "refs/heads/" + branch;
            String remoteLine = run(workspace, token, "git", "ls-remote", "--heads", "origin", ref).trim();
            if (remoteLine.isEmpty()) throw new RepositorySnapshotException("The selected GitHub branch was not found.");
            String baseSha = remoteLine.split("\\s+", 2)[0];
            if (!baseSha.matches("[0-9a-fA-F]{40,64}")) throw new RepositorySnapshotException("Git returned an invalid base commit SHA.");

            // Fetch the exact object resolved above, not the moving branch name.
            run(workspace, token, "git", "fetch", "--quiet", "--depth=1", "origin", baseSha);
            String fetchedSha = run(workspace, token, "git", "rev-parse", "FETCH_HEAD^{commit}").trim();
            if (!baseSha.equalsIgnoreCase(fetchedSha)) throw new RepositorySnapshotException("Fetched commit does not match the locked branch SHA.");

            String tree = run(workspace, token, "git", "ls-tree", "-r", "-z", "--long", fetchedSha);
            List<RepositorySnapshotEntry> entries = parseTree(tree).stream()
                    .map(entry -> new RepositorySnapshotEntry(entry.path(), entry.mode(), entry.objectType(),
                            entry.objectId(), entry.sizeBytes(), hashBlob(workspace, token, entry.objectId())))
                    .toList();
            return new RepositorySnapshot(importId, repositoryFullName, branch, fetchedSha,
                    entries, Instant.now(clock));
        } catch (IOException e) {
            throw new RepositorySnapshotException("Could not create the repository snapshot.", e);
        } finally {
            try { deleteRecursively(workspace); } catch (IOException ignored) { }
        }
    }

    static List<RepositorySnapshotEntry> parseTree(String output) {
        List<RepositorySnapshotEntry> result = new ArrayList<>();
        for (String row : output.split("\\u0000", -1)) {
            if (row.isEmpty()) continue;
            int tab = row.indexOf('\t');
            if (tab < 0) throw new RepositorySnapshotException("Unexpected git ls-tree output.");
            String[] metadata = row.substring(0, tab).trim().split("\\s+", 4);
            if (metadata.length != 4) throw new RepositorySnapshotException("Unexpected git ls-tree metadata.");
            long size = "-".equals(metadata[3]) ? -1L : Long.parseLong(metadata[3]);
            result.add(new RepositorySnapshotEntry(row.substring(tab + 1), metadata[0], metadata[1], metadata[2], size, null));
        }
        result.sort(Comparator.comparing(RepositorySnapshotEntry::path));
        return List.copyOf(result);
    }


    private String hashBlob(Path directory, String token, String objectId) {
        Path askPass = null;
        try {
            askPass = createAskPass(directory);
            ProcessBuilder builder = new ProcessBuilder("git", "cat-file", "blob", objectId)
                    .directory(directory.toFile()).redirectErrorStream(false);
            configureGitEnvironment(builder, askPass, token);
            Process process = builder.start();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[64 * 1024];
            try (var input = process.getInputStream()) {
                int read;
                while ((read = input.read(buffer)) != -1) digest.update(buffer, 0, read);
            }
            String error = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            int exit = process.waitFor();
            if (exit != 0) throw new RepositorySnapshotException("Git could not read repository content: " + sanitize(error, token));
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException e) {
            throw new RepositorySnapshotException("Could not hash repository content.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RepositorySnapshotException("Repository hashing was interrupted.", e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        } finally {
            if (askPass != null) try { Files.deleteIfExists(askPass); } catch (IOException ignored) { }
        }
    }

    private static Path createAskPass(Path directory) throws IOException {
        Path askPass = Files.createTempFile(directory, ".git-askpass-", ".sh");
        Files.writeString(askPass, "#!/bin/sh\ncase \"$1\" in\n  *Username*) printf '%s\\n' 'x-access-token' ;;\n  *) printf '%s\\n' \"$ZIP_GITHUB_GIT_TOKEN\" ;;\nesac\n", StandardCharsets.UTF_8);
        askPass.toFile().setExecutable(true, true);
        return askPass;
    }

    private static void configureGitEnvironment(ProcessBuilder builder, Path askPass, String token) {
        builder.environment().put("GIT_ASKPASS", askPass.toString());
        builder.environment().put("GIT_TERMINAL_PROMPT", "0");
        builder.environment().put("ZIP_GITHUB_GIT_TOKEN", token == null ? "" : token);
    }

    private String run(Path directory, String token, String... command) {
        Path askPass = null;
        try {
            askPass = createAskPass(directory);
            ProcessBuilder builder = new ProcessBuilder(command).directory(directory.toFile()).redirectErrorStream(true);
            configureGitEnvironment(builder, askPass, token);
            Process process = builder.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exit = process.waitFor();
            if (exit != 0) throw new RepositorySnapshotException("Git command failed (exit " + exit + "): " + sanitize(output, token));
            return output;
        } catch (IOException e) {
            throw new RepositorySnapshotException("Could not start Git.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RepositorySnapshotException("Git command was interrupted.", e);
        } finally {
            if (askPass != null) try { Files.deleteIfExists(askPass); } catch (IOException ignored) { }
        }
    }

    private static String sanitize(String value, String token) {
        if (token == null || token.isBlank()) return value.trim();
        return value.replace(token, "[REDACTED]").trim();
    }

    private static void validate(UUID importId, String repositoryFullName, String branch, URI remote) {
        if (importId == null) throw new IllegalArgumentException("importId is required");
        if (repositoryFullName == null || !repositoryFullName.matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+"))
            throw new IllegalArgumentException("repositoryFullName is invalid");
        if (branch == null || branch.isBlank() || branch.contains("..") || branch.startsWith("/") || branch.endsWith("/") || branch.contains("\\"))
            throw new IllegalArgumentException("branch is invalid");
        if (remote == null) throw new IllegalArgumentException("remote is required");
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (root == null || !Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }
}
