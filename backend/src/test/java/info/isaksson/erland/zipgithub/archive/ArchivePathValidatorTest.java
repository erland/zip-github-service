package info.isaksson.erland.zipgithub.archive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ArchivePathValidatorTest {
    @Test
    void rejectsTraversalAndAbsolutePaths() {
        ArchivePathValidator validator = new ArchivePathValidator();
        assertCode(ArchiveSecurityCode.TRAVERSAL_SEGMENT,
                () -> validator.validateAndRegister("../secret.txt", ArchiveEntryType.REGULAR_FILE));
        assertCode(ArchiveSecurityCode.ABSOLUTE_PATH,
                () -> validator.validateAndRegister("/etc/passwd", ArchiveEntryType.REGULAR_FILE));
        assertCode(ArchiveSecurityCode.WINDOWS_DRIVE_PATH,
                () -> validator.validateAndRegister("C:/temp/file.txt", ArchiveEntryType.REGULAR_FILE));
        assertCode(ArchiveSecurityCode.BACKSLASH_IN_PATH,
                () -> validator.validateAndRegister("..\\secret.txt", ArchiveEntryType.REGULAR_FILE));
    }

    @Test
    void rejectsDuplicatesCaseCollisionsAndFileDirectoryConflicts() {
        ArchivePathValidator duplicate = new ArchivePathValidator();
        duplicate.validateAndRegister("src/App.tsx", ArchiveEntryType.REGULAR_FILE);
        assertCode(ArchiveSecurityCode.DUPLICATE_PATH,
                () -> duplicate.validateAndRegister("src/App.tsx", ArchiveEntryType.REGULAR_FILE));

        ArchivePathValidator caseCollision = new ArchivePathValidator();
        caseCollision.validateAndRegister("README.md", ArchiveEntryType.REGULAR_FILE);
        assertCode(ArchiveSecurityCode.CASE_COLLISION,
                () -> caseCollision.validateAndRegister("readme.md", ArchiveEntryType.REGULAR_FILE));

        ArchivePathValidator typeCollision = new ArchivePathValidator();
        typeCollision.validateAndRegister("docs", ArchiveEntryType.REGULAR_FILE);
        assertCode(ArchiveSecurityCode.PATH_TYPE_COLLISION,
                () -> typeCollision.validateAndRegister("docs/guide.md", ArchiveEntryType.REGULAR_FILE));
    }

    @Test
    void acceptsNormalRelativePaths() {
        ArchivePathValidator validator = new ArchivePathValidator();
        assertEquals("src/main/App.tsx",
                validator.validateAndRegister("src/main/App.tsx", ArchiveEntryType.REGULAR_FILE));
        assertEquals("docs",
                validator.validateAndRegister("docs/", ArchiveEntryType.DIRECTORY));
    }

    private static void assertCode(ArchiveSecurityCode expected, Runnable action) {
        ArchiveSecurityException exception = assertThrows(ArchiveSecurityException.class, action::run);
        assertEquals(expected, exception.code());
    }
}
