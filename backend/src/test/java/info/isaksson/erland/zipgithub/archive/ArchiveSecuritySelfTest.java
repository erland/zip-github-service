package info.isaksson.erland.zipgithub.archive;

public final class ArchiveSecuritySelfTest {
    private ArchiveSecuritySelfTest() {}

    public static void main(String[] args) {
        ArchivePathValidator validator = new ArchivePathValidator();
        validator.validateAndRegister("src/App.java", ArchiveEntryType.REGULAR_FILE);
        expect(ArchiveSecurityCode.TRAVERSAL_SEGMENT,
                () -> new ArchivePathValidator().validateAndRegister("../escape", ArchiveEntryType.REGULAR_FILE));
        expect(ArchiveSecurityCode.CASE_COLLISION, () -> {
            ArchivePathValidator paths = new ArchivePathValidator();
            paths.validateAndRegister("README.md", ArchiveEntryType.REGULAR_FILE);
            paths.validateAndRegister("readme.md", ArchiveEntryType.REGULAR_FILE);
        });
        System.out.println("Archive security self-test passed");
    }

    private static void expect(ArchiveSecurityCode code, Runnable action) {
        try {
            action.run();
            throw new AssertionError("Expected " + code);
        } catch (ArchiveSecurityException e) {
            if (e.code() != code) throw new AssertionError("Expected " + code + " but got " + e.code());
        }
    }
}
