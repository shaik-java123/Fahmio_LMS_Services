package com.lms.service;

import com.lms.model.*;
import com.lms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MockInterviewService {

    private final MockInterviewRepository interviewRepository;
    private final InterviewQuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final GeminiAIService geminiAIService;
    private final SpeechToTextService speechToTextService;

    /** Start a new mock interview session (TEXT or VOICE) */
    @Transactional
    public MockInterview startInterview(Long userId, String topic, String difficulty, int totalQuestions, String interviewType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        MockInterview interview = new MockInterview();
        interview.setUser(user);
        interview.setTopic(topic);
        interview.setDifficulty(MockInterview.Difficulty.valueOf(difficulty.toUpperCase()));
        interview.setTotalQuestions(Math.min(Math.max(totalQuestions, 3), 10));
        interview.setStatus(MockInterview.Status.IN_PROGRESS);

        // Set interview type (TEXT or VOICE)
        try {
            interview.setInterviewType(MockInterview.InterviewType.valueOf(interviewType.toUpperCase()));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid interview type: {}, defaulting to TEXT", interviewType);
            interview.setInterviewType(MockInterview.InterviewType.TEXT);
        }

        interview = interviewRepository.save(interview);

        // Generate the first question immediately
        Map<String, String> q = geminiAIService.generateQuestion(topic, difficulty, 1, interview.getTotalQuestions());
        InterviewQuestion firstQ = new InterviewQuestion();
        firstQ.setInterview(interview);
        firstQ.setQuestion(q.get("question"));
        firstQ.setHint(q.get("hint"));
        firstQ.setOrderIndex(1);
        questionRepository.save(firstQ);

        return interviewRepository.findById(interview.getId()).orElse(interview);
    }

    /** Submit an answer for the current question, get AI evaluation, then get the next question */
    @Transactional
    public Map<String, Object> submitAnswer(Long interviewId, Long questionId, String userAnswer) {
        MockInterview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new RuntimeException("Interview not found"));

        InterviewQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        // Evaluate the answer via AI
        Map<String, Object> evaluation = geminiAIService.evaluateAnswer(
                interview.getTopic(),
                interview.getDifficulty().name(),
                question.getQuestion(),
                userAnswer
        );

        question.setUserAnswer(userAnswer);
        question.setScore((Integer) evaluation.get("score"));
        question.setFeedback((String) evaluation.get("feedback"));
        question.setIdealAnswer((String) evaluation.get("idealAnswer"));
        question.setStrengths((String) evaluation.get("strengths"));
        question.setImprovements((String) evaluation.get("improvements"));
        question.setAnsweredAt(LocalDateTime.now());
        questionRepository.save(question);

        // Determine next action
        int currentIndex = question.getOrderIndex();
        int total = interview.getTotalQuestions();

        Map<String, Object> result = new java.util.HashMap<>(evaluation);
        result.put("questionIndex", currentIndex);
        result.put("totalQuestions", total);
        result.put("isLast", currentIndex >= total);

        if (currentIndex < total) {
            // Generate next question
            Map<String, String> nextQ = geminiAIService.generateQuestion(
                    interview.getTopic(), interview.getDifficulty().name(), currentIndex + 1, total);
            InterviewQuestion nextQuestion = new InterviewQuestion();
            nextQuestion.setInterview(interview);
            nextQuestion.setQuestion(nextQ.get("question"));
            nextQuestion.setHint(nextQ.get("hint"));
            nextQuestion.setOrderIndex(currentIndex + 1);
            nextQuestion = questionRepository.save(nextQuestion);
            result.put("nextQuestion", Map.of("id", nextQuestion.getId(), "question", nextQuestion.getQuestion(), "hint", nextQuestion.getHint(), "orderIndex", nextQuestion.getOrderIndex()));
        }

        return result;
    }

    /** Submit a voice-based answer, transcribe it, and get AI evaluation */
    @Transactional
    public Map<String, Object> submitVoiceAnswer(Long interviewId, Long questionId, String voiceAnswerUrl,
                                                  Integer voiceDurationSeconds, String preTranscription) {
        MockInterview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new RuntimeException("Interview not found"));

        InterviewQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        // Store voice answer URL and duration
        question.setVoiceAnswerUrl(voiceAnswerUrl);
        question.setVoiceDurationSeconds(voiceDurationSeconds);

        // Transcribe voice to text (if not already provided)
        String transcription = preTranscription != null && !preTranscription.isEmpty()
            ? preTranscription
            : speechToTextService.transcribeAudio(voiceAnswerUrl, "en-US");

        question.setVoiceTranscription(transcription);
        question.setUserAnswer(transcription); // Store transcription as user answer for evaluation

        log.info("Voice answer transcribed. Length: {} characters, Duration: {} seconds",
                transcription.length(), voiceDurationSeconds);

        // Evaluate the transcribed answer via AI
        Map<String, Object> evaluation = geminiAIService.evaluateAnswer(
                interview.getTopic(),
                interview.getDifficulty().name(),
                question.getQuestion(),
                transcription
        );

        question.setScore((Integer) evaluation.get("score"));
        question.setFeedback((String) evaluation.get("feedback"));
        question.setIdealAnswer((String) evaluation.get("idealAnswer"));
        question.setStrengths((String) evaluation.get("strengths"));
        question.setImprovements((String) evaluation.get("improvements"));
        question.setAnsweredAt(LocalDateTime.now());
        questionRepository.save(question);

        // Determine next action
        int currentIndex = question.getOrderIndex();
        int total = interview.getTotalQuestions();

        Map<String, Object> result = new java.util.HashMap<>(evaluation);
        result.put("questionIndex", currentIndex);
        result.put("totalQuestions", total);
        result.put("isLast", currentIndex >= total);
        result.put("transcription", transcription); // Include transcription in response
        result.put("voiceDurationSeconds", voiceDurationSeconds);

        if (currentIndex < total) {
            // Generate next question
            Map<String, String> nextQ = geminiAIService.generateQuestion(
                    interview.getTopic(), interview.getDifficulty().name(), currentIndex + 1, total);
            InterviewQuestion nextQuestion = new InterviewQuestion();
            nextQuestion.setInterview(interview);
            nextQuestion.setQuestion(nextQ.get("question"));
            nextQuestion.setHint(nextQ.get("hint"));
            nextQuestion.setOrderIndex(currentIndex + 1);
            nextQuestion = questionRepository.save(nextQuestion);
            result.put("nextQuestion", Map.of("id", nextQuestion.getId(), "question", nextQuestion.getQuestion(), "hint", nextQuestion.getHint(), "orderIndex", nextQuestion.getOrderIndex()));
        }

        return result;
    }
    @Transactional
    public MockInterview completeInterview(Long interviewId) {
        MockInterview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new RuntimeException("Interview not found"));

        List<InterviewQuestion> questions = questionRepository.findByInterviewIdOrderByOrderIndex(interviewId);
        List<String> questionTexts = new ArrayList<>();
        List<Integer> scores = new ArrayList<>();

        int totalScore = 0;
        int answered = 0;
        for (InterviewQuestion q : questions) {
            if (q.getScore() != null) {
                totalScore += q.getScore();
                answered++;
                questionTexts.add(q.getQuestion());
                scores.add(q.getScore());
            }
        }

        int avgScore = answered > 0 ? totalScore / answered : 0;
        interview.setOverallScore(avgScore);
        interview.setStatus(MockInterview.Status.COMPLETED);
        interview.setCompletedAt(LocalDateTime.now());

        // Generate overall summary
        Map<String, Object> summary = geminiAIService.generateSessionSummary(
                interview.getTopic(), interview.getDifficulty().name(), avgScore, questionTexts, scores);

        String overallFeedback = String.format("%s\n\nReadiness: %s\nStrengths: %s\nFocus Areas: %s\nStudy Plan: %s",
                summary.get("overallFeedback"), summary.get("readinessLevel"),
                summary.get("topStrengths"), summary.get("focusAreas"), summary.get("studyPlan"));

        interview.setOverallFeedback(overallFeedback);
        return interviewRepository.save(interview);
    }

    @Transactional(readOnly = true)
    public List<MockInterview> getUserInterviews(Long userId) {
        return interviewRepository.findByUserIdOrderByStartedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public MockInterview getInterviewById(Long id) {
        return interviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Interview not found"));
    }

    @Transactional(readOnly = true)
    public List<InterviewQuestion> getInterviewQuestions(Long interviewId) {
        return questionRepository.findByInterviewIdOrderByOrderIndex(interviewId);
    }

    @Transactional
    public void abandonInterview(Long interviewId) {
        interviewRepository.findById(interviewId).ifPresent(i -> {
            i.setStatus(MockInterview.Status.ABANDONED);
            interviewRepository.save(i);
        });
    }
}

