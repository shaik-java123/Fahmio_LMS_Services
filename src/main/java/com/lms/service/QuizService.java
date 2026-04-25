package com.lms.service;

import com.lms.dto.QuizRequest;
import com.lms.dto.QuizSubmitRequest;
import com.lms.model.*;
import com.lms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;

    // ── Instructor: CRUD ────────────────────────────────────────────────────

    @Transactional
    public Quiz createQuiz(QuizRequest req) {
        Course course = courseRepository.findById(req.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found"));

        Quiz quiz = new Quiz();
        quiz.setCourse(course);
        quiz.setTitle(req.getTitle());
        quiz.setDescription(req.getDescription());
        quiz.setTimeLimitMinutes(req.getTimeLimitMinutes());
        quiz.setPassingScore(req.getPassingScore() != null ? req.getPassingScore() : 70);
        quiz.setAttemptsAllowed(req.getAttemptsAllowed());
        quiz.setShuffleQuestions(req.isShuffleQuestions());

        if (req.getLessonId() != null) {
            lessonRepository.findById(req.getLessonId())
                    .ifPresent(quiz::setLesson);
        }

        if (req.getQuestions() != null) {
            quiz.setQuestions(buildQuestions(req.getQuestions(), quiz));
        }

        return quizRepository.save(quiz);
    }

    @Transactional
    public Quiz updateQuiz(Long quizId, QuizRequest req) {
        Quiz quiz = getQuizOrThrow(quizId);
        if (req.getTitle() != null) quiz.setTitle(req.getTitle());
        if (req.getDescription() != null) quiz.setDescription(req.getDescription());
        if (req.getTimeLimitMinutes() != null) quiz.setTimeLimitMinutes(req.getTimeLimitMinutes());
        if (req.getPassingScore() != null) quiz.setPassingScore(req.getPassingScore());
        if (req.getAttemptsAllowed() != null) quiz.setAttemptsAllowed(req.getAttemptsAllowed());
        quiz.setShuffleQuestions(req.isShuffleQuestions());

        if (req.getQuestions() != null) {
            quiz.getQuestions().clear();
            quiz.getQuestions().addAll(buildQuestions(req.getQuestions(), quiz));
        }

        return quizRepository.save(quiz);
    }

    @Transactional
    public void publishQuiz(Long quizId, boolean published) {
        Quiz quiz = getQuizOrThrow(quizId);
        quiz.setPublished(published);
        quizRepository.save(quiz);
    }

    @Transactional
    public void deleteQuiz(Long quizId) {
        quizRepository.deleteById(quizId);
    }

    @Transactional(readOnly = true)
    public Quiz getQuiz(Long quizId) {
        return getQuizOrThrow(quizId);
    }

    @Transactional(readOnly = true)
    public List<Quiz> getQuizzesForCourse(Long courseId) {
        return quizRepository.findByCourseIdAndPublishedTrue(courseId);
    }

    @Transactional(readOnly = true)
    public Quiz getQuizByLesson(Long lessonId) {
        return quizRepository.findByLessonId(lessonId).stream().findFirst().orElse(null);
    }

    // ── Student: Attempts ───────────────────────────────────────────────────

    /**
     * Creates a new quiz attempt and returns it.
     * Validates that the student hasn't exceeded the allowed number of attempts.
     */
    @Transactional
    public QuizAttempt startAttempt(Long quizId, Long userId) {
        Quiz quiz = getQuizOrThrow(quizId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (quiz.getAttemptsAllowed() != null) {
            int attemptCount = quizAttemptRepository.countByUserIdAndQuizId(userId, quizId);
            if (attemptCount >= quiz.getAttemptsAllowed()) {
                throw new RuntimeException("Maximum attempts (" + quiz.getAttemptsAllowed() + ") reached");
            }
        }

        QuizAttempt attempt = new QuizAttempt();
        attempt.setUser(user);
        attempt.setQuiz(quiz);
        attempt.setStatus(QuizAttempt.AttemptStatus.IN_PROGRESS);
        return quizAttemptRepository.save(attempt);
    }

    /**
     * Submit answers, auto-grade objective questions, persist result.
     */
    @Transactional
    public QuizAttempt submitAttempt(QuizSubmitRequest req) {
        QuizAttempt attempt = quizAttemptRepository.findById(req.getAttemptId())
                .orElseThrow(() -> new RuntimeException("Attempt not found"));

        if (attempt.getStatus() != QuizAttempt.AttemptStatus.IN_PROGRESS) {
            throw new RuntimeException("Attempt already submitted");
        }

        Quiz quiz = attempt.getQuiz();
        List<Question> questions = quiz.getQuestions();

        int totalPoints = 0;
        int earnedPoints = 0;
        List<QuizAnswer> answers = new ArrayList<>();

        for (QuizSubmitRequest.AnswerRequest ar : req.getAnswers()) {
            Question question = questions.stream()
                    .filter(q -> q.getId().equals(ar.getQuestionId()))
                    .findFirst()
                    .orElse(null);
            if (question == null) continue;

            totalPoints += question.getPoints();
            QuizAnswer answer = new QuizAnswer();
            answer.setAttempt(attempt);
            answer.setQuestion(question);

            boolean isCorrect = false;

            switch (question.getType()) {
                case MCQ, TRUE_FALSE -> {
                    if (ar.getSelectedOptionId() != null) {
                        QuestionOption opt = question.getOptions().stream()
                                .filter(o -> o.getId().equals(ar.getSelectedOptionId()))
                                .findFirst().orElse(null);
                        answer.setSelectedOption(opt);
                        isCorrect = opt != null && opt.isCorrect();
                    }
                }
                case MSQ -> {
                    if (ar.getSelectedOptionIds() != null) {
                        List<QuestionOption> selected = question.getOptions().stream()
                                .filter(o -> ar.getSelectedOptionIds().contains(o.getId()))
                                .toList();
                        answer.setSelectedOptions(selected);
                        List<Long> correctIds = question.getOptions().stream()
                                .filter(QuestionOption::isCorrect).map(QuestionOption::getId).toList();
                        List<Long> selectedIds = selected.stream().map(QuestionOption::getId).toList();
                        isCorrect = correctIds.size() == selectedIds.size()
                                && correctIds.containsAll(selectedIds);
                    }
                }
                case SHORT_ANSWER, FILL_BLANK -> {
                    answer.setTextAnswer(ar.getTextAnswer());
                    // Short answers require manual grading; mark as pending
                    isCorrect = false;
                }
            }

            answer.setCorrect(isCorrect);
            int awarded = isCorrect ? question.getPoints() : 0;
            answer.setPointsAwarded(awarded);
            earnedPoints += awarded;
            answers.add(answer);
        }

        attempt.setAnswers(answers);
        attempt.setPointsEarned(earnedPoints);
        attempt.setPointsPossible(totalPoints);
        double scorePercent = totalPoints > 0 ? (earnedPoints * 100.0 / totalPoints) : 0.0;
        attempt.setScore(Math.round(scorePercent * 10.0) / 10.0);
        attempt.setPassed(scorePercent >= quiz.getPassingScore());
        attempt.setStatus(QuizAttempt.AttemptStatus.GRADED);
        attempt.setSubmittedAt(LocalDateTime.now());

        log.info("Quiz attempt {} submitted: score={}, passed={}", attempt.getId(), attempt.getScore(), attempt.isPassed());
        return quizAttemptRepository.save(attempt);
    }

    @Transactional(readOnly = true)
    public List<QuizAttempt> getUserAttempts(Long quizId, Long userId) {
        return quizAttemptRepository.findByUserIdAndQuizId(userId, quizId);
    }

    @Transactional(readOnly = true)
    public QuizAttempt getBestAttempt(Long quizId, Long userId) {
        return quizAttemptRepository.findTopByUserIdAndQuizIdOrderByScoreDesc(userId, quizId)
                .orElseThrow(() -> new RuntimeException("No attempts found"));
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private Quiz getQuizOrThrow(Long quizId) {
        return quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found: " + quizId));
    }

    private List<Question> buildQuestions(List<QuizRequest.QuestionRequest> questionRequests, Quiz quiz) {
        List<Question> questions = new ArrayList<>();
        for (int i = 0; i < questionRequests.size(); i++) {
            QuizRequest.QuestionRequest qr = questionRequests.get(i);
            Question q = new Question();
            q.setQuiz(quiz);
            q.setType(Question.QuestionType.valueOf(qr.getType().toUpperCase()));
            q.setText(qr.getText());
            q.setExplanation(qr.getExplanation());
            q.setPoints(qr.getPoints() != null ? qr.getPoints() : 1);
            q.setOrderIndex(qr.getOrderIndex() != null ? qr.getOrderIndex() : i);

            if (qr.getOptions() != null) {
                List<QuestionOption> options = new ArrayList<>();
                for (QuizRequest.OptionRequest or : qr.getOptions()) {
                    QuestionOption opt = new QuestionOption();
                    opt.setQuestion(q);
                    opt.setText(or.getText());
                    opt.setCorrect(or.isCorrect());
                    options.add(opt);
                }
                q.setOptions(options);
            }

            questions.add(q);
        }
        return questions;
    }
}
