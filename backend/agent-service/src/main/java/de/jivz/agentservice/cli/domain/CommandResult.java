package de.jivz.agentservice.cli.domain;
import lombok.Builder;
import lombok.Value;
import java.time.LocalDateTime;
@Value
@Builder
public class CommandResult {
    boolean success;
    String message;
    String details;
    LocalDateTime timestamp;
    Exception error;
    public static CommandResult success(String message) {
        return CommandResult.builder().success(true).message(message).timestamp(LocalDateTime.now()).build();
    }
    public static CommandResult success(String message, String details) {
        return CommandResult.builder().success(true).message(message).details(details).timestamp(LocalDateTime.now()).build();
    }
    public static CommandResult failure(String message, Exception error) {
        return CommandResult.builder().success(false).message(message).error(error).timestamp(LocalDateTime.now()).build();
    }
    public static CommandResult failure(String message) {
        return CommandResult.builder().success(false).message(message).timestamp(LocalDateTime.now()).build();
    }
}
