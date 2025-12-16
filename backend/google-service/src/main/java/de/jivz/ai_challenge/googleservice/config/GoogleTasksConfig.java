package de.jivz.ai_challenge.googleservice.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.tasks.Tasks;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Configuration
@Slf4j
public class GoogleTasksConfig {

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TASKS_SCOPE = "https://www.googleapis.com/auth/tasks";

    @Value("${google.credentials.file-path:credentials.json}")
    private String credentialsFilePath;

    @Value("${google.application-name:Google Tasks Service}")
    private String applicationName;

    @Bean
    public Tasks tasksClient() throws GeneralSecurityException, IOException {
        log.info("Initialisierung Google Tasks Client mit credentials: {}", credentialsFilePath);

        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        // Lade credentials aus JSON file (Service Account)
        GoogleCredentials credentials = GoogleCredentials
                .fromStream(new FileInputStream(credentialsFilePath))
                .createScoped(Collections.singleton(TASKS_SCOPE));

        return new Tasks.Builder(httpTransport, JSON_FACTORY, new HttpCredentialsAdapter(credentials))
                .setApplicationName(applicationName)
                .build();
    }
}
