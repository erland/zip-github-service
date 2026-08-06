package info.isaksson.erland.zipbuildserver.application.run;

import info.isaksson.erland.zipbuildserver.worker.CommandExecutionResult;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class FailureClassificationService {
    public String category(CommandExecutionResult result) {
        if (result.timedOut()) {
            return "timeout";
        }
        String output = (result.stdout() + "\n" + result.stderr() + "\n" + result.failureMessage()).toLowerCase();
        if (output.contains("compilation") || output.contains("compile")) {
            return "compilation";
        }
        if (output.contains("test") || output.contains("assert")) {
            return "test";
        }
        if (output.contains("dependency") || output.contains("could not resolve") || output.contains("npm err")) {
            return "dependency";
        }
        return "command_failure";
    }

    public String message(CommandExecutionResult result) {
        if (result.failureMessage() != null && !result.failureMessage().isBlank()) {
            return result.failureMessage();
        }
        if (result.timedOut()) {
            return "Command timed out.";
        }
        return "Command exited with status " + result.exitCode() + ".";
    }
}
