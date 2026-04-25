package com.lms.dto;

import lombok.Data;

/**
 * Request to submit a voice-based answer for an interview question
 * Includes audio file reference and metadata
 */
@Data
public class SubmitVoiceAnswerRequest {

    /** Interview ID */
    private Long interviewId;

    /** Question ID being answered */
    private Long questionId;

    /** URL/path to the uploaded audio file (in storage) */
    private String voiceAnswerUrl;

    /** Duration of the voice answer in seconds */
    private Integer voiceDurationSeconds;

    /** Optional: Pre-transcribed text (if client-side transcription was done) */
    private String voiceTranscription;
}

