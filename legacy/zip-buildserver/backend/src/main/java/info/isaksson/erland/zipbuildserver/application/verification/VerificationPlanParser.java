package info.isaksson.erland.zipbuildserver.application.verification;

import info.isaksson.erland.zipbuildserver.domain.model.NetworkMode;
import info.isaksson.erland.zipbuildserver.domain.model.project.ProjectTechnology;
import info.isaksson.erland.zipbuildserver.domain.model.verification.VerificationCommand;
import info.isaksson.erland.zipbuildserver.domain.model.verification.VerificationPlan;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class VerificationPlanParser {
    private final VerificationPlanValidator validator;

    public VerificationPlanParser() {
        this(new VerificationPlanValidator());
    }

    VerificationPlanParser(VerificationPlanValidator validator) {
        this.validator = validator;
    }

    public VerificationPlan parse(String source) {
        PlanBuilder builder = new PlanBuilder();
        VerificationCommandBuilder commandBuilder = null;
        Section section = Section.ROOT;

        String[] lines = source.split("\\R", -1);
        for (int index = 0; index < lines.length; index++) {
            int lineNumber = index + 1;
            String line = stripComment(lines[index]);
            if (line.isBlank()) {
                continue;
            }
            String trimmed = line.trim();

            if (!line.startsWith(" ")) {
                if (commandBuilder != null) {
                    builder.commands.add(commandBuilder.build());
                    commandBuilder = null;
                }
                if (trimmed.equals("indicators:")) {
                    section = Section.INDICATORS;
                } else if (trimmed.equals("commands:")) {
                    section = Section.COMMANDS;
                } else {
                    section = Section.ROOT;
                    applyRootKeyValue(builder, trimmed, lineNumber);
                }
                continue;
            }

            if (section == Section.INDICATORS) {
                builder.indicators.add(parseListValue(trimmed, lineNumber, "indicators"));
                continue;
            }

            if (section == Section.COMMANDS) {
                if (trimmed.startsWith("- label:")) {
                    if (commandBuilder != null) {
                        builder.commands.add(commandBuilder.build());
                    }
                    commandBuilder = new VerificationCommandBuilder();
                    commandBuilder.label = unquote(afterColon(trimmed, lineNumber));
                } else if (commandBuilder != null) {
                    applyCommandKeyValue(commandBuilder, trimmed, lineNumber);
                } else {
                    throw parseError(lineNumber, "commands entries must start with '- label:'.");
                }
                continue;
            }

            throw parseError(lineNumber, "indented content is only supported under indicators or commands.");
        }

        if (commandBuilder != null) {
            builder.commands.add(commandBuilder.build());
        }
        VerificationPlan plan = builder.build();
        validator.validate(plan);
        return plan;
    }

    private static void applyRootKeyValue(PlanBuilder builder, String line, int lineNumber) {
        String value = unquote(afterColon(line, lineNumber));
        if (line.startsWith("id:")) {
            builder.id = value;
        } else if (line.startsWith("name:")) {
            builder.name = value;
        } else if (line.startsWith("technology:")) {
            builder.technology = parseEnum(ProjectTechnology.class, value, "technology", lineNumber);
        } else if (line.startsWith("networkMode:")) {
            builder.networkMode = parseEnum(NetworkMode.class, value, "networkMode", lineNumber);
        } else if (line.startsWith("enabled:")) {
            builder.enabled = parseBoolean(value, "enabled", lineNumber);
        } else if (line.startsWith("selectionReason:")) {
            builder.selectionReason = value;
        } else {
            throw parseError(lineNumber, "unsupported root key.");
        }
    }

    private static void applyCommandKeyValue(VerificationCommandBuilder builder, String line, int lineNumber) {
        String value = unquote(afterColon(line, lineNumber));
        if (line.startsWith("workingDirectory:")) {
            builder.workingDirectory = value;
        } else if (line.startsWith("commandDisplay:")) {
            builder.commandDisplay = value;
        } else if (line.startsWith("timeoutSeconds:")) {
            builder.timeoutSeconds = parseInteger(value, "timeoutSeconds", lineNumber);
        } else if (line.startsWith("optional:")) {
            builder.optional = parseBoolean(value, "optional", lineNumber);
        } else {
            throw parseError(lineNumber, "unsupported command key.");
        }
    }

    private static String parseListValue(String line, int lineNumber, String sectionName) {
        if (!line.startsWith("- ")) {
            throw parseError(lineNumber, sectionName + " entries must start with '- '.");
        }
        return unquote(line.substring(2).trim());
    }

    private static String afterColon(String line, int lineNumber) {
        int colon = line.indexOf(':');
        if (colon < 0) {
            throw parseError(lineNumber, "expected key/value entry containing ':'.");
        }
        return line.substring(colon + 1).trim();
    }

    private static <T extends Enum<T>> T parseEnum(Class<T> enumType, String value, String key, int lineNumber) {
        try {
            return Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException exception) {
            throw parseError(lineNumber, "invalid " + key + " value '" + value + "'.", exception);
        }
    }

    private static int parseInteger(String value, String key, int lineNumber) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw parseError(lineNumber, "invalid integer for " + key + ": '" + value + "'.", exception);
        }
    }

    private static boolean parseBoolean(String value, String key, int lineNumber) {
        if ("true".equals(value)) {
            return true;
        }
        if ("false".equals(value)) {
            return false;
        }
        throw parseError(lineNumber, "invalid boolean for " + key + ": '" + value + "'.");
    }

    private static String stripComment(String line) {
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (character == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            } else if (character == '#' && !inSingleQuote && !inDoubleQuote) {
                return line.substring(0, index);
            }
        }
        return line;
    }

    private static String unquote(String value) {
        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static VerificationPlanParseException parseError(int lineNumber, String message) {
        return new VerificationPlanParseException("Unable to parse verification plan at line " + lineNumber + ": " + message);
    }

    private static VerificationPlanParseException parseError(int lineNumber, String message, Throwable cause) {
        return new VerificationPlanParseException("Unable to parse verification plan at line " + lineNumber + ": " + message, cause);
    }

    private enum Section {
        ROOT,
        INDICATORS,
        COMMANDS
    }

    private static final class PlanBuilder {
        private String id;
        private String name;
        private ProjectTechnology technology;
        private final List<String> indicators = new ArrayList<>();
        private final List<VerificationCommand> commands = new ArrayList<>();
        private NetworkMode networkMode = NetworkMode.DEPENDENCY;
        private boolean enabled = true;
        private String selectionReason;

        private VerificationPlan build() {
            return new VerificationPlan(id, name, technology, List.copyOf(indicators),
                    List.copyOf(commands), networkMode, enabled, selectionReason);
        }
    }

    private static final class VerificationCommandBuilder {
        private String label;
        private String workingDirectory = "${project.path}";
        private String commandDisplay;
        private int timeoutSeconds = 600;
        private boolean optional;

        private VerificationCommand build() {
            return new VerificationCommand(label, workingDirectory, commandDisplay, timeoutSeconds, optional);
        }
    }
}
