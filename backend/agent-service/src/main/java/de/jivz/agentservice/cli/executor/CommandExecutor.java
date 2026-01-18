package de.jivz.agentservice.cli.executor;
import de.jivz.agentservice.cli.domain.Command;
import de.jivz.agentservice.cli.domain.CommandResult;
import reactor.core.publisher.Mono;
public interface CommandExecutor {
    boolean canExecute(Command command);
    Mono<CommandResult> execute(Command command);
}
