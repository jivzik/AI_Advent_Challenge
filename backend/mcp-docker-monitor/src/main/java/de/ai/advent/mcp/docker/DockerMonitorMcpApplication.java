package de.ai.advent.mcp.docker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MCP Docker Monitor Application
 *
 * Main entry point for the Docker monitoring MCP server.
 * Provides tools for remote Docker container monitoring through SSH.
 */
@SpringBootApplication
public class DockerMonitorMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(DockerMonitorMcpApplication.class, args);
    }
}

