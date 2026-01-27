package de.jivz.ai_challenge.openrouterservice.controller;

import de.jivz.ai_challenge.openrouterservice.dto.VoiceAgentRequest;
import de.jivz.ai_challenge.openrouterservice.dto.VoiceAgentResponse;
import de.jivz.ai_challenge.openrouterservice.dto.WhisperResponse;
import de.jivz.ai_challenge.openrouterservice.service.VoiceAgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/voice")
@RequiredArgsConstructor
@Tag(name = "Voice Agent", description = "Voice Agent API - Audio zu Text zu LLM Pipeline")
public class VoiceAgentController {

    private final VoiceAgentService voiceAgentService;

    /**
     * Vollständiger Voice Agent Pipeline
     * Audio → Speech-to-Text → LLM → Text Response
     */
    @PostMapping(value = "/process", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Verarbeite Sprachbefehl",
            description = "Nimmt eine Audiodatei entgegen, transkribiert sie mit Whisper und verarbeitet sie mit LLM"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Erfolgreich verarbeitet",
            content = @Content(schema = @Schema(implementation = VoiceAgentResponse.class))
    )
    @ApiResponse(responseCode = "400", description = "Ungültige Eingabe")
    @ApiResponse(responseCode = "500", description = "Serverfehler")
    public ResponseEntity<VoiceAgentResponse> processVoiceCommand(
            @RequestPart("audio") MultipartFile audioFile,  // ИЗМЕНЕНО: @RequestPart вместо @RequestParam
            @RequestParam("userId") String userId,
            @RequestParam(value = "language", required = false) String language,
            @RequestParam(value = "model", required = false) String model,
            @RequestParam(value = "temperature", required = false) Double temperature,
            @RequestParam(value = "systemPrompt", required = false) String systemPrompt
    ) {
        log.info("Received voice command - User: {}, File: {}, Size: {} bytes, ContentType: {}",
                userId, audioFile.getOriginalFilename(), audioFile.getSize(), audioFile.getContentType());

        try {
            // Validierung
            if (audioFile.isEmpty()) {
                log.warn("Empty audio file received");
                return ResponseEntity.badRequest().build();
            }

            // Request zusammenstellen
            VoiceAgentRequest request = VoiceAgentRequest.builder()
                    .userId(userId)
                    .language(language)
                    .model(model)
                    .temperature(temperature)
                    .systemPrompt(systemPrompt)
                    .build();

            // Processing
            VoiceAgentResponse response = voiceAgentService.processVoiceCommand(audioFile, request);

            log.info("Voice command processed successfully in {} ms", response.getTotalTimeMs());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid input: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error processing voice command", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Nur Transkription (für Debugging/Testing)
     */
    @PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Transkribiere Audio",
            description = "Nur Speech-to-Text ohne LLM-Verarbeitung (für Debugging)"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Erfolgreich transkribiert",
            content = @Content(schema = @Schema(implementation = WhisperResponse.class))
    )
    public ResponseEntity<WhisperResponse> transcribeAudio(
            @RequestPart("audio") MultipartFile audioFile,  // ИЗМЕНЕНО: @RequestPart
            @RequestParam(value = "language", required = false) String language
    ) {
        log.info("Received transcription request - File: {}, Size: {} bytes, ContentType: {}",
                audioFile.getOriginalFilename(), audioFile.getSize(), audioFile.getContentType());

        try {
            if (audioFile.isEmpty()) {
                log.warn("Empty audio file received");
                return ResponseEntity.badRequest().build();
            }

            WhisperResponse response = voiceAgentService.transcribeAudio(audioFile, language);

            log.info("Transcription successful: {} characters", response.getText().length());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid input: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error transcribing audio", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Health Check
     */
    @GetMapping("/health")
    @Operation(summary = "Health Check", description = "Prüft, ob der Voice Agent verfügbar ist")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Voice Agent is running");
    }
}