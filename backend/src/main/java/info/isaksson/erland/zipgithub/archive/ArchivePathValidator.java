package info.isaksson.erland.zipgithub.archive;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class ArchivePathValidator {
    private static final Pattern WINDOWS_DRIVE = Pattern.compile("^[A-Za-z]:.*");
    private final Map<String, ArchiveEntryType> exactPaths = new HashMap<>();
    private final Map<String, String> caseFoldedPaths = new HashMap<>();

    public String validateAndRegister(String rawPath, ArchiveEntryType type) {
        if (rawPath == null || rawPath.isEmpty()) {
            throw violation(ArchiveSecurityCode.EMPTY_ENTRY_NAME, rawPath, "ZIP entry name must not be empty");
        }
        if (rawPath.indexOf('\0') >= 0) {
            throw violation(ArchiveSecurityCode.NUL_IN_PATH, rawPath, "ZIP entry path contains a NUL character");
        }
        if (rawPath.indexOf('\\') >= 0) {
            throw violation(ArchiveSecurityCode.BACKSLASH_IN_PATH, rawPath, "Backslashes are not allowed in ZIP entry paths");
        }
        if (rawPath.startsWith("/") || rawPath.startsWith("//")) {
            throw violation(ArchiveSecurityCode.ABSOLUTE_PATH, rawPath, "Absolute ZIP entry paths are not allowed");
        }
        if (WINDOWS_DRIVE.matcher(rawPath).matches()) {
            throw violation(ArchiveSecurityCode.WINDOWS_DRIVE_PATH, rawPath, "Windows drive paths are not allowed");
        }

        String normalized = Normalizer.normalize(rawPath, Normalizer.Form.NFC);
        boolean directoryMarker = normalized.endsWith("/");
        String withoutTrailingSlash = directoryMarker ? normalized.substring(0, normalized.length() - 1) : normalized;
        if (withoutTrailingSlash.isEmpty()) {
            throw violation(ArchiveSecurityCode.EMPTY_ENTRY_NAME, rawPath, "ZIP entry path must identify a file or directory");
        }

        String[] segments = withoutTrailingSlash.split("/", -1);
        for (String segment : segments) {
            if (segment.isEmpty() || segment.equals(".")) {
                throw violation(ArchiveSecurityCode.DOT_SEGMENT, rawPath, "Empty and dot path segments are not allowed");
            }
            if (segment.equals("..")) {
                throw violation(ArchiveSecurityCode.TRAVERSAL_SEGMENT, rawPath, "Parent traversal segments are not allowed");
            }
            if (segment.indexOf('\0') >= 0) {
                throw violation(ArchiveSecurityCode.NUL_IN_PATH, rawPath, "ZIP entry path contains a NUL character");
            }
        }

        String canonical = withoutTrailingSlash;
        ArchiveEntryType previous = exactPaths.putIfAbsent(canonical, type);
        if (previous != null) {
            ArchiveSecurityCode code = previous == type ? ArchiveSecurityCode.DUPLICATE_PATH : ArchiveSecurityCode.PATH_TYPE_COLLISION;
            throw violation(code, rawPath, "Duplicate or conflicting ZIP entry path: " + canonical);
        }

        String folded = canonical.toLowerCase(Locale.ROOT);
        String previousCase = caseFoldedPaths.putIfAbsent(folded, canonical);
        if (previousCase != null && !previousCase.equals(canonical)) {
            throw violation(ArchiveSecurityCode.CASE_COLLISION, rawPath,
                    "ZIP paths collide on case-insensitive file systems: " + previousCase + " and " + canonical);
        }

        // A file may not be an ancestor of another path (for example `a` and `a/b.txt`).
        int slash = canonical.indexOf('/');
        while (slash > 0) {
            String ancestor = canonical.substring(0, slash);
            ArchiveEntryType ancestorType = exactPaths.get(ancestor);
            if (ancestorType != null && ancestorType != ArchiveEntryType.DIRECTORY) {
                throw violation(ArchiveSecurityCode.PATH_TYPE_COLLISION, rawPath,
                        "A non-directory entry is an ancestor of another ZIP path: " + ancestor);
            }
            slash = canonical.indexOf('/', slash + 1);
        }
        if (type != ArchiveEntryType.DIRECTORY) {
            String prefix = canonical + "/";
            boolean hasChild = exactPaths.keySet().stream().anyMatch(path -> path.startsWith(prefix));
            if (hasChild) {
                throw violation(ArchiveSecurityCode.PATH_TYPE_COLLISION, rawPath,
                        "A file path conflicts with an existing directory tree: " + canonical);
            }
        }
        return canonical;
    }

    private static ArchiveSecurityException violation(ArchiveSecurityCode code, String path, String message) {
        return new ArchiveSecurityException(code, path, message);
    }
}
