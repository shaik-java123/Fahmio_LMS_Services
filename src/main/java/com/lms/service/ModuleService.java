package com.lms.service;

import com.lms.model.Course;
import com.lms.model.Module;
import com.lms.repository.CourseRepository;
import com.lms.repository.ModuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ModuleService {

    private final ModuleRepository moduleRepository;
    private final CourseRepository courseRepository;

    @Transactional
    public Module createModule(Long courseId, Module module, Long userId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        // Check if user is the instructor of the course
        if (!course.getInstructor().getId().equals(userId) &&
                !course.getInstructor().getRole().name().equals("ADMIN")) {
            throw new RuntimeException("Not authorized to add modules to this course");
        }

        module.setCourse(course);
        return moduleRepository.save(module);
    }

    @Transactional(readOnly = true)
    public List<Module> getModulesByCourse(Long courseId) {
        return moduleRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
    }

    @Transactional
    public Module updateModule(Long moduleId, Module moduleDetails, Long userId) {
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new RuntimeException("Module not found"));

        if (!module.getCourse().getInstructor().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to update this module");
        }

        module.setTitle(moduleDetails.getTitle());
        module.setDescription(moduleDetails.getDescription());
        module.setOrderIndex(moduleDetails.getOrderIndex());

        return moduleRepository.save(module);
    }

    @Transactional
    public void deleteModule(Long moduleId, Long userId) {
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new RuntimeException("Module not found"));

        if (!module.getCourse().getInstructor().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to delete this module");
        }

        moduleRepository.deleteById(moduleId);
    }
}
