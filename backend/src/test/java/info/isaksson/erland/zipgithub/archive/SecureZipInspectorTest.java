package info.isaksson.erland.zipgithub.archive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SecureZipInspectorTest {
    @TempDir Path tempDir;

    @Test
    void inventoriesRegularFilesAndDirectories() throws Exception {
        Path zip = createZip("project.zip", List.of("src/", "src/App.tsx"));
        List<ArchiveEntryDescriptor> entries = new SecureZipInspector().inspect(zip);
        assertEquals(2, entries.size());
        assertEquals(ArchiveEntryType.DIRECTORY, entries.get(0).type());
        assertEquals(ArchiveEntryType.REGULAR_FILE, entries.get(1).type());
    }

    @Test
    void rejectsTraversalEntry() throws Exception {
        Path zip = createZip("traversal.zip", List.of("../outside.txt"));
        ArchiveSecurityException exception = assertThrows(ArchiveSecurityException.class,
                () -> new SecureZipInspector().inspect(zip));
        assertEquals(ArchiveSecurityCode.TRAVERSAL_SEGMENT, exception.code());
    }

    @Test
    void rejectsUnixSymlinkFromCentralDirectoryMetadata() throws Exception {
        Path zip = createZip("symlink.zip", List.of("link"));
        patchFirstCentralEntryAsUnixType(zip, 0120777);
        ArchiveSecurityException exception = assertThrows(ArchiveSecurityException.class,
                () -> new SecureZipInspector().inspect(zip));
        assertEquals(ArchiveSecurityCode.SYMLINK_NOT_ALLOWED, exception.code());
    }

    @Test
    void rejectsUnixSpecialFile() throws Exception {
        Path zip = createZip("fifo.zip", List.of("pipe"));
        patchFirstCentralEntryAsUnixType(zip, 0010644);
        ArchiveSecurityException exception = assertThrows(ArchiveSecurityException.class,
                () -> new SecureZipInspector().inspect(zip));
        assertEquals(ArchiveSecurityCode.SPECIAL_FILE_NOT_ALLOWED, exception.code());
    }

    private Path createZip(String name, List<String> paths) throws IOException {
        Path zip = tempDir.resolve(name);
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip))) {
            for (String path : paths) {
                out.putNextEntry(new ZipEntry(path));
                if (!path.endsWith("/")) out.write("test".getBytes());
                out.closeEntry();
            }
        }
        return zip;
    }

    private static void patchFirstCentralEntryAsUnixType(Path zip, int unixMode) throws IOException {
        byte[] bytes = Files.readAllBytes(zip);
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i <= bytes.length - 46; i++) {
            if (buffer.getInt(i) == 0x02014b50) {
                buffer.putShort(i + 4, (short) ((3 << 8) | 20));
                buffer.putInt(i + 38, unixMode << 16);
                Files.write(zip, bytes);
                return;
            }
        }
        throw new IOException("Central directory entry not found");
    }
}
