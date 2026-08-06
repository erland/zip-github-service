package info.isaksson.erland.zipbuildserver.worker;

public interface CommandExecutor {
    CommandExecutionResult execute(CommandExecutionRequest request);
}
