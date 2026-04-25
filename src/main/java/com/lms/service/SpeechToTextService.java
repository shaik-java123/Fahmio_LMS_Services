package com.lms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for converting voice/speech to text
 * Uses Google Cloud Speech-to-Text API or fallback transcription method
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SpeechToTextService {

    @Value("${google.cloud.speech.enabled:false}")
    private boolean speechEnabled;

    @Value("${google.cloud.project.id:}")
    private String projectId;

    /**
     * Transcribe audio file to text
     * @param audioFilePath Path to the audio file
     * @param languageCode Language code (e.g., "en-US")
     * @return Transcribed text
     */
    public String transcribeAudio(String audioFilePath, String languageCode) {
        if (!speechEnabled) {
            log.warn("Speech-to-Text is disabled in configuration");
            return ""; // Return empty - client should provide transcription
        }

        try {
            return transcribeAudioUsingGoogleCloud(audioFilePath, languageCode);
        } catch (Exception e) {
            log.error("Error transcribing audio: {}", e.getMessage(), e);
            return ""; // Return empty if transcription fails
        }
    }

    /**
     * Transcribe audio using Google Cloud Speech API
     * Note: This method requires Google Cloud credentials to be configured
     */
    private String transcribeAudioUsingGoogleCloud(String audioFilePath, String languageCode) throws Exception {
        // Placeholder implementation
        // In production, this would use Google Cloud Speech-to-Text API
        // For now, return empty string - frontend should handle transcription

        log.info("Google Cloud Speech-to-Text transcription requested for: {}", audioFilePath);

        // TODO: Implement actual Google Cloud Speech API integration
        // This requires:
        // 1. Google Cloud account setup
        // 2. Speech-to-Text API enabled
        // 3. Service account credentials configured
        // 4. Adding google-cloud-speech dependency

        return "";
    }

    /**
     * Check if speech-to-text is enabled
     */
    public boolean isSpeechToTextEnabled() {
        return speechEnabled;
    }

    /**
     * Alternative: Process transcription that's already provided by client
     * (Client-side speech recognition can be done using Web Speech API)
     */
    public String processClientTranscription(String voiceTranscription) {
        if (voiceTranscription == null || voiceTranscription.isEmpty()) {
            log.warn("Empty transcription provided");
            return "";
        }

        // Clean up transcription if needed
        String cleaned = voiceTranscription.trim();
        log.info("Processing client transcription. Length: {} characters", cleaned.length());
        return cleaned;
    }
}



