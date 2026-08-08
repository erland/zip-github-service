package info.isaksson.erland.zipgithub.workspace;

import info.isaksson.erland.zipgithub.archive.ArchiveNormalization;
import info.isaksson.erland.zipgithub.github.GitHubInstallationTokenProvider;
import info.isaksson.erland.zipgithub.plan.ImmutableImportPlan;
import info.isaksson.erland.zipgithub.plan.ImmutableImportPlanEntry;
import info.isaksson.erland.zipgithub.selection.ApprovedSelection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Prepares an isolated Git workspace and applies only approved ADDED/MODIFIED archive files. */
@ApplicationScoped
public class ImportWorkspaceService {
    private static final int BUFFER_SIZE = 64 * 1024;

    private final GitHubInstallationTokenProvider tokens;
    private final Path workspaceRoot;
    private final Clock clock;

    @Inject
    public ImportWorkspaceService(
            GitHubInstallationTokenProvider tokens,
            @ConfigProperty(name = "zipgithub.delivery.workspace-root") String workspaceRoot) {
        this(tokens, Path.of(workspaceRoot), Clock.systemUTC());
    }

    ImportWorkspaceService(GitHubInstallationTokenProvider tokens, Path workspaceRoot, Clock clock) {
        this.tokens = tokens;
        this.workspaceRoot = workspaceRoot.toAbsolutePath().normalize();
        this.clock = clock;
    }

    public AppliedImportWorkspace prepare(UUID importId, long installationId, String repositoryFullName,
                                          Path sourceZip, String strippedWrapperDirectory,
                                          ImmutableImportPlan plan, ApprovedSelection selection) {
        String token = tokens.createInstallationToken(installationId);
        URI remote = URI.create("https://github.com/" + repositoryFullName + ".git");
        return prepare(importId, repositoryFullName, remote, token, sourceZip, strippedWrapperDirectory, plan, selection);
    }

    AppliedImportWorkspace prepare(UUID importId, String repositoryFullName, URI remote, String token,
                                   Path sourceZip, String strippedWrapperDirectory, ImmutableImportPlan plan, ApprovedSelection selection) {
        validate(importId, repositoryFullName, remote, sourceZip, plan, selection);
        Path workspace = workspaceRoot.resolve(importId.toString()).normalize();
        if (!workspace.startsWith(workspaceRoot)) throw new ImportWorkspaceException("Invalid delivery workspace path.");
        boolean success = false;
        try {
            deleteRecursively(workspace);
            Files.createDirectories(workspace);
            run(workspace, token, "git", "init", "--quiet");
            run(workspace, token, "git", "config", "core.filemode", "true");
            run(workspace, token, "git", "remote", "add", "origin", remote.toString());
            run(workspace, token, "git", "fetch", "--quiet", "--depth=1", "origin", plan.baseCommitSha());
            String fetched = run(workspace, token, "git", "rev-parse", "FETCH_HEAD^{commit}").trim();
            if (!plan.baseCommitSha().equalsIgnoreCase(fetched)) {
                throw new ImportWorkspaceException("Fetched commit does not match the approved base commit.");
            }
            run(workspace, token, "git", "checkout", "--quiet", "--detach", fetched);
            run(workspace, token, "git", "remote", "remove", "origin");

            Map<String, ImmutableImportPlanEntry> expected = selectedChanges(plan, selection);
            applyArchive(sourceZip, strippedWrapperDirectory, workspace, expected);
            applyDeletions(workspace, expected);
            applyFileModes(workspace, expected);
            verifyWorkspace(workspace, expected);

            List<String> applied = expected.keySet().stream().sorted().toList();
            Map<String,String> expectedModes = expected.values().stream().filter(e -> !"WOULD_DELETE".equals(e.comparisonStatus()))
                    .collect(java.util.stream.Collectors.toUnmodifiableMap(ImmutableImportPlanEntry::path, ImmutableImportPlanEntry::effectiveMode));
            success = true;
            return new AppliedImportWorkspace(importId, repositoryFullName, fetched,
                    plan.planDigestSha256(), selection.selectionDigestSha256(), workspace, applied, expectedModes, Instant.now(clock));
        } catch (IOException e) {
            throw new ImportWorkspaceException("Could not prepare the import workspace.", e);
        } finally {
            if (!success) {
                try { deleteRecursively(workspace); } catch (IOException ignored) { }
            }
        }
    }

    public void delete(AppliedImportWorkspace workspace) {
        if (workspace == null) return;
        deletePath(workspace.workspacePath());
    }

    /** Removes any temporary workspace for an import, including one left behind before a backend restart. */
    public void delete(UUID importId) {
        if (importId == null) return;
        deletePath(workspaceRoot.resolve(importId.toString()));
    }

    private void deletePath(Path candidate) {
        Path path = candidate.toAbsolutePath().normalize();
        if (!path.startsWith(workspaceRoot)) throw new ImportWorkspaceException("Refusing to delete a workspace outside the configured root.");
        try { deleteRecursively(path); }
        catch (IOException e) { throw new ImportWorkspaceException("Could not delete the import workspace.", e); }
    }

    private static Map<String, ImmutableImportPlanEntry> selectedChanges(ImmutableImportPlan plan, ApprovedSelection selection) {
        if (!selection.planDigestSha256().equals(plan.planDigestSha256())
                || !selection.baseCommitSha().equals(plan.baseCommitSha())) {
            throw new ImportWorkspaceException("Selection identity does not match the approved import plan.");
        }
        Map<String, ImmutableImportPlanEntry> all = new HashMap<>();
        for (ImmutableImportPlanEntry entry : plan.entries()) all.put(entry.path(), entry);
        Set<String> approvedOverridePaths = selection.overrides().stream()
                .map(info.isaksson.erland.zipgithub.selection.ApprovedSelectionOverride::path)
                .collect(java.util.stream.Collectors.toSet());
        Map<String, ImmutableImportPlanEntry> expected = new HashMap<>();
        for (String path : selection.selectedPaths()) {
            ImmutableImportPlanEntry entry = all.get(path);
            if (entry == null) throw new ImportWorkspaceException("Selected path is missing from the import plan: " + path);
            if ("HARD_BLOCKED".equals(entry.blockerType())) throw new ImportWorkspaceException("Hard-blocked path reached workspace preparation: " + path);
            if ("OVERRIDABLE_BLOCKED".equals(entry.blockerType()) && !approvedOverridePaths.contains(path))
                throw new ImportWorkspaceException("Selected blocker lacks explicit override audit: " + path);
            boolean deletion = "WOULD_DELETE".equals(entry.comparisonStatus());
            if (!deletion && (entry.archiveSha256() == null || entry.archiveSizeBytes() == null)) {
                throw new ImportWorkspaceException("Selected change lacks archive identity: " + path);
            }
            expected.put(path, entry);
        }
        return Map.copyOf(expected);
    }

    private static void applyDeletions(Path workspace, Map<String, ImmutableImportPlanEntry> expected) throws IOException {
        for (ImmutableImportPlanEntry entry : expected.values()) {
            if (!"WOULD_DELETE".equals(entry.comparisonStatus())) continue;
            Path target = workspace.resolve(entry.path()).normalize();
            if (!target.startsWith(workspace) || target.equals(workspace)) {
                throw new ImportWorkspaceException("Deletion path escaped the workspace: " + entry.path());
            }
            Files.deleteIfExists(target);
        }
    }

    private static void applyArchive(Path sourceZip, String wrapper, Path workspace,
                                     Map<String, ImmutableImportPlanEntry> expected) throws IOException {
        Set<String> applied = new HashSet<>();
        try (InputStream input = new BufferedInputStream(Files.newInputStream(sourceZip));
             ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String original = entry.getName();
                String normalized = ArchiveNormalization.stripWrapper(original, wrapper);
                if (ArchiveNormalization.isTransportNoise(original) || ".DS_Store".equals(normalized)) {
                    drain(zip);
                    continue;
                }
                ImmutableImportPlanEntry planned = expected.get(normalized);
                if (planned == null || "WOULD_DELETE".equals(planned.comparisonStatus())) {
                    drain(zip);
                    continue;
                }
                Path target = workspace.resolve(normalized).normalize();
                if (!target.startsWith(workspace) || target.equals(workspace)) {
                    throw new ImportWorkspaceException("Archive path escaped the workspace: " + normalized);
                }
                Files.createDirectories(target.getParent());
                Path temporary = Files.createTempFile(target.getParent(), ".zip-github-", ".part");
                try {
                    HashedCopy copied = copyAndHash(zip, temporary);
                    if (copied.size() != planned.archiveSizeBytes()
                            || !copied.sha256().equals(planned.archiveSha256())) {
                        throw new ImportWorkspaceException("Archive content no longer matches the approved plan: " + normalized);
                    }
                    Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                    applied.add(normalized);
                } finally {
                    Files.deleteIfExists(temporary);
                }
            }
        }
        Set<String> expectedArchivePaths = expected.values().stream()
                .filter(item -> !"WOULD_DELETE".equals(item.comparisonStatus()))
                .map(ImmutableImportPlanEntry::path).collect(java.util.stream.Collectors.toSet());
        if (!applied.equals(expectedArchivePaths)) {
            Set<String> missing = new HashSet<>(expectedArchivePaths);
            missing.removeAll(applied);
            throw new ImportWorkspaceException("Approved files were missing from the source ZIP: " + String.join(", ", missing));
        }
    }

    private static void applyFileModes(Path workspace, Map<String, ImmutableImportPlanEntry> expected) throws IOException {
        for (ImmutableImportPlanEntry entry : expected.values()) {
            if ("WOULD_DELETE".equals(entry.comparisonStatus())) continue;
            String mode = entry.effectiveMode();
            if (!"100644".equals(mode) && !"100755".equals(mode))
                throw new ImportWorkspaceException("Approved file lacks a supported Git mode: " + entry.path());
            Path file = workspace.resolve(entry.path()).normalize();
            Set<PosixFilePermission> permissions = new HashSet<>(Files.getPosixFilePermissions(file));
            permissions.remove(PosixFilePermission.OWNER_EXECUTE);
            permissions.remove(PosixFilePermission.GROUP_EXECUTE);
            permissions.remove(PosixFilePermission.OTHERS_EXECUTE);
            if ("100755".equals(mode)) {
                permissions.add(PosixFilePermission.OWNER_EXECUTE);
                permissions.add(PosixFilePermission.GROUP_EXECUTE);
                permissions.add(PosixFilePermission.OTHERS_EXECUTE);
            }
            Files.setPosixFilePermissions(file, permissions);
        }
    }

    private static void verifyWorkspace(Path workspace, Map<String, ImmutableImportPlanEntry> expected) {
        Set<String> changed = new HashSet<>();
        parseNulPaths(runPlain(workspace, "git", "diff", "--name-only", "--no-renames", "-z")).forEach(changed::add);
        parseNulPaths(runPlain(workspace, "git", "ls-files", "--others", "--exclude-standard", "-z")).forEach(changed::add);
        if (!changed.equals(expected.keySet())) {
            throw new ImportWorkspaceException("Local Git diff does not match the approved selection.");
        }
        for (ImmutableImportPlanEntry entry : expected.values()) {
            Path file = workspace.resolve(entry.path()).normalize();
            if ("WOULD_DELETE".equals(entry.comparisonStatus())) {
                if (Files.exists(file)) throw new ImportWorkspaceException("Selected deletion still exists: " + entry.path());
                continue;
            }
            try {
                HashedFile actual = hashFile(file);
                if (actual.size() != entry.archiveSizeBytes() || !actual.sha256().equals(entry.archiveSha256())) {
                    throw new ImportWorkspaceException("Applied file does not match the approved content: " + entry.path());
                }
                Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(file);
                boolean executable = permissions.contains(PosixFilePermission.OWNER_EXECUTE)
                        || permissions.contains(PosixFilePermission.GROUP_EXECUTE)
                        || permissions.contains(PosixFilePermission.OTHERS_EXECUTE);
                if (("100755".equals(entry.effectiveMode())) != executable)
                    throw new ImportWorkspaceException("Applied file mode does not match the approved plan: " + entry.path());
            } catch (IOException e) {
                throw new ImportWorkspaceException("Could not verify applied file: " + entry.path(), e);
            }
        }
    }

    private static List<String> parseNulPaths(String output) {
        List<String> result = new ArrayList<>();
        for (String item : output.split("\\u0000", -1)) if (!item.isEmpty()) result.add(item);
        result.sort(String::compareTo);
        return result;
    }

    private static HashedCopy copyAndHash(InputStream input, Path target) throws IOException {
        MessageDigest digest = sha256();
        long size = 0;
        byte[] buffer = new byte[BUFFER_SIZE];
        try (var output = Files.newOutputStream(target)) {
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
                digest.update(buffer, 0, read);
                size += read;
            }
        }
        return new HashedCopy(size, HexFormat.of().formatHex(digest.digest()));
    }

    private static HashedFile hashFile(Path file) throws IOException {
        MessageDigest digest = sha256();
        long size = 0;
        byte[] buffer = new byte[BUFFER_SIZE];
        try (InputStream input = Files.newInputStream(file)) {
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
                size += read;
            }
        }
        return new HashedFile(size, HexFormat.of().formatHex(digest.digest()));
    }

    private static MessageDigest sha256() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 is unavailable", e); }
    }

    private static void drain(InputStream input) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        while (input.read(buffer) != -1) { }
    }

    private static void validate(UUID importId, String repositoryFullName, URI remote, Path sourceZip,
                                 ImmutableImportPlan plan, ApprovedSelection selection) {
        if (importId == null || plan == null || selection == null || !importId.equals(plan.importId()) || !importId.equals(selection.importId()))
            throw new IllegalArgumentException("Import, plan and selection identity must match.");
        if (repositoryFullName == null || !repositoryFullName.matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+"))
            throw new IllegalArgumentException("repositoryFullName is invalid");
        if (remote == null || sourceZip == null || !Files.isRegularFile(sourceZip))
            throw new IllegalArgumentException("Remote and source ZIP are required.");
    }

    private static String runPlain(Path directory, String... command) {
        try {
            Process process = new ProcessBuilder(command).directory(directory.toFile()).redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exit = process.waitFor();
            if (exit != 0) throw new ImportWorkspaceException("Git command failed (exit " + exit + "): " + output.trim());
            return output;
        } catch (IOException e) {
            throw new ImportWorkspaceException("Could not start Git.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ImportWorkspaceException("Git command was interrupted.", e);
        }
    }

    private static String run(Path directory, String token, String... command) {
        try {
            ProcessBuilder builder = new ProcessBuilder(command).directory(directory.toFile()).redirectErrorStream(true);
            configureGitEnvironment(builder, token);
            Process process = builder.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exit = process.waitFor();
            if (exit != 0) throw new ImportWorkspaceException("Git command failed (exit " + exit + "): " + sanitize(output, token));
            return output;
        } catch (IOException e) {
            throw new ImportWorkspaceException("Could not start Git.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ImportWorkspaceException("Git command was interrupted.", e);
        }
    }

    private static void configureGitEnvironment(ProcessBuilder builder, String token) {
        builder.environment().put("GIT_ASKPASS", "/usr/local/bin/zip-github-git-askpass");
        builder.environment().put("GIT_TERMINAL_PROMPT", "0");
        builder.environment().put("ZIP_GITHUB_GIT_TOKEN", token == null ? "" : token);
    }

    private static String sanitize(String value, String token) {
        return token == null || token.isBlank() ? value.trim() : value.replace(token, "[REDACTED]").trim();
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (root == null || !Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }

    private record HashedCopy(long size, String sha256) { }
    private record HashedFile(long size, String sha256) { }
}
