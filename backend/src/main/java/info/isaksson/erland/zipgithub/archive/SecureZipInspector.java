package info.isaksson.erland.zipgithub.archive;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class SecureZipInspector {
    private static final int EOCD_SIGNATURE = 0x06054b50;
    private static final int CENTRAL_SIGNATURE = 0x02014b50;
    private static final Charset CP437 = Charset.forName("CP437");
    private static final int UNIX_FILE_TYPE_MASK = 0170000;
    private static final int UNIX_REGULAR_FILE = 0100000;
    private static final int UNIX_DIRECTORY = 0040000;
    private static final int UNIX_SYMLINK = 0120000;
    private static final int BUFFER_SIZE = 64 * 1024;

    public List<ArchiveEntryDescriptor> inspect(Path zipFile) throws IOException {
        return inspect(zipFile, ArchiveResourceLimits.defaults());
    }

    public List<ArchiveEntryDescriptor> inspect(Path zipFile, ArchiveResourceLimits limits) throws IOException {
        List<ArchiveEntryDescriptor> entries;
        try (FileChannel channel = FileChannel.open(zipFile, StandardOpenOption.READ)) {
            long fileSize = channel.size();
            if (fileSize > limits.maxCompressedBytes()) {
                throw violation(ArchiveSecurityCode.COMPRESSED_SIZE_LIMIT_EXCEEDED, null,
                        "Compressed ZIP size exceeds configured limit");
            }
            Eocd eocd = readEndOfCentralDirectory(channel, fileSize);
            if (eocd.entryCount == 0xffff || eocd.centralSize == 0xffffffffL || eocd.centralOffset == 0xffffffffL) {
                throw violation(ArchiveSecurityCode.UNSUPPORTED_ZIP64, null,
                        "ZIP64 archives are not supported by this inspection stage");
            }
            if (eocd.entryCount > limits.maxEntries()) {
                throw violation(ArchiveSecurityCode.ENTRY_COUNT_LIMIT_EXCEEDED, null,
                        "ZIP contains more entries than the configured limit");
            }
            if (eocd.centralOffset < 0 || eocd.centralSize < 0
                    || eocd.centralOffset > fileSize - eocd.centralSize) {
                throw violation(ArchiveSecurityCode.INVALID_ZIP_STRUCTURE, null,
                        "Central directory points outside the ZIP file");
            }

            ArchivePathValidator paths = new ArchivePathValidator();
            entries = new ArrayList<>(eocd.entryCount);
            long declaredUncompressedTotal = 0;
            long cursor = eocd.centralOffset;
            long centralEnd = eocd.centralOffset + eocd.centralSize;
            for (int index = 0; index < eocd.entryCount; index++) {
                ByteBuffer header = read(channel, cursor, 46);
                if (header.getInt(0) != CENTRAL_SIGNATURE) {
                    throw violation(ArchiveSecurityCode.INVALID_ZIP_STRUCTURE, null,
                            "Invalid central directory entry signature");
                }
                int versionMadeBy = ushort(header, 4);
                int flags = ushort(header, 8);
                long compressedSize = uint(header, 20);
                long uncompressedSize = uint(header, 24);
                int nameLength = ushort(header, 28);
                int extraLength = ushort(header, 30);
                int commentLength = ushort(header, 32);
                long externalAttributes = uint(header, 38);
                long totalLength = 46L + nameLength + extraLength + commentLength;
                if (cursor > centralEnd - totalLength) {
                    throw violation(ArchiveSecurityCode.INVALID_ZIP_STRUCTURE, null,
                            "Central directory entry exceeds declared directory size");
                }

                ByteBuffer nameBytes = read(channel, cursor + 46, nameLength);
                String rawName = decodeName(nameBytes, (flags & (1 << 11)) != 0);
                if (rawName.length() > limits.maxPathLength()) {
                    throw violation(ArchiveSecurityCode.PATH_LENGTH_LIMIT_EXCEEDED, rawName,
                            "ZIP entry path exceeds configured length limit");
                }
                int hostSystem = (versionMadeBy >>> 8) & 0xff;
                int unixMode = hostSystem == 3 ? (int) ((externalAttributes >>> 16) & 0xffff) : 0;
                ArchiveEntryType type = classify(rawName, unixMode);
                if (type == ArchiveEntryType.SYMLINK) {
                    throw violation(ArchiveSecurityCode.SYMLINK_NOT_ALLOWED, rawName,
                            "Symbolic links are not allowed in uploaded ZIP files");
                }
                if (type == ArchiveEntryType.SPECIAL_FILE) {
                    throw violation(ArchiveSecurityCode.SPECIAL_FILE_NOT_ALLOWED, rawName,
                            "Special files are not allowed in uploaded ZIP files");
                }
                String path = paths.validateAndRegister(rawName, type);
                if (type == ArchiveEntryType.REGULAR_FILE) {
                    if (uncompressedSize > limits.maxSingleFileBytes()) {
                        throw violation(ArchiveSecurityCode.SINGLE_FILE_SIZE_LIMIT_EXCEEDED, path,
                                "ZIP entry exceeds configured single-file limit");
                    }
                    declaredUncompressedTotal = addWithLimit(declaredUncompressedTotal, uncompressedSize,
                            limits.maxUncompressedBytes(), ArchiveSecurityCode.UNCOMPRESSED_SIZE_LIMIT_EXCEEDED,
                            path, "Declared uncompressed ZIP size exceeds configured limit");
                    validateRatio(compressedSize, uncompressedSize, limits.maxCompressionRatio(), path);
                }
                entries.add(new ArchiveEntryDescriptor(path, type, compressedSize, uncompressedSize, unixMode));
                cursor += totalLength;
            }
            if (cursor > centralEnd) {
                throw violation(ArchiveSecurityCode.INVALID_ZIP_STRUCTURE, null,
                        "Central directory entries exceed declared size");
            }
        }

        validateActualInflatedSizes(zipFile, limits);
        return List.copyOf(entries);
    }

    private static void validateActualInflatedSizes(Path zipFile, ArchiveResourceLimits limits) throws IOException {
        long total = 0;
        int count = 0;
        byte[] buffer = new byte[BUFFER_SIZE];
        try (ZipFile zip = new ZipFile(zipFile.toFile())) {
            Enumeration<? extends ZipEntry> enumeration = zip.entries();
            while (enumeration.hasMoreElements()) {
                ZipEntry entry = enumeration.nextElement();
                count++;
                if (count > limits.maxEntries()) {
                    throw violation(ArchiveSecurityCode.ENTRY_COUNT_LIMIT_EXCEEDED, null,
                            "ZIP contains more entries than the configured limit");
                }
                if (entry.isDirectory()) continue;
                long fileTotal = 0;
                try (InputStream input = zip.getInputStream(entry)) {
                    int read;
                    while ((read = input.read(buffer)) >= 0) {
                        if (read == 0) continue;
                        fileTotal = addWithLimit(fileTotal, read, limits.maxSingleFileBytes(),
                                ArchiveSecurityCode.SINGLE_FILE_SIZE_LIMIT_EXCEEDED, entry.getName(),
                                "Inflated ZIP entry exceeds configured single-file limit");
                        total = addWithLimit(total, read, limits.maxUncompressedBytes(),
                                ArchiveSecurityCode.UNCOMPRESSED_SIZE_LIMIT_EXCEEDED, entry.getName(),
                                "Inflated ZIP size exceeds configured total limit");
                        validateRatio(entry.getCompressedSize(), fileTotal, limits.maxCompressionRatio(), entry.getName());
                    }
                }
            }
        }
    }

    private static long addWithLimit(long current, long addition, long limit, ArchiveSecurityCode code,
                                     String path, String message) {
        if (addition < 0 || current > limit - addition) {
            throw violation(code, path, message);
        }
        return current + addition;
    }

    private static void validateRatio(long compressed, long uncompressed, double maxRatio, String path) {
        if (uncompressed == 0) return;
        if (compressed <= 0 || ((double) uncompressed / (double) compressed) > maxRatio) {
            throw violation(ArchiveSecurityCode.COMPRESSION_RATIO_LIMIT_EXCEEDED, path,
                    "ZIP entry compression ratio exceeds configured limit");
        }
    }

    private static ArchiveEntryType classify(String name, int unixMode) {
        int fileType = unixMode & UNIX_FILE_TYPE_MASK;
        if (fileType == UNIX_SYMLINK) return ArchiveEntryType.SYMLINK;
        if (fileType != 0 && fileType != UNIX_REGULAR_FILE && fileType != UNIX_DIRECTORY) {
            return ArchiveEntryType.SPECIAL_FILE;
        }
        if (fileType == UNIX_DIRECTORY || name.endsWith("/")) return ArchiveEntryType.DIRECTORY;
        return ArchiveEntryType.REGULAR_FILE;
    }

    private static Eocd readEndOfCentralDirectory(FileChannel channel, long fileSize) throws IOException {
        if (fileSize < 22) {
            throw violation(ArchiveSecurityCode.INVALID_ZIP_STRUCTURE, null, "ZIP file is too short");
        }
        int tailLength = (int) Math.min(fileSize, 65_557L);
        long tailOffset = fileSize - tailLength;
        ByteBuffer tail = read(channel, tailOffset, tailLength);
        for (int i = tailLength - 22; i >= 0; i--) {
            if (tail.getInt(i) == EOCD_SIGNATURE) {
                int commentLength = ushort(tail, i + 20);
                if (i + 22 + commentLength != tailLength) continue;
                return new Eocd(ushort(tail, i + 10), uint(tail, i + 12), uint(tail, i + 16));
            }
        }
        throw violation(ArchiveSecurityCode.INVALID_ZIP_STRUCTURE, null,
                "End of central directory was not found");
    }

    private static String decodeName(ByteBuffer bytes, boolean utf8) {
        Charset charset = utf8 ? Charset.forName("UTF-8") : CP437;
        try {
            return charset.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(bytes)
                    .toString();
        } catch (CharacterCodingException e) {
            throw violation(ArchiveSecurityCode.INVALID_ENTRY_NAME_ENCODING, null,
                    "ZIP entry name has invalid encoding");
        }
    }

    private static ByteBuffer read(FileChannel channel, long offset, int length) throws IOException {
        if (offset < 0 || length < 0 || offset > channel.size() - length) {
            throw violation(ArchiveSecurityCode.INVALID_ZIP_STRUCTURE, null,
                    "ZIP structure contains an out-of-range field");
        }
        ByteBuffer buffer = ByteBuffer.allocate(length).order(ByteOrder.LITTLE_ENDIAN);
        while (buffer.hasRemaining()) {
            int count = channel.read(buffer, offset + buffer.position());
            if (count < 0) {
                throw violation(ArchiveSecurityCode.INVALID_ZIP_STRUCTURE, null,
                        "Unexpected end of ZIP file");
            }
        }
        return buffer.flip().order(ByteOrder.LITTLE_ENDIAN);
    }

    private static int ushort(ByteBuffer buffer, int offset) {
        return Short.toUnsignedInt(buffer.getShort(offset));
    }

    private static long uint(ByteBuffer buffer, int offset) {
        return Integer.toUnsignedLong(buffer.getInt(offset));
    }

    private static ArchiveSecurityException violation(ArchiveSecurityCode code, String path, String message) {
        return new ArchiveSecurityException(code, path, message);
    }

    private record Eocd(int entryCount, long centralSize, long centralOffset) {}
}
