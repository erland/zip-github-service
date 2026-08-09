package info.isaksson.erland.zipgithub.comparison;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Evaluates repository .gitignore files for normalized ZIP paths without consulting the host filesystem. */
final class GitIgnoreMatcher {
    private final List<Rule> rules;

    GitIgnoreMatcher(Map<String, String> gitIgnoreFiles) {
        var result = new ArrayList<Rule>();
        if (gitIgnoreFiles != null) {
            gitIgnoreFiles.entrySet().stream()
                    .sorted(Comparator.comparingInt((Map.Entry<String, String> e) -> depth(directoryOf(e.getKey())))
                            .thenComparing(Map.Entry::getKey))
                    .forEach(entry -> parseFile(directoryOf(entry.getKey()), entry.getValue(), result));
        }
        this.rules = List.copyOf(result);
    }

    boolean isIgnored(String path) {
        String normalized = normalize(path);
        boolean ignored = false;
        for (Rule rule : rules) {
            if (!isWithin(rule.baseDirectory(), normalized)) continue;
            String relative = rule.baseDirectory().isEmpty() ? normalized
                    : normalized.substring(rule.baseDirectory().length() + 1);
            if (rule.pattern().matcher(relative).matches()) ignored = !rule.negated();
        }
        return ignored;
    }

    private static void parseFile(String baseDirectory, String content, List<Rule> target) {
        if (content == null || content.isEmpty()) return;
        for (String raw : content.split("\\R", -1)) {
            if (raw.isEmpty()) continue;
            String line = stripUnescapedTrailingSpaces(raw);
            if (line.isEmpty()) continue;
            boolean escapedMarker = line.startsWith("\\#") || line.startsWith("\\!");
            if (line.startsWith("#") && !escapedMarker) continue;
            boolean negated = line.startsWith("!") && !escapedMarker;
            if (negated) line = line.substring(1);
            else if (escapedMarker) line = line.substring(1);
            if (line.isEmpty()) continue;
            target.add(new Rule(baseDirectory, negated, compile(line)));
        }
    }

    private static Pattern compile(String source) {
        boolean directoryOnly = source.endsWith("/") && !source.endsWith("\\/");
        if (directoryOnly) source = source.substring(0, source.length() - 1);
        boolean anchored = source.startsWith("/");
        if (anchored) source = source.substring(1);
        boolean containsSlash = source.indexOf('/') >= 0;
        String glob = globToRegex(source);
        String prefix = anchored || containsSlash ? "^" : "^(?:.*/)?";
        // Git ignores descendants when a matched path is a directory, even without a trailing slash.
        String suffix = "(?:$|/.*$)";
        return Pattern.compile(prefix + glob + suffix);
    }

    private static String globToRegex(String glob) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            if (c == '*') {
                boolean doublestar = i + 1 < glob.length() && glob.charAt(i + 1) == '*';
                if (doublestar) {
                    while (i + 1 < glob.length() && glob.charAt(i + 1) == '*') i++;
                    if (i + 1 < glob.length() && glob.charAt(i + 1) == '/') {
                        i++;
                        out.append("(?:.*/)?");
                    } else out.append(".*");
                } else out.append("[^/]*");
            } else if (c == '?') out.append("[^/]");
            else if (c == '[') {
                int end = glob.indexOf(']', i + 1);
                if (end > i + 1) {
                    String cls = glob.substring(i + 1, end);
                    if (cls.startsWith("!")) cls = "^" + cls.substring(1);
                    out.append('[').append(cls.replace("\\", "\\\\")).append(']');
                    i = end;
                } else out.append("\\[");
            } else if (c == '\\' && i + 1 < glob.length()) {
                out.append(Pattern.quote(String.valueOf(glob.charAt(++i))));
            } else {
                if (".(){}+$^|".indexOf(c) >= 0) out.append('\\');
                out.append(c);
            }
        }
        return out.toString();
    }

    private static String stripUnescapedTrailingSpaces(String value) {
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == ' ') {
            int slashes = 0;
            for (int i = end - 2; i >= 0 && value.charAt(i) == '\\'; i--) slashes++;
            if ((slashes & 1) == 1) break;
            end--;
        }
        return value.substring(0, end);
    }

    private static String directoryOf(String gitIgnorePath) {
        String normalized = normalize(gitIgnorePath);
        int slash = normalized.lastIndexOf('/');
        return slash < 0 ? "" : normalized.substring(0, slash);
    }

    private static int depth(String directory) {
        if (directory.isEmpty()) return 0;
        return 1 + (int) directory.chars().filter(ch -> ch == '/').count();
    }

    private static boolean isWithin(String base, String path) {
        return base.isEmpty() || path.equals(base) || path.startsWith(base + "/");
    }

    private static String normalize(String value) {
        String normalized = value == null ? "" : value.replace('\\', '/');
        while (normalized.startsWith("/")) normalized = normalized.substring(1);
        return normalized;
    }

    private record Rule(String baseDirectory, boolean negated, Pattern pattern) { }
}
