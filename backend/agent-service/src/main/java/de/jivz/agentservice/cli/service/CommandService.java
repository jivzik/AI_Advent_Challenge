package de.jivz.agentservice.cli.service;

import de.jivz.agentservice.cli.domain.Command;
import de.jivz.agentservice.cli.domain.CommandResult;
import de.jivz.agentservice.cli.executor.CommandExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Command orchestration service.
 * Single Responsibility: Route commands to appropriate executors.
 * Open/Closed Principle: New executors can be added without modifying this class.
 */
@Service
@Slf4j
public class CommandService {

    private final List<CommandExecutor> executors;

    public CommandService(List<CommandExecutor> executors) {
        this.executors = executors;
        log.info("Command Service initialized with {} executors", executors.size());
    }

    /**
     * Execute a command by routing it to the appropriate executor
     */
    public Mono<CommandResult> execute(Command command) {
        log.debug("Executing command: {}", command.getType());

        // Handle HELP and EXIT internally
        if (command.getType() == Command.CommandType.HELP) {
            return Mono.just(CommandResult.success("Help", ""));
        }

        if (command.getType() == Command.CommandType.EXIT) {
            return Mono.just(CommandResult.success("Goodbye!"));
        }

        if (command.getType() == Command.CommandType.UNKNOWN) {
            return Mono.just(CommandResult.failure(
                "Unknown command. Type 'help' for available commands."));
        }

        // Find appropriate executor
        return executors.stream()
            .filter(executor -> executor.canExecute(command))
            .findFirst()
            .map(executor -> {
                log.info("Routing command {} to executor: {}",
                    command.getType(),
                    executor.getClass().getSimpleName());
                return executor.execute(command);
            })
            .orElseGet(() -> {
                log.warn("No executor found for command: {}", command.getType());
                return Mono.just(CommandResult.failure(
                    "Command not implemented: " + command.getType()));
            });
    }

    /**
     * Validate if a command requires additional parameters
     */
    public boolean validate(Command command) {
        if (command.requiresServiceName() && command.getServiceName() == null) {
            log.warn("Command {} requires service name", command.getType());
            return false;
        }
        return true;
    }
}

