package info.isaksson.erland.zipgithub.snapshot;

import static org.junit.jupiter.api.Assertions.*;

import info.isaksson.erland.zipgithub.github.GitHubInstallationTokenProvider;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RepositorySnapshotServiceTest {
    @TempDir Path temp;

    @Test
    void locksBranchToExactCommitAndInventoriesTree() throws Exception {
        Path source = temp.resolve("source");
        Files.createDirectories(source);
        git(source, "init", "--quiet", "-b", "main");
        git(source, "config", "user.name", "Snapshot Test");
        git(source, "config", "user.email", "snapshot@example.invalid");
        Files.writeString(source.resolve("README.md"), "hello\n");
        Files.writeString(source.resolve(".gitignore"), "/shortcut/releases/*.shortcut\n*.log\n");
        Files.createDirectories(source.resolve("src"));
        Files.writeString(source.resolve("src/App.java"), "class App {}\n");
        git(source, "add", ".");
        git(source, "commit", "--quiet", "-m", "initial");
        String expectedSha = git(source, "rev-parse", "HEAD").trim();

        GitHubInstallationTokenProvider tokens = installationId -> "unused-local-token";
        RepositorySnapshotService service = new RepositorySnapshotService(
                tokens, temp.resolve("workspaces"), Clock.fixed(Instant.parse("2026-08-06T19:30:00Z"), ZoneOffset.UTC));
        UUID importId = UUID.randomUUID();

        RepositorySnapshot snapshot = service.create(importId, "erland/local-test", "main", source.toUri(), "unused-local-token");

        assertEquals(expectedSha, snapshot.baseCommitSha());
        assertEquals("main", snapshot.branch());
        assertEquals(3, snapshot.entries().size());
        assertEquals(".gitignore", snapshot.entries().get(0).path());
        assertEquals("README.md", snapshot.entries().get(1).path());
        assertEquals("src/App.java", snapshot.entries().get(2).path());
        assertEquals("/shortcut/releases/*.shortcut\n*.log\n", snapshot.gitIgnoreFiles().get(".gitignore"));
        assertTrue(snapshot.entries().stream().allMatch(entry -> entry.objectType().equals("blob")));
        assertTrue(snapshot.entries().stream().allMatch(entry -> entry.sha256() != null && entry.sha256().matches("[0-9a-f]{64}")));
        assertFalse(Files.exists(temp.resolve("workspaces").resolve(importId.toString())), "temporary workspace must be deleted");
    }

    @Test
    void treeParserIsDeterministic() {
        String input = "100644 blob bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb 2\tz.txt\0"
                + "100644 blob aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa 1\ta.txt\0";
        var entries = RepositorySnapshotService.parseTree(input);
        assertEquals("a.txt", entries.get(0).path());
        assertEquals("z.txt", entries.get(1).path());
    }

    private static String git(Path directory, String... args) throws Exception {
        String[] command = new String[args.length + 1];
        command[0] = "git";
        System.arraycopy(args, 0, command, 1, args.length);
        Process process = new ProcessBuilder(command).directory(directory.toFile()).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes());
        int exit = process.waitFor();
        if (exit != 0) throw new AssertionError("git failed: " + output);
        return output;
    }
}
