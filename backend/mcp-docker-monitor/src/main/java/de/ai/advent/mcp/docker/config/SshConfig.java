package de.ai.advent.mcp.docker.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * SSH Configuration Properties
 *
 * Loaded from application.yml under 'ssh' prefix
 */
@Data
@Component
@ConfigurationProperties(prefix = "ssh")
public class SshConfig {

    /**
     * Remote server host
     */
    private String host;

    /**
     * SSH port (default 22)
     */
    private int port;

    /**
     * SSH username
     */
    private String username;

    /**
     * Path to private SSH key file
     */
    private String privateKeyPath;

    /**
     * Path to known_hosts file (optional)
     */
    private String knownHostsPath;

    /**
     * Connection timeout in milliseconds
     */
    private long connectionTimeout;

    /**
     * Command execution timeout in milliseconds
     */
    private long commandTimeout;

    /**
     * Validate configuration
     *
     * @return true if configuration is valid
     */
    public boolean isValid() {
        return host != null && !host.isEmpty() &&
                username != null && !username.isEmpty() &&
                privateKeyPath != null && !privateKeyPath.isEmpty();
    }
}

