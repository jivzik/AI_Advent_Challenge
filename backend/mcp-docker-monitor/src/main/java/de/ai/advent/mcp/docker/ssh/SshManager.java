package de.ai.advent.mcp.docker.ssh;

import de.ai.advent.mcp.docker.config.SshConfig;
import de.ai.advent.mcp.docker.exception.SshConnectionException;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * SSH Manager using Apache MINA SSHD
 *
 * Handles SSH connections to remote servers and command execution.
 * Supports modern algorithms: ED25519, RSA SHA256/512
 * Works with PEM and OpenSSH key formats
 */
@Slf4j
@Component
public class SshManager {

    private final SshConfig sshConfig;
    private final Map<String, ClientSession> activeSessions = new HashMap<>();
    private SshClient sshClient;

    public SshManager(SshConfig sshConfig) {
        this.sshConfig = sshConfig;
        log.info("SshManager initialized with Apache MINA SSHD (supports ED25519, RSA SHA256+)");
    }


    /**
     * Connects to the remote SSH server using Apache MINA SSHD
     * Supports ED25519, RSA SHA256/512 and other modern algorithms
     *
     * @throws SshConnectionException if connection fails
     */
    public void connect() throws SshConnectionException {
        if (!sshConfig.isValid()) {
            String errorMsg = "SSH configuration is not valid. Please configure ssh.host, ssh.username, and ssh.privateKeyPath";
            log.error(errorMsg);
            throw new SshConnectionException(errorMsg);
        }

        String sessionKey = getSessionKey();

        // Return if already connected (connection pooling)
        if (activeSessions.containsKey(sessionKey)) {
            ClientSession existingSession = activeSessions.get(sessionKey);
            if (existingSession.isOpen()) {
                log.debug("Reusing existing SSH session: {}", sessionKey);
                return;
            } else {
                activeSessions.remove(sessionKey);
            }
        }

        try {
            log.debug("Initiating SSH connection to {}@{}:{} using Apache MINA SSHD",
                    sshConfig.getUsername(), sshConfig.getHost(), sshConfig.getPort());

            // Initialize SSH client if needed
            if (sshClient == null || sshClient.isClosed()) {
                sshClient = SshClient.setUpDefaultClient();

                // Accept all host keys (disable strict host key checking)
                sshClient.setServerKeyVerifier((clientSession, remoteAddress, serverKey) -> true);

                sshClient.start();
                log.debug("SSH client started");
            };

            // Validate private key file exists
            if (!Files.exists(Paths.get(sshConfig.getPrivateKeyPath()))) {
                String errorMsg = "Private key file not found: " + sshConfig.getPrivateKeyPath();
                log.error(errorMsg);
                throw new SshConnectionException(errorMsg);
            }

            log.debug("Loading private key from: {}", sshConfig.getPrivateKeyPath());

            // Load private key (supports PEM and OpenSSH formats automatically)
            KeyPair keyPair = loadPrivateKey(sshConfig.getPrivateKeyPath());

            if (keyPair == null) {
                throw new SshConnectionException("Failed to load private key from: " + sshConfig.getPrivateKeyPath());
            }

            log.debug("Private key loaded successfully");

            // Connect to SSH server
            log.debug("Connecting to {}:{}", sshConfig.getHost(), sshConfig.getPort());

            ClientSession session = sshClient.connect(
                    sshConfig.getUsername(),
                    sshConfig.getHost(),
                    sshConfig.getPort()
            ).verify(sshConfig.getConnectionTimeout(), TimeUnit.MILLISECONDS).getSession();

            log.debug("SSH connection established, authenticating...");

            // Add public key identity for authentication
            session.addPublicKeyIdentity(keyPair);

            // Authenticate
            session.auth().verify(sshConfig.getConnectionTimeout(), TimeUnit.MILLISECONDS);

            // Store session
            activeSessions.put(sessionKey, session);

            log.info("Successfully connected to SSH server: {}@{}:{} using Apache MINA SSHD",
                    sshConfig.getUsername(), sshConfig.getHost(), sshConfig.getPort());

        } catch (IOException e) {
            String errorMsg = String.format("Failed to establish SSH connection to %s@%s:%d",
                    sshConfig.getUsername(), sshConfig.getHost(), sshConfig.getPort());
            log.error(errorMsg, e);
            throw new SshConnectionException(errorMsg, e);
        }
    }

    /**
     * Executes a command on the remote SSH server
     *
     * @param command the command to execute
     * @return command output (stdout)
     * @throws SshConnectionException if connection fails
     * @throws IOException if I/O error occurs
     */
    public String executeCommand(String command) throws SshConnectionException, IOException {
        String sessionKey = getSessionKey();
        ClientSession session = activeSessions.get(sessionKey);

        if (session == null || !session.isOpen()) {
            log.debug("No active SSH connection. Attempting to reconnect...");
            try {
                connect();
                session = activeSessions.get(sessionKey);
            } catch (SshConnectionException e) {
                String errorMsg = "SSH connection not available for: " + sessionKey;
                log.error(errorMsg, e);
                throw new SshConnectionException(errorMsg, e);
            }
        }

        org.apache.sshd.client.channel.ChannelExec channel = null;
        try {
            log.debug("Executing SSH command: {}", command);

            // Create and open channel for command execution
            channel = session.createExecChannel(command);
            channel.open().verify(sshConfig.getCommandTimeout(), TimeUnit.MILLISECONDS);

            // Read output from command (stdout)
            InputStream in = channel.getInvertedOut();
            StringBuilder output = new StringBuilder();
            byte[] tmp = new byte[1024];
            int read;

            // Read all available data with timeout
            long endTime = System.currentTimeMillis() + sshConfig.getCommandTimeout();
            while ((read = in.read(tmp)) != -1 && System.currentTimeMillis() < endTime) {
                output.append(new String(tmp, 0, read));
            }


            log.debug("Command executed successfully, output length: {} bytes", output.length());
            return output.toString();

        } catch (IOException e) {
            String errorMsg = "Failed to execute command on SSH server: " + command;
            log.error(errorMsg, e);
            throw new SshConnectionException(errorMsg, e);
        } finally {
            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException e) {
                    log.debug("Error closing channel", e);
                }
            }
        }
    }

    /**
     * Disconnects from the remote SSH server
     */
    public void disconnect() {
        String sessionKey = getSessionKey();
        ClientSession session = activeSessions.remove(sessionKey);

        if (session != null && session.isOpen()) {
            try {
                session.close();
                log.info("Disconnected from SSH server: {}", sessionKey);
            } catch (IOException e) {
                log.warn("Error closing SSH session: {}", sessionKey, e);
            }
        }
    }

    /**
     * Disconnects all active SSH sessions and stops SSH client
     */
    public void disconnectAll() {
        for (Map.Entry<String, ClientSession> entry : activeSessions.entrySet()) {
            ClientSession session = entry.getValue();
            if (session != null && session.isOpen()) {
                try {
                    session.close();
                    log.info("Disconnected from SSH server: {}", entry.getKey());
                } catch (IOException e) {
                    log.error("Error disconnecting from SSH server: {}", entry.getKey(), e);
                }
            }
        }
        activeSessions.clear();

        // Stop SSH client
        if (sshClient != null && !sshClient.isClosed()) {
            try {
                sshClient.stop();
                log.info("SSH client stopped");
            } catch (Exception e) {
                log.error("Error stopping SSH client", e);
            }
        }

        log.info("Disconnected from all SSH servers");
    }

    /**
     * Checks if the SSH connection is active
     *
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        String sessionKey = getSessionKey();
        ClientSession session = activeSessions.get(sessionKey);
        boolean connected = session != null && session.isOpen();
        log.debug("SSH connection status for {}: {}", sessionKey, connected);
        return connected;
    }

    /**
     * Cleanup on application shutdown
     */
    @PreDestroy
    public void cleanup() {
        log.info("Cleaning up SSH resources");
        disconnectAll();
    }

    /**
     * Loads a private key from file in PEM or OpenSSH format
     *
     * @param keyPath path to the private key file
     * @return KeyPair or null if loading fails
     */
    private KeyPair loadPrivateKey(String keyPath) {
        try {
            log.debug("Loading private key from: {}", keyPath);

            FileKeyPairProvider provider = new FileKeyPairProvider(new File(keyPath).toPath());
            Iterable<KeyPair> keyPairs = provider.loadKeys(null);

            for (KeyPair keyPair : keyPairs) {
                if (keyPair != null) {
                    log.info("✅ Loaded key: algorithm={}, format={}",
                            keyPair.getPublic().getAlgorithm(),
                            keyPair.getPublic().getFormat());
                    log.info("✅ Public key fingerprint (for verification): {}",
                            java.util.Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
                    return keyPair;
                }
            }

            log.error("No valid private key found in file: {}", keyPath);
            return null;
        } catch (Exception e) {
            log.error("Error loading private key from {}: {}", keyPath, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Generates a unique session key based on SSH configuration
     *
     * @return session key string
     */
    private String getSessionKey() {
        return String.format("%s@%s:%d", sshConfig.getUsername(), sshConfig.getHost(), sshConfig.getPort());
    }
}

