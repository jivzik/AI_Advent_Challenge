package de.jivz.ai_challenge.googleservice.service;


import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.FileReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class GoogleTasksRestService {

    private static final String TASKS_API_URL = "https://tasks.googleapis.com/tasks/v1";
    private static final String AUTH_URL = "https://oauth2.googleapis.com/token";

    private final RestTemplate restTemplate = new RestTemplate();
    private String accessToken;

    /**
     * Holt Access Token von Google OAuth2
     */
    private String getAccessToken() throws Exception {
        if (accessToken != null) {
            return accessToken;
        }

        // Lese credentials.json
        Gson gson = new Gson();
        JsonObject credentials = gson.fromJson(new FileReader("credentials.json"), JsonObject.class);

        String clientEmail = credentials.get("client_email").getAsString();
        String privateKey = credentials.get("private_key").getAsString()
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        // Erstelle JWT
        long now = System.currentTimeMillis() / 1000;
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes());

        String claim = String.format(
                "{\"iss\":\"%s\",\"scope\":\"https://www.googleapis.com/auth/tasks\",\"aud\":\"%s\",\"exp\":%d,\"iat\":%d}",
                clientEmail, AUTH_URL, now + 3600, now
        );
        String claimBase64 = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(claim.getBytes());

        // Für Production: richtige JWT Signatur mit Private Key
        // Hier vereinfacht - in Produktion JWT Library verwenden!
        String jwt = header + "." + claimBase64 + ".signature";

        // Hole Access Token
        String body = "grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=" + jwt;

        URL url = new URL(AUTH_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        // Parse Response
        JsonObject response = gson.fromJson(new FileReader("credentials.json"), JsonObject.class);
        accessToken = response.get("access_token").getAsString();

        log.info("Access Token erhalten");
        return accessToken;
    }

    /**
     * Hole alle Task Lists
     * GET https://tasks.googleapis.com/tasks/v1/users/@me/lists
     */
    public Map<String, Object> getAllTaskLists() throws Exception {
        String token = getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<Map> response = restTemplate.exchange(
                TASKS_API_URL + "/users/@me/lists",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
        );

        return response.getBody();
    }

    /**
     * Hole Tasks aus einer Liste
     * GET https://tasks.googleapis.com/tasks/v1/lists/{taskListId}/tasks
     */
    public Map<String, Object> getTasks(String taskListId) throws Exception {
        String token = getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<Map> response = restTemplate.exchange(
                TASKS_API_URL + "/lists/" + taskListId + "/tasks",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
        );

        return response.getBody();
    }

    /**
     * Erstelle neue Task
     * POST https://tasks.googleapis.com/tasks/v1/lists/{taskListId}/tasks
     */
    public Map<String, Object> createTask(String taskListId, Map<String, String> task) throws Exception {
        String token = getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("Content-Type", "application/json");

        ResponseEntity<Map> response = restTemplate.exchange(
                TASKS_API_URL + "/lists/" + taskListId + "/tasks",
                HttpMethod.POST,
                new HttpEntity<>(task, headers),
                Map.class
        );

        return response.getBody();
    }
}
