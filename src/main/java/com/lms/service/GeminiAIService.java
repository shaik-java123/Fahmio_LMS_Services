package com.lms.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiAIService {

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String GEMINI_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=";

    /**
     * Generate an interview question for the given topic, difficulty and question number.
     * Returns JSON string: { "question": "...", "hint": "..." }
     */
    public Map<String, String> generateQuestion(String topic, String difficulty, int questionNumber, int total) {
        String prompt = String.format("""
            You are an expert technical interviewer. Generate interview question number %d out of %d questions.
            
            IMPORTANT:
            - This is question #%d (NOT a repeat of previous questions)
            - Generate a COMPLETELY DIFFERENT question than questions 1 to %d
            - Each question should cover different aspects/subtopics of: %s
            - Do NOT repeat the same question concept
            - Use different question types: theoretical, practical, design-based, etc.
            
            Difficulty Level: %s
            - BEGINNER: Focus on definitions, basic concepts, and simple usage
            - INTERMEDIATE: Focus on implementation details, trade-offs, real-world scenarios, and best practices
            - ADVANCED: Focus on system design, optimization, edge cases, performance, and deep internals
            
            Rules for this specific question #%d:
            - If this is question 1: Ask about core concepts and fundamentals
            - If this is question 2+: Ask about DIFFERENT aspects (implementation, design, optimization, etc.)
            - Be specific and practical
            - Make questions progressively build on knowledge
            
            RESPOND WITH ONLY VALID JSON (no markdown, no extra text):
            {"question": "your full question here", "hint": "a helpful hint without revealing the answer"}
            """, questionNumber, total, questionNumber, questionNumber - 1, topic, difficulty, questionNumber);

        String raw = callGemini(prompt);
        try {
            raw = extractJson(raw);
            JsonNode node = objectMapper.readTree(raw);
            String generatedQuestion = node.path("question").asText();
            String generatedHint = node.path("hint").asText();

            // Ensure we got valid content
            if (generatedQuestion.isEmpty()) {
                generatedQuestion = String.format("Question #%d about %s - %s level: Explain a different aspect of %s not covered in previous questions.",
                    questionNumber, topic, difficulty, topic);
            }
            if (generatedHint.isEmpty()) {
                generatedHint = "Think about the practical application and real-world scenarios.";
            }

            return Map.of(
                "question", generatedQuestion,
                "hint", generatedHint
            );
        } catch (Exception e) {
            log.warn("Failed to parse question JSON for question #{}, using fallback. Raw: {}", questionNumber, raw);
            return Map.of(
                "question", String.format("Explain aspect %d of %s at a %s level. (Question %d of %d)",
                    questionNumber, topic, difficulty, questionNumber, total),
                "hint", "Focus on a different aspect than previous questions."
            );
        }
    }

    /**
     * Evaluate the user's answer and return detailed feedback.
     * Returns JSON: { "score": 75, "feedback": "...", "idealAnswer": "...", "strengths": "...", "improvements": "..." }
     */
    public Map<String, Object> evaluateAnswer(String topic, String difficulty, String question, String userAnswer) {
        if (userAnswer == null || userAnswer.trim().isEmpty()) {
            return Map.of(
                "score", 0,
                "feedback", "No answer was provided. Please provide a detailed answer to receive feedback.",
                "idealAnswer", "A complete answer should address all aspects of the question with relevant examples and explanations.",
                "strengths", "None - No attempt was made.",
                "improvements", "Please attempt to answer the question with at least 2-3 sentences of explanation."
            );
        }

        String prompt = String.format("""
            You are an expert technical interviewer evaluating a candidate's answer.
            
            Topic: %s
            Difficulty: %s
            Question: %s
            Candidate's Answer: %s
            
            Evaluate the answer strictly and fairly. Score from 0-100 where:
            - 90-100: Exceptional, comprehensive, shows deep expertise
            - 70-89: Good, covers main points with minor gaps
            - 50-69: Adequate, some key concepts covered but missing important aspects
            - 30-49: Below average, significant gaps in understanding
            - 0-29: Poor or incorrect answer
            
            Respond ONLY with valid JSON (no markdown, no extra text):
            {
              "score": <integer 0-100>,
              "feedback": "<2-3 sentence constructive feedback about this specific answer>",
              "idealAnswer": "<comprehensive model answer in 3-5 sentences>",
              "strengths": "<what the candidate got right, 1-2 points>",
              "improvements": "<specific areas to improve, 1-2 actionable points>"
            }
            """, topic, difficulty, question, userAnswer);

        String raw = callGemini(prompt);
        try {
            raw = extractJson(raw);
            JsonNode node = objectMapper.readTree(raw);
            return Map.of(
                "score", node.path("score").asInt(50),
                "feedback", node.path("feedback").asText("Good attempt."),
                "idealAnswer", node.path("idealAnswer").asText(""),
                "strengths", node.path("strengths").asText(""),
                "improvements", node.path("improvements").asText("Review the topic further.")
            );
        } catch (Exception e) {
            log.warn("Failed to parse evaluation JSON, using fallback. Raw: {}", raw);
            return Map.of(
                "score", 50,
                "feedback", "Your answer shows some understanding of the topic. Review for completeness.",
                "idealAnswer", "A complete answer would cover the core concepts, practical usage, and common pitfalls.",
                "strengths", "Attempted the answer.",
                "improvements", "Provide more specific examples and deeper explanations."
            );
        }
    }

    /**
     * Generate an overall session summary after all questions are answered.
     */
    public Map<String, Object> generateSessionSummary(String topic, String difficulty, int avgScore,
                                                        java.util.List<String> questions,
                                                        java.util.List<Integer> scores) {
        StringBuilder qa = new StringBuilder();
        for (int i = 0; i < questions.size(); i++) {
            qa.append(String.format("Q%d: %s (Score: %d/100)\n", i + 1, questions.get(i), scores.get(i)));
        }

        String prompt = String.format("""
            You are a senior technical interviewer. Provide a final evaluation summary for a mock interview session.
            
            Topic: %s
            Difficulty: %s
            Average Score: %d/100
            Questions and Scores:
            %s
            
            Respond ONLY with valid JSON (no markdown):
            {
              "overallFeedback": "<3-4 sentence comprehensive summary of the candidate's performance, what they did well and what needs work>",
              "readinessLevel": "<one of: Not Ready, Needs Practice, Almost Ready, Interview Ready, Exceptional>",
              "topStrengths": "<comma-separated 2-3 key strengths demonstrated>",
              "focusAreas": "<comma-separated 2-3 specific topics to study before a real interview>",
              "studyPlan": "<2-3 actionable steps to improve in this topic>"
            }
            """, topic, difficulty, avgScore, qa.toString());

        String raw = callGemini(prompt);
        try {
            raw = extractJson(raw);
            JsonNode node = objectMapper.readTree(raw);
            return Map.of(
                "overallFeedback", node.path("overallFeedback").asText("Good session."),
                "readinessLevel", node.path("readinessLevel").asText("Needs Practice"),
                "topStrengths", node.path("topStrengths").asText(""),
                "focusAreas", node.path("focusAreas").asText(""),
                "studyPlan", node.path("studyPlan").asText("Review the topic documentation.")
            );
        } catch (Exception e) {
            log.warn("Failed to parse summary JSON. Raw: {}", raw);
            return Map.of(
                "overallFeedback", "You completed the mock interview. Review each answer's feedback to improve.",
                "readinessLevel", avgScore >= 70 ? "Almost Ready" : "Needs Practice",
                "topStrengths", "Completed the session",
                "focusAreas", topic,
                "studyPlan", "Review the topic fundamentals and practice more questions."
            );
        }
    }

    private String callGemini(String prompt) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            log.warn("Gemini API key not configured — returning mock response");
            // Generate context-aware mock response based on the prompt content
            if (prompt.contains("\"question\":")) {
                // For question generation requests
                return generateMockQuestionResponse(prompt);
            } else if (prompt.contains("\"feedback\":") || prompt.contains("Candidate's Answer")) {
                // For answer evaluation requests
                return generateMockEvaluationResponse(prompt);
            } else if (prompt.contains("\"overallFeedback\":") || prompt.contains("final evaluation summary")) {
                // For session summary requests
                return generateMockSummaryResponse(prompt);
            }
            // Default fallback
            return "{\"question\":\"Mock question\",\"hint\":\"Mock hint\",\"score\":70,\"feedback\":\"Mock feedback\",\"idealAnswer\":\"Mock ideal answer\",\"strengths\":\"Attempted\",\"improvements\":\"Configure Gemini API key\",\"overallFeedback\":\"Configure your Gemini API key in application.properties\",\"readinessLevel\":\"Needs Practice\",\"topStrengths\":\"N/A\",\"focusAreas\":\"N/A\",\"studyPlan\":\"Add gemini.api.key to application.properties\"}";
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = GEMINI_URL + geminiApiKey;

            String requestBody = String.format("""
                {
                  "contents": [{"parts": [{"text": %s}]}],
                  "generationConfig": {"temperature": 0.7, "maxOutputTokens": 1024}
                }
                """, objectMapper.writeValueAsString(prompt));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText("");
        } catch (Exception e) {
            log.error("Gemini API call failed: {}", e.getMessage());
            return "{}";
        }
    }

    private String extractJson(String raw) {
        if (raw == null) return "{}";
        // Strip markdown code fences if present
        raw = raw.trim();
        if (raw.startsWith("```json")) raw = raw.substring(7);
        else if (raw.startsWith("```")) raw = raw.substring(3);
        if (raw.endsWith("```")) raw = raw.substring(0, raw.length() - 3);
        // Find first { to last }
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start >= 0 && end > start) return raw.substring(start, end + 1);
        return raw.trim();
    }

    /**
     * Generate a context-aware mock question based on the prompt
     */
    private String generateMockQuestionResponse(String prompt) {
        // Extract question number and topic from prompt
        int questionNumber = 1;
        String topic = "Interview Topic";

        if (prompt.contains("question number")) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("question number (\\d+)");
            java.util.regex.Matcher m = p.matcher(prompt);
            if (m.find()) {
                questionNumber = Integer.parseInt(m.group(1));
            }
        }

        // Extract topic from prompt
        if (prompt.contains("about: ")) {
            int idx = prompt.indexOf("about: ");
            int endIdx = prompt.indexOf("\n", idx);
            if (endIdx == -1) endIdx = prompt.indexOf(".", idx);
            if (endIdx > idx) {
                topic = prompt.substring(idx + 7, Math.min(endIdx, idx + 50));
            }
        }

        // Generate different questions based on question number
        String question;
        String hint;

        switch (questionNumber) {
            case 1:
                question = String.format("What are the fundamental concepts and core principles of %s?", topic);
                hint = "Think about the basics, definitions, and foundational understanding.";
                break;
            case 2:
                question = String.format("How would you implement a real-world scenario using %s? Provide a practical example.", topic);
                hint = "Consider a practical use case and explain the implementation step by step.";
                break;
            case 3:
                question = String.format("What are the key design patterns and best practices when working with %s?", topic);
                hint = "Think about common patterns, pitfalls, and optimization strategies.";
                break;
            case 4:
                question = String.format("Explain the performance implications and optimization techniques for %s.", topic);
                hint = "Consider scalability, efficiency, and common bottlenecks.";
                break;
            case 5:
                question = String.format("What are the edge cases and how would you handle errors in %s?", topic);
                hint = "Think about exception handling, boundary conditions, and error recovery.";
                break;
            default:
                question = String.format("Describe an advanced scenario involving %s and how you would approach it. (Question %d)", topic, questionNumber);
                hint = "Think about complex scenarios, trade-offs, and advanced techniques.";
        }

        try {
            return objectMapper.writeValueAsString(Map.of(
                "question", question,
                "hint", hint
            ));
        } catch (Exception e) {
            return "{\"question\":\"" + question + "\",\"hint\":\"" + hint + "\"}";
        }
    }

    /**
     * Generate a context-aware mock evaluation based on the user answer
     */
    private String generateMockEvaluationResponse(String prompt) {
        // Extract answer length and content to determine score
        String userAnswer = "";
        if (prompt.contains("Candidate's Answer: ")) {
            int idx = prompt.indexOf("Candidate's Answer: ");
            int endIdx = prompt.indexOf("\n", idx + 20);
            if (endIdx == -1) endIdx = prompt.length();
            userAnswer = prompt.substring(idx + 20, endIdx);
        }

        // Score based on answer length and presence of keywords
        int score;
        String feedback;
        String strengths;
        String improvements;

        if (userAnswer.length() < 20) {
            score = 30;
            feedback = "Your answer is too brief. Try to provide more detailed explanation with examples.";
            strengths = "Showed effort to attempt the answer.";
            improvements = "Expand your answer with more context, examples, and explanations.";
        } else if (userAnswer.length() < 50) {
            score = 50;
            feedback = "Your answer covers some basic concepts but needs more depth and specific examples.";
            strengths = "You understood the core concept and provided basic explanation.";
            improvements = "Add more practical examples, edge cases, and implementation details.";
        } else if (userAnswer.length() < 100) {
            score = 70;
            feedback = "Good answer with relevant points and some examples. Could be more comprehensive.";
            strengths = "You demonstrated understanding and provided relevant examples.";
            improvements = "Consider discussing performance implications, best practices, or alternative approaches.";
        } else {
            score = 85;
            feedback = "Excellent answer with comprehensive explanation, examples, and good understanding demonstrated.";
            strengths = "Demonstrated deep understanding, provided clear examples, and covered multiple aspects.";
            improvements = "You could discuss edge cases or advanced optimization techniques.";
        }

        String idealAnswer = "A comprehensive answer should include: (1) Core concepts and definitions, " +
            "(2) Practical examples with code or scenarios, (3) Best practices and patterns, " +
            "(4) Performance considerations, and (5) Edge cases or common pitfalls.";

        try {
            return objectMapper.writeValueAsString(Map.of(
                "score", score,
                "feedback", feedback,
                "idealAnswer", idealAnswer,
                "strengths", strengths,
                "improvements", improvements
            ));
        } catch (Exception e) {
            return "{\"score\":" + score + ",\"feedback\":\"" + feedback + "\",\"idealAnswer\":\"" + idealAnswer +
                "\",\"strengths\":\"" + strengths + "\",\"improvements\":\"" + improvements + "\"}";
        }
    }

    /**
     * Generate a context-aware mock session summary based on average score
     */
    private String generateMockSummaryResponse(String prompt) {
        // Extract average score from prompt
        int avgScore = 70;
        if (prompt.contains("Average Score: ")) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("Average Score: (\\d+)");
            java.util.regex.Matcher m = p.matcher(prompt);
            if (m.find()) {
                avgScore = Integer.parseInt(m.group(1));
            }
        }

        String overallFeedback;
        String readinessLevel;
        String topStrengths;
        String focusAreas;
        String studyPlan;

        if (avgScore >= 85) {
            overallFeedback = "Outstanding performance! You demonstrated excellent understanding of the topic with comprehensive answers, " +
                "strong examples, and deep technical knowledge. You are well-prepared for technical interviews.";
            readinessLevel = "Interview Ready";
            topStrengths = "Deep technical knowledge, clear explanations, practical examples, advanced problem-solving";
            focusAreas = "Edge cases, performance optimization, system design tradeoffs";
            studyPlan = "Focus on advanced topics and system design patterns. Practice mock interviews at advanced level.";
        } else if (avgScore >= 70) {
            overallFeedback = "Good performance! You demonstrated solid understanding of the topic with mostly complete answers. " +
                "You have covered most key concepts but could strengthen your answers with more examples and depth.";
            readinessLevel = "Almost Ready";
            topStrengths = "Core concept understanding, practical knowledge, problem-solving approach";
            focusAreas = "Best practices, implementation details, edge case handling, performance considerations";
            studyPlan = "Review best practices and implementation details. Practice more complex scenarios and real-world examples.";
        } else if (avgScore >= 50) {
            overallFeedback = "You showed understanding of some concepts but your answers lacked depth and specific examples. " +
                "You need more practice and study before attempting a real technical interview.";
            readinessLevel = "Needs Practice";
            topStrengths = "Basic understanding, willingness to attempt questions";
            focusAreas = "Fundamental concepts, practical examples, implementation details, pattern recognition";
            studyPlan = "Review topic fundamentals thoroughly. Study real-world examples and case studies. Practice answering with more detail.";
        } else {
            overallFeedback = "You need significant practice in this area. Your answers showed limited understanding of the topic. " +
                "Please review the fundamentals and practice more questions before attempting interviews.";
            readinessLevel = "Not Ready";
            topStrengths = "Attempted to answer all questions";
            focusAreas = "All fundamental concepts, definitions, basic usage patterns, core principles";
            studyPlan = "Start with tutorial content and fundamentals. Study each concept thoroughly. Complete beginner-level practice sets.";
        }

        try {
            return objectMapper.writeValueAsString(Map.of(
                "overallFeedback", overallFeedback,
                "readinessLevel", readinessLevel,
                "topStrengths", topStrengths,
                "focusAreas", focusAreas,
                "studyPlan", studyPlan
            ));
        } catch (Exception e) {
            return "{\"overallFeedback\":\"" + overallFeedback + "\",\"readinessLevel\":\"" + readinessLevel +
                "\",\"topStrengths\":\"" + topStrengths + "\",\"focusAreas\":\"" + focusAreas +
                "\",\"studyPlan\":\"" + studyPlan + "\"}";
        }
    }
}

