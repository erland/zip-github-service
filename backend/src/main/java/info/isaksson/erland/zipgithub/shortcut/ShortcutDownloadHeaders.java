package info.isaksson.erland.zipgithub.shortcut;

/** Stable HTTP download identity for the signed iOS Shortcut release. */
public final class ShortcutDownloadHeaders {
    public static final String DOWNLOAD_FILENAME = "Skicka till zip-github.shortcut";

    private ShortcutDownloadHeaders() {}

    public static String contentDisposition() {
        return "attachment; filename=\"" + DOWNLOAD_FILENAME + "\"";
    }
}
