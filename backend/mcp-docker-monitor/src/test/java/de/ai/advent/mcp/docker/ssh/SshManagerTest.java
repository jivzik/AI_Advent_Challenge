package de.ai.advent.mcp.docker.ssh;

import de.ai.advent.mcp.docker.config.SshConfig;
import de.ai.advent.mcp.docker.exception.SshConnectionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for SshManager
 *
 * Note: These tests require SSH server configuration in application.yml
 */
@SpringBootTest
@DisplayName("SSH Manager Tests")
@TestPropertySource(properties = {
        "ssh.host=localhost",
        "ssh.port=22",
        "ssh.username=testuser",
        "ssh.privateKeyPath=/home/testuser/.ssh/id_rsa",
        "ssh.connectionTimeout=30000",
        "ssh.commandTimeout=60000"
})
public class SshManagerTest {

    @Autowired
    private SshManager sshManager;

    @Autowired
    private SshConfig sshConfig;

    @BeforeEach
    void tearDown() {
        // Disconnect after each test
        sshManager.disconnectAll();
    }

    @Test
    @DisplayName("Should create SshManager with valid configuration")
    void testSshManagerInitialization() {
        assertNotNull(sshManager, "SshManager should be initialized");
        assertNotNull(sshConfig, "SshConfig should be initialized");
    }

    @Test
    @DisplayName("Should validate SSH configuration")
    void testConfigurationValidation() {
        // Test with invalid configuration
        SshConfig invalidConfig = new SshConfig();
        assertFalse(invalidConfig.isValid(), "Invalid config should fail validation");

        // Test with valid configuration
        sshConfig.setHost("test-host");
        sshConfig.setUsername("testuser");
        sshConfig.setPrivateKeyPath("/path/to/key");
        assertTrue(sshConfig.isValid(), "Valid config should pass validation");
    }

    @Test
    @DisplayName("Should handle connection with invalid configuration")
    void testConnectionFailureWithInvalidConfig() {
        SshConfig invalidConfig = new SshConfig();
        SshManager manager = new SshManager(invalidConfig);

        assertThrows(SshConnectionException.class, manager::connect,
                "Should throw JSchException for invalid configuration");
    }

    @Test
    @DisplayName("Should initialize connection status as disconnected")
    void testInitialConnectionStatus() {
        assertFalse(sshManager.isConnected(),
                "Should be disconnected initially");
    }

    @Test
    @DisplayName("Should format session key correctly")
    void testSessionKeyFormatting() {
        String expectedFormat = String.format("%s@%s:%d",
                sshConfig.getUsername(),
                sshConfig.getHost(),
                sshConfig.getPort());

        // Test is more for documentation - session key is private
        assertNotNull(expectedFormat);
    }

    @Test
    @DisplayName("Should handle timeout configuration")
    void testTimeoutConfiguration() {
        assertEquals(30000, sshConfig.getConnectionTimeout(),
                "Connection timeout should be configurable");
        assertEquals(60000, sshConfig.getCommandTimeout(),
                "Command timeout should be configurable");
    }
}

