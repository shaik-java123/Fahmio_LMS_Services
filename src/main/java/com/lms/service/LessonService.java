package com.lms.service;

import com.lms.model.Lesson;
import com.lms.model.Module;
import com.lms.repository.LessonRepository;
import com.lms.repository.ModuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LessonService {

    private final LessonRepository lessonRepository;
    private final ModuleRepository moduleRepository;

    @Transactional
    public Lesson createLesson(Long moduleId, Lesson lesson, Long userId) {
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new RuntimeException("Module not found"));

        if (!module.getCourse().getInstructor().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to add lessons to this module");
        }

        lesson.setModule(module);
        return lessonRepository.save(lesson);
    }

    @Transactional(readOnly = true)
    public List<Lesson> getLessonsByModule(Long moduleId) {
        return lessonRepository.findByModuleIdOrderByOrderIndexAsc(moduleId);
    }

    @Transactional
    public Lesson updateLesson(Long lessonId, Lesson lessonDetails, Long userId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));

        if (!lesson.getModule().getCourse().getInstructor().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to update this lesson");
        }

        lesson.setTitle(lessonDetails.getTitle());
        lesson.setContent(lessonDetails.getContent());
        lesson.setContentType(lessonDetails.getContentType());
        lesson.setVideoUrl(lessonDetails.getVideoUrl());
        lesson.setDocumentUrl(lessonDetails.getDocumentUrl());
        lesson.setExternalUrl(lessonDetails.getExternalUrl());
        lesson.setOrderIndex(lessonDetails.getOrderIndex());
        lesson.setDurationMinutes(lessonDetails.getDurationMinutes());

        return lessonRepository.save(lesson);
    }

    @Transactional
    public void deleteLesson(Long lessonId, Long userId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));

        if (!lesson.getModule().getCourse().getInstructor().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to delete this lesson");
        }

        lessonRepository.deleteById(lessonId);
    }

    @Transactional(readOnly = true)
    public Lesson getLessonById(Long id) {
        return lessonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));
    }
}
