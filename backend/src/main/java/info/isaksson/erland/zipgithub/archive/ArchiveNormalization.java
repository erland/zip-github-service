package info.isaksson.erland.zipgithub.archive;

import java.util.List;
import java.util.Locale;

public final class ArchiveNormalization {
    private ArchiveNormalization() {
    }

    public static String detectSingleWrapper(List<String> filePaths) {
        String wrapper = null;
        boolean foundRelevantFile = false;
        for (String path : filePaths) {
            if (isTransportNoise(path)) {
                continue;
            }
            foundRelevantFile = true;
            int slash = path.indexOf('/');
            if (slash <= 0) {
                return null;
            }
            String first = path.substring(0, slash);
            if (wrapper == null) {
                wrapper = first;
            } else if (!wrapper.equals(first)) {
                return null;
            }
        }
        return foundRelevantFile ? wrapper : null;
    }

    public static String stripWrapper(String path, String wrapper) {
        if (wrapper == null) {
            return path;
        }
        String prefix = wrapper + "/";
        return path.startsWith(prefix) ? path.substring(prefix.length()) : path;
    }

    public static boolean isTransportNoise(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        String filename = lower.substring(lower.lastIndexOf('/') + 1);
        return lower.startsWith("__macosx/")
                || lower.contains("/__macosx/")
                || lower.endsWith("/.ds_store")
                || lower.equals(".ds_store")
                || filename.startsWith("._");
    }
}
