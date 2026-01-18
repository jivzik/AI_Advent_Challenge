package de.jivz.agentservice.cli.domain;
import lombok.Builder;
import lombok.Value;
@Value
@Builder
public class Command {
    CommandType type;
    String serviceName;
    String rawInput;
    String language;
    public enum CommandType {
        DEPLOY, STATUS, LOGS, HEALTH, RELEASE_NOTES, CREATE_RELEASE, COMMITS, ROLLBACK, COMMIT, HELP, EXIT, UNKNOWN
    }
    public boolean requiresServiceName() {
        return type == CommandType.DEPLOY || type == CommandType.LOGS || type == CommandType.HEALTH || type == CommandType.ROLLBACK;
    }
    public boolean isExit() {
        return type == CommandType.EXIT;
    }
}
