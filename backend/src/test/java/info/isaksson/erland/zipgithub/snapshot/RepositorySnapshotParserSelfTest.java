package info.isaksson.erland.zipgithub.snapshot;

public final class RepositorySnapshotParserSelfTest {
    public static void main(String[] args) {
        String input = "100644 blob bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb 2\tz.txt\0"
                + "100644 blob aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa 1\ta.txt\0";
        var entries = RepositorySnapshotService.parseTree(input);
        if (entries.size() != 2 || !entries.get(0).path().equals("a.txt") || !entries.get(1).path().equals("z.txt")) {
            throw new AssertionError("tree inventory is not deterministic");
        }
    }
}
