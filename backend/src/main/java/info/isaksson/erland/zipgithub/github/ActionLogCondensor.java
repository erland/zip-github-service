package info.isaksson.erland.zipgithub.github;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

final class ActionLogCondensor {
    private static final int MAX_LINES = 8;
    private static final int MAX_LINE_CHARS = 180;

    private static final Pattern ANSI_CSI = Pattern.compile("\\u001B\\[[0-?]*[ -/]*[@-~]");
    private static final Pattern ANSI_OSC = Pattern.compile("\\u001B\\][^\\u0007]*(?:\\u0007|\\u001B\\\\)");
    private static final Pattern GITHUB_TOKEN = Pattern.compile("(?i)\\b(?:gh[pousr]_[A-Za-z0-9_]{16,}|github_pat_[A-Za-z0-9_]{16,})\\b");
    private static final Pattern BEARER = Pattern.compile("(?i)\\bBearer\\s+[A-Za-z0-9._~+\\-/=]{8,}");
    private static final Pattern SECRET_ASSIGNMENT = Pattern.compile("(?i)(\\b(?:token|secret|password|passwd|api[_-]?key|private[_-]?key)\\b\\s*[:=]\\s*)([^\\s]+)");
    private static final Pattern AUTH_HEADER = Pattern.compile("(?i)(Authorization\\s*:\\s*)(.+)");

    private static final List<ToolPattern> TOOLS = List.of(
            new ToolPattern("Maven/Gradle", Pattern.compile("(?i)(\\[ERROR\\]|BUILD FAILURE|Failed to execute goal|COMPILATION ERROR|FAILURE: Build failed|Execution failed for task|> Task .+ FAILED|\\* What went wrong:)")),
            new ToolPattern("npm/Vite", Pattern.compile("(?i)(npm (?:ERR!|error)|ELIFECYCLE|vite(?:\\s|:).*(?:error|failed)|Build failed|failed to load config|RollupError)")),
            new ToolPattern("Pandoc", Pattern.compile("(?i)(pandoc:|Error producing PDF|pdflatex not found|xelatex not found|lualatex not found|Could not find data file)")),
            new ToolPattern("xcodebuild", Pattern.compile("(?i)(\\*\\* BUILD FAILED \\*\\*|xcodebuild: error:|Command .+ failed with a nonzero exit code|:[0-9]+:[0-9]+: error:|\\berror: .+)"))
    );

    private ActionLogCondensor() {}

    static Condensed condense(String raw) {
        if (raw == null || raw.isBlank()) return new Condensed("Unknown", List.of());
        String sanitized = sanitize(raw);
        String[] all = sanitized.split("\\R");
        ToolPattern selected = selectTool(all);
        if (selected == null) return new Condensed("Unknown", List.of());

        List<String> result = new ArrayList<>();
        for (int i = 0; i < all.length && result.size() < MAX_LINES; i++) {
            if (!selected.pattern().matcher(all[i]).find()) continue;
            addUnique(result, trimLine(all[i]));
            if (result.size() < MAX_LINES && i + 1 < all.length && continuation(all[i + 1])) {
                addUnique(result, trimLine(all[i + 1]));
            }
        }
        return new Condensed(selected.name(), List.copyOf(result));
    }

    static String sanitize(String value) {
        String clean = ANSI_OSC.matcher(value).replaceAll("");
        clean = ANSI_CSI.matcher(clean).replaceAll("");
        clean = clean.replaceAll("[\\p{Cc}&&[^\\r\\n\\t]]", "");
        clean = GITHUB_TOKEN.matcher(clean).replaceAll("[REDACTED_TOKEN]");
        clean = BEARER.matcher(clean).replaceAll("Bearer [REDACTED]");
        clean = AUTH_HEADER.matcher(clean).replaceAll("$1[REDACTED]");
        clean = SECRET_ASSIGNMENT.matcher(clean).replaceAll("$1[REDACTED]");
        return clean;
    }


    static List<String> context(String raw, int before, int after) {
        if (raw == null || raw.isBlank()) return List.of();
        String[] all = sanitize(raw).split("\\R");
        ToolPattern selected = selectTool(all);
        int match = -1;
        if (selected != null) {
            for (int i = 0; i < all.length; i++) {
                if (selected.pattern().matcher(all[i]).find()) { match = i; break; }
            }
        }
        if (match < 0) match = Math.max(0, all.length - Math.max(1, before + after + 1));
        int start = Math.max(0, match - Math.max(0, before));
        int end = Math.min(all.length, match + Math.max(0, after) + 1);
        List<String> lines = new ArrayList<>();
        for (int i = start; i < end; i++) lines.add(trimDisplayLine(all[i]));
        return List.copyOf(lines);
    }

    static List<String> sanitizedLines(String raw, int maxLines) {
        if (raw == null || raw.isBlank() || maxLines <= 0) return List.of();
        String[] all = sanitize(raw).split("\\R");
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < all.length && lines.size() < maxLines; i++) lines.add(trimDisplayLine(all[i]));
        return List.copyOf(lines);
    }

    private static String trimDisplayLine(String line) {
        return line.length() <= 500 ? line : line.substring(0, 499) + "…";
    }

    private static ToolPattern selectTool(String[] lines) {
        int best = 0;
        ToolPattern selected = null;
        for (ToolPattern tool : TOOLS) {
            int score = 0;
            for (String line : lines) if (tool.pattern().matcher(line).find()) score++;
            if (score > best) { best = score; selected = tool; }
        }
        return selected;
    }

    private static boolean continuation(String line) {
        String value = line.strip();
        if (value.isEmpty()) return false;
        String lower = value.toLowerCase(Locale.ROOT);
        return value.startsWith(">") || value.startsWith("Caused by:") || value.startsWith("at ")
                || lower.startsWith("error ") || lower.startsWith("error:") || lower.startsWith("reason:");
    }

    private static String trimLine(String line) {
        String value = line.strip();
        return value.length() <= MAX_LINE_CHARS ? value : value.substring(0, MAX_LINE_CHARS - 1) + "…";
    }

    private static void addUnique(List<String> lines, String candidate) {
        if (!candidate.isBlank() && !lines.contains(candidate)) lines.add(candidate);
    }

    record Condensed(String tool, List<String> lines) {}
    private record ToolPattern(String name, Pattern pattern) {}
}
